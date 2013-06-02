package swat.common.json

import play.api.libs.json._
import scala.reflect.runtime.universe._
import scala.collection.mutable
import scala.reflect.internal.MissingRequirementError
import swat.common.reflect.ReflectionCache
import scala.reflect.ClassTag

/**
 * A custom JSON object serializer and deserializer. The JSON always has the same structure no matter what actually
 * the serialized object is. The root JSON node is an object with fields "$value" which contains the object that is
 * being serialized and "$objects" which contains all instances in the serialization object graph. An instance has
 * it's id (field "$id") and type (field "$type") specified. The rest of the fields are the vals and vars. AnyVals are
 * serialized to JSON primitive types, Arrays to JSON arrays, AnyRefs to instances in the repository and singleton
 * objects to references. An example covering all the concepts follows:
 *
 * {{{
 * {
 *     $value: { $ref: 0 }, // The root object.
 *     $objects: [ // Repository of all AnyRef objects.
 *         {
 *             $id: 0, // Id of the object, which is used while referencing it.
 *             $type: "my.package.X", // Type of the instance.
 *             a: "Foo", // AnyVal.
 *             b: { $ref: 1 }, // Reference to an object.
 *             c: { $ref: "scala.collection.immutable.Nil", // Reference to a singleton object.
 *             d: [ 123, 456, 789 ], // Array of AnyVals.
 *             e: [ { $ref: 1 }, { $ref: 2 } ] // Array of AnyRefs.
 *         },
 *         {
 *             $id: 1,
 *             // ...
 *         },
 *         {
 *            $id: 2,
 *            // ...
 *         }
 *         // ...
 *     ]
 * }
 * }}}
 *
 */
@swat.ignored
class JsonSerializer(val cache: ReflectionCache) {

    val mirror = cache.mirror

    /**
     * Serializes the specified object to a JSON string that conforms to the specification described at
     * [[swat.common.json.JsonSerializer]].
     */
    def serialize(obj: Any): String = mirror.synchronized {
        var objectId = 0
        val objectIds = mutable.HashMap[AnyRef, Int]()
        val serializedObjects = mutable.ListBuffer[JsObject]()

        /** Serializes the specified value to either a primitive value, array or reference. */
        def serializeValue(value: Any): JsValue = value match {
            case null | () => JsNull
            case b: Boolean => JsBoolean(b)
            case b: Byte => JsNumber(b)
            case s: Short => JsNumber(s)
            case i: Int => JsNumber(i)
            case l: Long => JsNumber(l)
            case f: Float => JsNumber(f)
            case d: Double => JsNumber(d)
            case c: Char => JsString(c.toString)
            case s: String => JsString(s)
            case a: Array[_] => JsArray(a.map(serializeValue _))
            case a: AnyRef => serializeAnyRef(a)
            case x => throw new JsonException(s"Can't serialize value '$x'.")
        }

        /**
         * If the object hasn't been serialized yet, serializes it to JSON and adds it to the serializedObjects with
         * a new id. Returns reference to the object. A singleton object is serialized as reference to it's type name.
         */
        def serializeAnyRef(obj: AnyRef): JsObject = {
            val reference = objectIds.get(obj).map(JsNumber(_)).getOrElse {
                // The object hasn't been serialized yet.
                val symbol = cache.getInstanceSymbol(obj)
                if (symbol.isModuleClass) {
                    // Serialize singleton object as a reference to it's type full name.
                    JsString(symbol.fullName + "$")
                } else {
                    // Assign the object a new id and serialize it.
                    val id = objectId
                    objectIds += obj -> id
                    objectId += 1

                    val members = symbol.typeSignature.members
                    val getters = members.collect { case m: MethodSymbol if m.isGetter => m }
                    val serializedFields = getters.map { getter =>
                        val value = mirror.reflect(obj).reflectMethod(getter).apply()
                        getter.name.toString -> serializeValue(value)
                    }
                    val internalFields = Seq("$id" -> JsNumber(id), "$type" -> JsString(symbol.fullName))

                    serializedObjects += JsObject(internalFields ++ serializedFields)
                    JsNumber(id)
                }
            }

            JsObject(Seq("$ref" -> reference))
        }

        // Just start serializing the root object. All referenced objects get discovered during the process.
        Json.prettyPrint(JsObject(Seq(
            "$value" -> serializeValue(obj),
            "$objects" -> JsArray(serializedObjects)
        )))
    }

    /**
     * Deserializes the specified JSON string to an equivalent Scala object. The JSON string has to conform to
     * specification described at [[swat.common.json.JsonSerializer]]. If the second parameter tpe is defined, then
     * the deserialization succeeds only if the deserialized object actually conforms to the specified type. It serves
     * the purpose of a type hint, so for example conversions of numeric types can be performed.
     */
    def deserialize(json: String, tpe: Option[Type] = None): Any = mirror.synchronized {
        val visitedObjectsIds = mutable.HashSet[Int]()
        val deserializedObjects = mutable.HashMap[Int, Any]()
        val referencesToResolve = mutable.ListBuffer[Reference]()

        // Parse the JSON and if it matches the specified structure, extract the root value and all objects from it.
        val (rootValue, serializedObjects) = Json.parse(json) match {
            case JsObject(Seq(("$value", jsValue), ("$objects", jsObjects: JsArray))) => {
                val objects = jsObjects.value.collect {
                    case o@JsObject(Seq(("$id", JsNumber(id)), ("$type", JsString(typeName)), _*)) => {
                        id -> SerializedObject(typeName, o.fields.drop(2))
                    }
                }
                (jsValue, objects.toMap)
            }
            case _ => throw new JsonException(s"Cannot deserialize non-conforming JSON structure '$json'.")
        }

        /**
         * Deserializes the specified value. Primitive types, arrays, references to singleton objects and references
         * to already deserialized objects are immediately returned as the Right value. References to objects that
         * are being deserialized are returned as the Left value.
         */
        def deserializeValue(value: JsValue, tpe: Option[Type]): Either[Int, Any] = value match {
            case JsNull => Right(convert(null, tpe))
            case JsBoolean(b) => Right(convert(b, tpe))
            case JsNumber(n) => Right(convert(n, tpe))
            case JsString(s) => Right(convert(s, tpe))
            case JsArray(items) => Right(deserializeArray(items, tpe))
            case JsObject(Seq(("$ref", reference))) => deserializeRef(reference, tpe)
            case _ => throw new JsonException(s"Cannot deserialize unrecognized value '$value'.")
        }

        /** Deserializes an array. */
        def deserializeArray(items: Seq[JsValue], tpe: Option[Type]): Any = {
            val arrayTpe = tpe.filter(_ <:< typeOf[Array[_]])
            val itemTpe = arrayTpe.map(_.asInstanceOf[TypeRefApi].args.head)

            def createResult[T : ClassTag](): Array[T] = {
                val result = new Array[T](items.length)
                lazy val refResult = result.asInstanceOf[Array[AnyRef]]
                items.map(i => deserializeValue(i, itemTpe)).zipWithIndex.foreach {
                    case (Right(v), index) => result.update(index, v.asInstanceOf[T])
                    case (Left(id), index) => referencesToResolve += ArrayItemReference(refResult, index, id, itemTpe)
                }
                result
            }

            itemTpe match {
                case Some(t) if t =:= typeOf[Boolean] => createResult[Boolean]()
                case Some(t) if t =:= typeOf[Byte] => createResult[Byte]()
                case Some(t) if t =:= typeOf[Short] => createResult[Short]()
                case Some(t) if t =:= typeOf[Int] => createResult[Int]()
                case Some(t) if t =:= typeOf[Long] => createResult[Long]()
                case Some(t) if t =:= typeOf[Float] => createResult[Float]()
                case Some(t) if t =:= typeOf[Double] => createResult[Double]()
                case Some(t) if t =:= typeOf[Char] => createResult[Char]()
                case Some(t) if t =:= typeOf[String] => createResult[String]()
                case _ => createResult[AnyRef]()
            }
        }

        /** Deserializes both references to other objects and references to singleton objects. */
        def deserializeRef(reference: JsValue, tpe: Option[Type]): Either[Int, Any] = reference match {
            case JsNumber(id) => deserializeObjectRef(id.toInt, tpe)
            case JsString(fullName) => Right(cache.getObject(fullName))
            case _ => throw new JsonException(s"Unrecongized reference '$reference'.")
        }

        /** Deserializes references to other objects. */
        def deserializeObjectRef(id: Int, tpe: Option[Type]): Either[Int, Any] = deserializedObjects.get(id) match {
            // The object has already been deserialized.
            case Some(deserializedObject) => Right(convert(deserializedObject, tpe))

            // The object is being deserialized, so it can't be deserialized again in order to avoid cycles.
            case _ if visitedObjectsIds(id) => Left(id)

            // The object hasn't been visited yet, deserialize it.
            case _ => serializedObjects.get(id) match {
                case Some(serializedObject) => {
                    visitedObjectsIds += id
                    val deserializedObject = deserializeObject(serializedObject, tpe)
                    deserializedObjects += id -> deserializedObject
                    Right(deserializedObject)
                }
                case _ => throw new JsonException(s"Cannot resolve reference to object with id '$id'.")
            }
        }

        /** Deserializes the specified object. */
        def deserializeObject(serializedObject: SerializedObject, tpe: Option[Type]): Any = {
            val symbol = cache.getClassSymbol(serializedObject.typeName)

            // Deserialize fields of the object first. When determining the field type, prefer the type hint first,
            // because it contains more information about the type. As a fallback, use the type provided by the object.
            val deserializedFields = serializedObject.fields.map { case (name, value) =>
                val hintFieldTpe = tpe.flatMap(getFieldType(name, _))
                val actualFieldTpe = getFieldType(name, symbol.toType)
                val fieldTpe = hintFieldTpe.orElse(actualFieldTpe)
                DeserializedField(name, deserializeValue(value, fieldTpe), fieldTpe)
            }

            // Create a list of constructor arguments from the deserialized field values.
            val constructor = symbol.toType.declaration(nme.CONSTRUCTOR).asMethod
            val parameterNames = constructor.paramss.flatten.map(_.name.toString)
            val args = parameterNames.map { name =>
                deserializedFields.find(_.name == name) match {
                    case Some(f) => f.value.right.getOrElse(null)
                    case None => throw new JsonException(
                        s"Value of constructor parameter '$name' not found when deserializing '$serializedObject'.")
                }
            }

            // Create the instance using invocation of the default constructor.
            val classMirror = mirror.reflectClass(symbol)
            val constructorMirror = classMirror.reflectConstructor(constructor)
            val instance = constructorMirror(args: _*)

            // Set the rest of the fields and register the references.
            deserializedFields.foreach {
                case DeserializedField(name, Left(id), fieldTpe) => {
                    referencesToResolve += InstanceFieldReference(instance, name, id, fieldTpe)
                }
                case DeserializedField(name, Right(value), _) if !parameterNames.contains(name) => {
                    setFieldValue(instance, name, value)
                }
                case _ => // NOOP, the field was already set via constructor.
            }

            instance
        }

        try {
            // Deserialize the root value, which transitively deserializes all the referenced objects. Resolve the
            // unresolved references after that.
            val result = deserializeValue(rootValue, tpe).right.get
            referencesToResolve.foreach {
                case ArrayItemReference(a, i, id, t) => a(i) = convert(deserializedObjects(id), t).asInstanceOf[AnyRef]
                case InstanceFieldReference(o, n, id, t) => setFieldValue(o, n, convert(deserializedObjects(id), t))
            }
            result
        } catch {
            case e: MissingRequirementError => throw new JsonException(e.getMessage)
        }
    }

    /**
     * Converts the specified value to the target type if the conversion is possible. If the target type isn't defined,
     * returns the input value. Otherwise returns the converted value.
     */
    private def convert(value: Any, targetTpe: Option[Type]): Any = targetTpe match {
        case Some(tpe) => value match {
            case _ if tpe.typeSymbol.isParameter => value
            case null if tpe <:< typeOf[AnyRef] => null
            case null if tpe <:< typeOf[Unit] => ()
            case v if v != null && cache.getInstanceSymbol(v).toType <:< tpe => v
            case n: BigDecimal if tpe weak_<:< typeOf[Double] => tpe match {
                case t if t =:= typeOf[Byte] => n.toByte
                case t if t =:= typeOf[Short] => n.toShort
                case t if t =:= typeOf[Int] => n.toInt
                case t if t =:= typeOf[Long] => n.toLong
                case t if t =:= typeOf[Float] => n.toFloat
                case t if t =:= typeOf[Double] => n.toDouble
            }
            case b: Boolean if tpe =:= typeOf[Boolean] => b
            case s: String if tpe =:= typeOf[Char] && s.length == 1 => s.head
            case v if tpe =:= typeOf[String] => v.toString
            case _ => throw new JsonException(s"Cannot convert '$value' to type '$tpe'.")
        }
        case None => value
    }

    /** Returns type of the specified field. */
    private def getFieldType(name: String, tpe: Type): Option[Type] = tpe.member(newTermName(name)) match {
        case m: MethodSymbol if m.isGetter => Some(m.typeSignatureIn(tpe).asInstanceOf[NullaryMethodType].resultType)
        case _ => None
    }

    /** Sets value of the specified field. */
    private def setFieldValue(target: Any, name: String, value: Any) {
        // Using Java reflection to access directly the field that is lying under the getter and setter, so even vals
        // can be set using this method. The field has to become accessible (in case it isnt) in order to be settable.
        val field = target.getClass.getDeclaredField(name)
        val isAccessible = field.isAccessible
        def setAccessibleIfNeeded(accessible: Boolean) {
            if (!isAccessible) {
                field.setAccessible(accessible)
            }
        }

        setAccessibleIfNeeded(true)
        field.set(target, value)
        setAccessibleIfNeeded(false)
    }

    private case class SerializedObject(typeName: String, fields: Seq[(String, JsValue)])

    private sealed abstract class Reference
    private case class InstanceFieldReference(target: Any, fieldName: String, id: Int, tpe: Option[Type])
        extends Reference
    private case class ArrayItemReference(target: Array[AnyRef], index: Int, id: Int, tpe: Option[Type])
        extends Reference

    private case class DeserializedField(name: String, value: Either[Int, Any], tpe: Option[Type])
}

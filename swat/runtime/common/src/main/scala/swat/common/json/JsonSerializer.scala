package swat.common.json

import play.api.libs.json._
import scala.reflect.runtime.universe._
import scala.collection.mutable
import scala.reflect.internal.MissingRequirementError

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
class JsonSerializer(val mirror: Mirror = runtimeMirror(getClass.getClassLoader)) {

    val valueField = "$value"
    val objectsField = "$objects"
    val idField = "$id"
    val typeField = "$type"
    val refField = "$ref"

    /**
     * Serializes the specified object to a JSON string that conforms to the specification described at
     * [[swat.common.json.JsonSerializer]].
     */
    def serialize(obj: Any): String = {
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
                val symbol = classToSymbol(obj.getClass)
                if (symbol.isModuleClass) {
                    // Serialize singleton object as a reference to it's type full name.
                    JsString(symbol.fullName)
                } else {
                    // Assign the object a new id and serialize it.
                    val id = objectId
                    objectIds += obj -> id
                    objectId += 1

                    val members = symbol.typeSignature.members
                    val getters = members.collect { case m: MethodSymbol if m.isGetter => m }
                    val serializedFields = getters.map { getter =>
                        val objMirror = mirror.reflect(obj)
                        val getterMirror = objMirror.reflectMethod(getter)
                        val value = getterMirror.apply()
                        getter.name.toString -> serializeValue(value)
                    }
                    val internalFields = Seq(idField -> JsNumber(id), typeField -> JsString(symbol.fullName))

                    serializedObjects += JsObject(internalFields ++ serializedFields)
                    JsNumber(id)
                }
            }

            JsObject(Seq(refField -> reference))
        }

        // Just start serializing the root object. All referenced objects get discovered during the process.
        Json.prettyPrint(JsObject(Seq(
            valueField -> serializeValue(obj),
            objectsField -> JsArray(serializedObjects)
        )))
    }

    /**
     * Deserializes the specified JSON string to an equivalent Scala object. The JSON string has to conform to
     * specification described at [[swat.common.json.JsonSerializer]]. If the second parameter tpe is defined, then
     * the deserialization succeeds only if the deserialized object actually conforms to the specified type. It serves
     * the purpose of a type hint, so for example conversions of numeric types can be performed.
     */
    def deserialize(json: String, tpe: Option[Type] = None): Any = {
        val deserializedObjects = mutable.HashMap[Int, Any]()
        val referencesToResolve = mutable.ListBuffer[Reference]()

        /**
         * Deserializes the specified value. Primitive types, arrays, references to singleton objects and references
         * to already deserialized objects are immediately returned.
         */
        def deserializeValue(value: JsValue, tpe: Option[Type]): Either[Int, Any] = value match {
            case JsNull => Right(convert(null, tpe))
            case JsBoolean(b) => Right(convert(b, tpe))
            case JsNumber(n) => Right(convert(n, tpe))
            case JsString(s) => Right(convert(s, tpe))
            case JsArray(items) => Right(deserializeArray(items, tpe))
            case JsObject(Seq((r, reference))) if r == refField => deserializeReference(reference, tpe)
            case _ => throw new JsonException(s"Cannot deserialize unrecognized value '$value'.")
        }

        /** Deserializes an array. */
        def deserializeArray(items: Seq[JsValue], tpe: Option[Type]): Array[Any] = {
            val result = new Array[Any](items.length)
            val itemTpe = tpe.filter(_ <:< typeOf[Array[_]]).map(t => t.asInstanceOf[TypeRefApi].args.head)
            items.map(i => deserializeValue(i, itemTpe)).zipWithIndex.foreach {
                case (Right(v), index) => result.update(index, v)
                case (Left(id), index) => referencesToResolve += ArrayItemReference(result, index, id)
            }
            result
        }

        /** Deserializes both references to other objects and references to singleton objects. */
        def deserializeReference(reference: JsValue, tpe: Option[Type]): Either[Int, Any] = reference match {
            case JsNumber(id) => deserializedObjects.get(id.toInt) match {
                case Some(deserializedObject) => Right(convert(deserializedObject, tpe))
                case None => Left(id.toInt)
            }
            case JsString(typeFullName) => Right(mirror.reflectModule(mirror.staticModule(typeFullName)).instance)
            case _ => throw new JsonException(s"Unrecongized reference '$reference'.")
        }

        /** Deserializes the specified object and registers it to the deserializedObjects under its id. */
        def deserializeObject(value: JsValue): Any = value match {
            case JsObject(fields) => {
                // TODO
            }
            case _ => throw new JsonException(s"Cannot deserialize object '$value'.")
        }

        /** Resolves all unresolved references. */
        def resolveReferences() {
            // TODO
        }

        /** Returns a deserialized object with the specified id. */
        def dereference(id: Int): Any = deserializedObjects.get(id) match {
            case Some(obj) => obj
            case None => throw new JsonException(s"Cannot resolve reference to object with id '$id'.")
        }

        try {
            Json.parse(json) match {
                case JsObject(Seq((v, value), (o, objects: JsArray))) if v == valueField && o == objectsField => {
                    // Deserialize all objects and resolve the unresolved references.
                    objects.value.foreach(deserializeObject _)
                    resolveReferences()

                    // Return the root value.
                    deserializeValue(value, tpe) match {
                        case Right(x) => x
                        case Left(id) => convert(dereference(id), tpe)
                    }
                }
                case _ => throw new JsonException(s"Cannot deserialize non-conforming JSON structure '$json'.")
            }
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
            case null if tpe <:< typeOf[AnyRef] => null
            case null if tpe <:< typeOf[Unit] => ()
            case v if v != null && classToSymbol(v.getClass).typeSignature <:< tpe => v
            case n: BigDecimal if tpe weak_<:< typeOf[Double] => tpe match {
                case t if t =:= typeOf[Byte] => n.toByte
                case t if t =:= typeOf[Short] => n.toShort
                case t if t =:= typeOf[Int] => n.toInt
                case t if t =:= typeOf[Long] => n.toLong
                case t if t =:= typeOf[Float] => n.toFloat
                case t if t =:= typeOf[Double] => n.toDouble
            }
            case s: String if tpe =:= typeOf[Char] && s.length == 1 => s.head
            case v if tpe =:= typeOf[String] => v.toString
            case _ => throw new JsonException(s"Cannot convert '$value' to type '$tpe'.")
        }
        case None => value
    }

    /** Cache of class symbols. */
    private val classSymbols = mutable.HashMap[Class[_], Symbol]()

    /** Returns symbol corresponding to the specified class utilizing the class symbol cache. */
    private def classToSymbol(c: Class[_]): Symbol = {
        classSymbols.getOrElseUpdate(c, mirror.classSymbol(c))
    }

    private sealed abstract class Reference
    private case class ObjectFieldReference(target: Any, field: TermSymbol, id: Int) extends Reference
    private case class ArrayItemReference(target: Array[Any], index: Int, id: Int) extends Reference
}

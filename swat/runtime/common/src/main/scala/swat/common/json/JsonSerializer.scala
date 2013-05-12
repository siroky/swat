package swat.common.json

import play.api.libs.json._
import scala.reflect.runtime.universe._
import scala.collection.mutable
import scala.runtime.BoxedUnit

/**
 * A custom JSON object serializer and deserializer. The JSON always has the same structure no matter what actually
 * the serialized object is. The root JSON node is an object with fields "value" which contains the object that is
 * being serialized and "objects" which contains all instances in the serialization object graph. An instance has
 * it's id (field "$id") and type (field "$type") specified. The rest of the fields are the vals and vars. AnyVals are
 * serialized to JSON primitive types, Arrays to JSON arrays, AnyRefs to instances in the repository and singleton
 * objects to references. An example covering all the concepts follows:
 *
 * {{{
 * {
 *     value: { ref: 0 }, // The root object.
 *     objects: [ // Repository of all AnyRef objects.
 *         {
 *             $id: 0, // Id of the object, which is used while referencing it.
 *             $type: "my.package.X", // Type of the instance.
 *             a: "Foo", // AnyVal.
 *             b: { ref: 1 }, // Reference to an object.
 *             c: { ref: "scala.collection.immutable.Nil", // Reference to a singleton object.
 *             d: [ 123, 456, 789 ], // Array of AnyVals.
 *             e: [ { ref: 1 }, { ref: 2 } ] // Array of AnyRefs.
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

    /**
     * Serializes the specified object to a JSON string that conforms to the specification described at
     * [[swat.common.json.JsonSerializer]].
     */
    def serialize(obj: Any): String = {
        var objectId = 0
        val objectIds = mutable.HashMap[AnyRef, Int]()
        val serializedObjects = mutable.ListBuffer[JsObject]()

        // Internal serialization function.
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

        // Internal serialization of AnyRefs.
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
                    serializedObjects += serializeObject(obj, id, symbol)
                    JsNumber(id)
                }
            }

            JsObject(Seq("ref" -> reference))
        }

        // Internal serialization of objects using reflection.
        def serializeObject(obj: AnyRef, id: Int, symbol: Symbol): JsObject = {
            // Serialize all fields with getters.
            val members = symbol.typeSignature.members
            val getters = members.collect { case m: MethodSymbol if m.isGetter => m }
            val serializedFields = getters.map { getter =>
                val objMirror = mirror.reflect(obj)
                val getterMirror = objMirror.reflectMethod(getter)
                val value = getterMirror.apply()
                getter.name.toString -> serializeValue(value)
            }

            JsObject(Seq(
                "$id" -> JsNumber(id),
                "$type" -> JsString(symbol.fullName)
            ) ++ serializedFields)
        }

        Json.prettyPrint(JsObject(Seq(
            "value" -> serializeValue(obj),
            "objects" -> JsArray(serializedObjects)
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
        val referenceResolvers = mutable.ListBuffer[() => Unit]()

        def deserializeValue(value: JsValue, tpe: Option[Type]): Any = value match {
            case JsNull => convertValue(null, tpe)
            case JsBoolean(b) => convertValue(b, tpe)
            case JsNumber(n) => convertValue(n, tpe)
            case JsString(s) => convertValue(s, tpe)
            case JsArray(items) => deserializeArray(items, tpe)
            case JsObject(Seq(("ref", JsNumber(id)))) => deserializedObjects.get(id.toInt) match {
                case Some(deserializedObject) => convert(deserializedObject, tpe)
                case None => Reference(id.toInt)
            }
            case _ => throw new JsonException(s"Cannot deserialize unrecognized value '$value'.")
        }

        def deserializeArray(items: Seq[JsValue], tpe: Option[Type]): Any = {
            val itemTpe = tpe.map(t => t.t)
            val (array, itemTpe) = tpe match {
                case Some(arrayType) => {

                }
                case None => (new Array[Any](items.length), None)
            }

            tpe match {
                case Some(tpe) =>

                case None => {
                    val result = new Array[Any](items.length)
                    items.zipWithIndex
                }
            }
        }

        def processValue(value: Any, tpe: Option[Type], valueSetter: Any => Unit) {
            value match {
                case Reference(id) => referenceResolvers += {
                    deserializedObjects.get(id) match {
                        case Some(obj) =>
                        case None => throw new JsonException(s"Cannot resolver reference to object with id '$id'.")
                    }
                }
                case v => valueSetter(v)
            }
        }

        Json.parse(json) match {
            case JsObject(Seq(("value", value), ("objects", objects: JsArray))) => {

            }
            case _ => throw new JsonException(s"Cannot deserialize non-conforming structure '$json'.")
        }
    }

    private def convert(value: Any, targetTpe: Option[Type]): Any = targetTpe match {
        // The target type is specified so the value has to be converted if necessary.
        case Some(tpe) => value match {
            case null if tpe <:< typeOf[AnyRef] => null
            case null if tpe <:< typeOf[Unit] => ()
            case v if v != null && classToSymbol(v.getClass).typeSignature <:< tpe => v
            case n: BigDecimal if tpe weak_<:< typeOf[Double] => numericConvertors(tpe)(n)
            case s: String if tpe =:= typeOf[Char] && s.length == 1 => s.head
            case _ => throw new JsonException(s"Cannot convert '$value' to type '$tpe'.")
        }

        // Target type isn't specified so the value may be anything.
        case None => value
    }

    /** Convertors of BigDecimal to other numeric types indexed by the types. */
    private val numericConvertors: Map[Type, BigDecimal => AnyVal](
        typeOf[Byte] -> toByte,
        typeOf[Short] -> toShort,
        typeOf[Int] -> toInt,
        typeOf[Long] -> toLong,
        typeOf[Float] -> toFloat,
        typeOf[Double] -> toDouble
    )

    /** Reference to an object with the specified id. */
    private case class Reference(id: Int)

    /** Cache of class symbols. */
    private val classSymbols = mutable.HashMap[Class[_], Symbol]()

    /**
     * Returns symbol corresponding to the specified class utilizing the class symbol cache.
     */
    private def classToSymbol(c: Class[_]): Symbol = {
        classSymbols.getOrElseUpdate(c, mirror.classSymbol(c.getClass))
    }
}

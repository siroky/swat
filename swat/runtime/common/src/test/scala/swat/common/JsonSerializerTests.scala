package swat.common

import org.scalatest.FunSuite
import swat.common.json.JsonSerializer
import scala.reflect.runtime.universe._

class A(var a: A)

class JsonSerializerTests extends FunSuite {
    val mirror: Mirror = runtimeMirror(getClass.getClassLoader)

    test("Primitive values are serialized to JSON primitive values.") {
        () shouldSerializeTo wrapValue("null")
        (null: Any) shouldSerializeTo wrapValue("null")
        true shouldSerializeTo wrapValue("true")
        12.toByte shouldSerializeTo wrapValue("12")
        123.toShort shouldSerializeTo wrapValue("123")
        1234 shouldSerializeTo wrapValue("1234")
        12345.toLong shouldSerializeTo wrapValue("12345")
        1234.5.toFloat shouldSerializeTo wrapValue("1234.5")
        12345.6 shouldSerializeTo wrapValue("12345.6")
        'c' shouldSerializeTo wrapValue(""""c"""")
        "foo" shouldSerializeTo wrapValue(""""foo"""")
    }

    test("Arrays are serialized to JSON arrays.") {
        Array[Any]() shouldSerializeTo wrapValue("[ ]")
        Array(123, 456, 789) shouldSerializeTo wrapValue("[ 123, 456, 789 ]")
        Array("foo", "bar", "baz") shouldSerializeTo wrapValue("""[ "foo", "bar", "baz" ]""")
    }

    test("Objects and singleton objects are serialized to references.") {
        List(Some(Some(true)), Right("foo"), 123, 456) shouldSerializeTo """
            {
                "$value":{"$ref":0},
                "$objects": [
                    {
                        "$id":3,
                        "$type":"scala.collection.immutable.$colon$colon",
                        "tl":{"$ref":"scala.collection.immutable.Nil"},
                        "hd":456
                    },
                    {
                        "$id":2,
                        "$type":"scala.collection.immutable.$colon$colon",
                        "tl":{"$ref":3},
                        "hd":123
                    },
                    {
                        "$id":4,
                        "$type":"scala.util.Right",
                        "b":"foo"
                    },
                    {
                        "$id":1,
                        "$type":"scala.collection.immutable.$colon$colon",
                        "tl":{"$ref":2},
                        "hd":{"$ref":4}
                    },
                    {
                        "$id":6,
                        "$type":"scala.Some",
                        "x":true
                    },
                    {
                        "$id":5,
                        "$type":"scala.Some",
                        "x":{"$ref":6}
                    },
                    {
                        "$id":0,
                        "$type":"scala.collection.immutable.$colon$colon",
                        "tl":{"$ref":1},
                        "hd":{"$ref":5}
                    }
                ]
            }
                                                                         """
    }

    test("Object graph with cycles is serialized.") {
        val a1 = new A(null)
        val a2 = new A(a1)
        val a3 = new A(a2)
        a1.a = a3

        a1 shouldSerializeTo """
            {
                "$value":{"$ref":0},
                "$objects": [
                    {
                        "$id":2,
                        "$type":"swat.common.A",
                        "a":{"$ref":0}
                    },
                    {
                        "$id":1,
                        "$type":"swat.common.A",
                        "a":{"$ref":2}
                    },
                    {
                        "$id":0,
                        "$type":"swat.common.A",
                        "a":{"$ref":1}
                    }
                ]
            }
                             """
    }

    test("Primitive values are deserialized from JSON primitive values.") {
        wrapValue("null") shouldDeserializeTo(null)
        wrapValue("true") shouldDeserializeTo(true)
        wrapValue("12") shouldDeserializeTo(12.toByte, Some(typeOf[Byte]))
        wrapValue("123") shouldDeserializeTo(123.toShort, Some(typeOf[Short]))
        wrapValue("1234") shouldDeserializeTo(1234, Some(typeOf[Int]))
        wrapValue("12345") shouldDeserializeTo(12345.toLong, Some(typeOf[Long]))
        wrapValue("1234.5") shouldDeserializeTo(1234.5.toFloat, Some(typeOf[Float]))
        wrapValue("12345.6") shouldDeserializeTo(12345.6, Some(typeOf[Double]))
        wrapValue(""""c"""") shouldDeserializeTo('c', Some(typeOf[Char]))
        wrapValue(""""foo"""") shouldDeserializeTo("foo", Some(typeOf[String]))
    }

    test("Arrays are deserialized from JSON primitive arrays.") {
        wrapValue("[ ]") shouldDeserializeTo(Array[Any]())
        wrapValue("[ 1, 2, 3 ]") shouldDeserializeTo(Array[Any](1, 2, 3))
        wrapValue("[ 1, 2, 3 ]") shouldDeserializeTo(Array[Int](1, 2, 3), Some(typeOf[Array[Int]]))
    }

    test("Singleton objects are deserialized from references.") {
        wrapValue("""{"$ref":"scala.collection.immutable.Nil"}""") shouldDeserializeTo(Nil)
    }

    implicit class SerializableObject(obj: Any) {
        def shouldSerializeTo(json: String) {
            val expected = normalize(json)
            val actual = normalize(new JsonSerializer(mirror).serialize(obj))
            if (expected != actual) {
                fail(
                    s"""|The serialized value isn't equal to the expected.
                        |EXPECTED: '$expected'
                        |ACTUAL:   '$actual'
                    """.stripMargin
                )
            }
        }

        def normalize(json: String): String = {
            json.lines.map(_.replace(" ", "")).filter(!_.isEmpty).mkString
        }
    }

    implicit class DeserializableString(json: String) {
        def shouldDeserializeTo(expected: Any, tpe: Option[Type] = None) {
            val actual = new JsonSerializer(mirror).deserialize(json, tpe)
            val succeeded = (expected, actual) match {
                case (a: Array[_], b: Array[_]) if a.length == b.length => (0 until a.length).forall(i => a(i) == b(i))
                case (a, b) => a == b
            }
            if (!succeeded) {
                fail(
                    s"""|The deserialized object isn't equal to the expected.
                        |EXPECTED: '$expected'
                        |ACTUAL:   '$actual'
                    """.stripMargin
                )
            }
        }
    }

    def wrapValue(value: String): String = s"""{ "$$value": $value, "$$objects": [ ] }"""
}



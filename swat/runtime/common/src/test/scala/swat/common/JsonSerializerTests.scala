package swat.common

import org.scalatest.FunSuite
import swat.common.json.JsonSerializer

class A(var a: A)

class JsonSerializerTests extends FunSuite {

    test("Primitive types are serialized to JSON primitive types.") {
        () shouldSerializeToValue """null"""
        (null: Any) shouldSerializeToValue """null"""
        true shouldSerializeToValue """true"""
        12.toByte shouldSerializeToValue """12"""
        123.toShort shouldSerializeToValue """123"""
        1234 shouldSerializeToValue """1234"""
        12345.toLong shouldSerializeToValue """12345"""
        1234.5.toFloat shouldSerializeToValue """1234.5"""
        12345.6 shouldSerializeToValue """12345.6"""
        'c' shouldSerializeToValue """"c""""
        "foo" shouldSerializeToValue """"foo""""
    }

    test("Arrays are serialized to JSON arrays.") {
        Array() shouldSerializeToValue """[ ]"""
        Array(123, 456, 789) shouldSerializeToValue """[ 123, 456, 789 ]"""
        Array("foo", "bar", "baz") shouldSerializeToValue """[ "foo", "bar", "baz" ]"""
    }

    test("Objects and singleton objects are serialized to references.") {
        List(Some(Some(true)), Right("foo"), 123, 456) shouldSerializeTo """
            {
                "value":{"ref":0},
                "objects": [
                    {
                        "$id":3,
                        "$type":"scala.collection.immutable.$colon$colon",
                        "tl":{"ref":"scala.collection.immutable.Nil"},
                        "hd":456
                    },
                    {
                        "$id":2,
                        "$type":"scala.collection.immutable.$colon$colon",
                        "tl":{"ref":3},
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
                        "tl":{"ref":2},
                        "hd":{"ref":4}
                    },
                    {
                        "$id":6,
                        "$type":"scala.Some",
                        "x":true
                    },
                    {
                        "$id":5,
                        "$type":"scala.Some",
                        "x":{"ref":6}
                    },
                    {
                        "$id":0,
                        "$type":"scala.collection.immutable.$colon$colon",
                        "tl":{"ref":1},
                        "hd":{"ref":5}
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
                "value":{"ref":0},
                "objects": [
                    {
                        "$id":2,
                        "$type":"swat.common.A",
                        "a":{"ref":0}
                    },
                    {
                        "$id":1,
                        "$type":"swat.common.A",
                        "a":{"ref":2}
                    },
                    {
                        "$id":0,
                        "$type":"swat.common.A",
                        "a":{"ref":1}
                    }
                ]
            }
                             """
    }

    implicit class SerializableObject(obj: Any) {
        def shouldSerializeTo(json: String) {
            val expected = normalize(json)
            val actual = normalize(new JsonSerializer().serialize(obj))
            if (expected != actual) {
                fail(
                    s"""|The serialized value isn't equal to the expected.
                        |EXPECTED: '$expected'
                        |ACTUAL:   '$actual'
                    """.stripMargin
                )
            }
        }

        def shouldSerializeToValue(value: String) {
            shouldSerializeTo(s"""{ "value": $value, "objects": [ ] }""")
        }

        def normalize(json: String): String = {
            json.lines.map(_.replace(" ", "")).filter(!_.isEmpty).mkString
        }
    }

    implicit class DeserializableString(json: String) {
        def shouldDeserializeTo(expected: Any) {
            val actual = new JsonSerializer().deserialize(json)
            if (expected != actual) {
                fail(
                    s"""|The deserialized object isn't equal to the expected.
                        |EXPECTED: '$expected'
                        |ACTUAL:   '$actual'
                    """.stripMargin
                )
            }
        }
    }
}



package swat.client.json

import swat.js.CommonScope._

object JsonSerializer {

    def serialize(obj: Any): String = obj match {
        case a: Array[_] => JSON.stringify(a(0))
        case _ => JSON.stringify(obj)
        // TODO
    }

    def deserialize(json: String): Any = {
        // TODO
        JSON.parse(json)
    }

    def escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\'")

    def escapeAndQuote(value: String): String = "'" + escape(value) + "'"
}

package swat.client.json

import swat.js.CommonScope._

object Serializer {

    def serialize(obj: Any): String = {
        // TODO
        JSON.stringify(obj)
    }

    def deserialize(json: String): Any = {
        // TODO
        JSON.parse(json)
    }

    def escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\'")

    def escapeAndQuote(value: String): String = "'" + escape(value) + "'"
}

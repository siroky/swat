package swat.client.json

import swat.js.CommonScope._

object JsonSerializer {

    def serialize(value: Any): String = swat.client.swat.serialize(value)

    def deserialize(json: String): Any = {
        // TODO
        JSON.parse(json)
    }
}

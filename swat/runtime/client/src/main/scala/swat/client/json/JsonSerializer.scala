package swat.client.json

import scala.concurrent._
import ExecutionContext.Implicits.global
import swat.client.swat
import _root_.swat.js.CommonScope._
import _root_.swat.common.TypeLoader

object JsonSerializer {

    def serialize(value: Any): String = {
        swat.serialize(value)
    }

    def deserialize(json: String): Future[Any] = {
        val obj = JSON.parse(json)
        val loadedTypes = swat.jsArrayToScalaArray(swat.loadedTypes)
        val missingTypes = swat.jsArrayToScalaArray(swat.findMissingTypes(obj))

        if (missingTypes.length > 0) {
            // Not all the types present in the result are loaded. Therefore they have to be loaded, the code with the
            // types executed and the deserialization performed after that.
            TypeLoader.get(missingTypes, loadedTypes).map { code =>
                eval(code)
                swat.deserialize(obj)
            }
        } else {
            // All the necessary types are loaded so deserialization can be done immediately.
            Future(swat.deserialize(obj))
        }
    }
}

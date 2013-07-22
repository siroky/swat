package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc._
import swat.common._
import swat.common.rpc._

object Swat extends Controller {
    def tpe(typeIdentifier: String) = AsyncAction { r =>
        TypeLoader.getOrAlert(Array(typeIdentifier), Array.empty)
    }

    def app(typeIdentifier: String, args: String) = AsyncAction { r =>
        TypeLoader.getAppOrAlert(typeIdentifier, args.split(","))
    }

    def rpc(methodIdentifier: String) = AsyncAction { r =>
        val arguments = r.body.asJson.map(_.toString()).getOrElse("")
        RpcDispatcher.invoke(methodIdentifier, arguments)
    }

    private def AsyncAction(a: Request[AnyContent] => Future[String]) = Action { r =>
        Async(a(r).map(Ok(_)))
    }
}

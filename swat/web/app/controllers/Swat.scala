package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc._
import swat.common.{TypeLoadingException, TypeLoader}
import swat.common.rpc.RpcDispatcher
import scala.concurrent.duration.Duration

object Swat extends Controller {
  
    def tpe(typeIdentifier: String) = AsyncAction { r =>
        TypeLoader.get(Array(typeIdentifier), Array.empty).recover {
            case e: TypeLoadingException => s"alert('Swat type loading error: ${e.message}');"
        }
    }

    def app(appObjectTypeIdentifier: String, args: String) = AsyncAction { r =>
        TypeLoader.getApp(appObjectTypeIdentifier, args.split(",")).recover {
            case e: TypeLoadingException => s"alert('Swat application loading error: ${e.message}');"
        }
    }

    def rpc(methodIdentifier: String) = AsyncAction { r =>
        val dispatcher = new RpcDispatcher
        val arguments = r.body.asJson.map(_.toString()).getOrElse("")
        dispatcher.invoke(methodIdentifier, arguments)
    }

    private def AsyncAction(a: Request[AnyContent] => Future[String]) = Action { request =>
        Async(a(request).map(Ok(_)))
    }
}

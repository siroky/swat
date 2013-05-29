package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc._
import swat.common.TypeLoader
import swat.common.rpc.RpcDispatcher
import scala.concurrent.duration.Duration

object Swat extends Controller {
  
    def tpe(typeIdentifier: String) = Action {
        val code = TypeLoader.get(Array(typeIdentifier), Array.empty)
        Async(code.map(Ok(_)))
    }

    def app(appObjectTypeIdentifier: String, args: String) = Action {
        val code = TypeLoader.getApp(appObjectTypeIdentifier, args.split(","))
        Async(code.map(Ok(_)))
    }

    def rpc(methodIdentifier: String) = Action { request =>
        val dispatcher = new RpcDispatcher
        val arguments = request.body.asJson.map(_.toString()).getOrElse("")
        val result = dispatcher.invoke(methodIdentifier, arguments)
        Async(result.map(Ok(_)))
    }
}

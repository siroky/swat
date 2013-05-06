package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc._
import swat.common.TypeLoader
import swat.common.rpc.RpcDispatcher

object Swat extends Controller {
  
    def tpe(typeIdentifier: String) = Action {
        val code = TypeLoader.get(List(typeIdentifier))
        Async(code.map(Ok(_)))
    }

    def app(appObjectTypeIdentifier: String, args: String) = Action {
        val code = TypeLoader.getApp(appObjectTypeIdentifier, args.split(",").toList)
        Async(code.map(Ok(_)))
    }

    def rpc(methodIdentifier: String) = Action { request =>
        Ok(RpcDispatcher.invoke(methodIdentifier, request.body.toString))
    }
}

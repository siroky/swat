package controllers

import play.api.mvc._
import swat.common.{TypeLoadingException, TypeLoader}

object Swat extends Controller {
  
    def tpe(typeIdentifier: String) = Action {
        Ok(TypeLoader.get(List(typeIdentifier)))
    }

    def app(appObjectTypeIdentifier: String, args: String) = Action {
        Ok(TypeLoader.getApp(appObjectTypeIdentifier, args.split(",").toList))
    }
}

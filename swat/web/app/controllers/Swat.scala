package controllers

import play.api.mvc._
import swat.common.{TypeLoadingException, TypeLoader}

object Swat extends Controller {
  
    def tpe(typeIdentifier: String) = Action {
        safelyLoaded(TypeLoader.get(List(typeIdentifier)))
    }

    def app(appObjectTypeIdentifier: String, args: String) = Action {
        safelyLoaded(TypeLoader.getApp(appObjectTypeIdentifier, args.split(",").toList))
    }

    private def safelyLoaded(code: => String): Result = {
        try {
            Ok(code)
        } catch {
            case e: TypeLoadingException => Ok(s"alert('Swat type loading error: ${e.message}');")
        }
    }
}

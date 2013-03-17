package controllers

import play.api.mvc._
import swat.runtime.server.{TypeLoader, TypeLoadingException}

object Swat extends Controller {
  
    def index(typeIdentifier: String) = Action {
        try {
            Ok(TypeLoader.get(List(typeIdentifier)))
        } catch {
            case TypeLoadingException(message) => Ok(s"alert('$message');")
        }
    }
}

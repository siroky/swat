package controllers

import play.api.mvc._

object Application extends Controller {
    def index = Action(Ok(views.html.index()))
    def tests = Action(Ok(views.html.tests()))
    def playground = Action(Ok(views.html.playground()))
}

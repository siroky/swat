package swat.api.js.html.elements

import swat.api.js.html._

trait Image extends Element {
    var alt: String
    var src: String
    val complete: Boolean
    var height: Double
    var width: Double
}

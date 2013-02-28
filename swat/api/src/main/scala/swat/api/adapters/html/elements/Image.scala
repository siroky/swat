package swat.api.adapters.html.elements

import swat.api.adapters.html._

trait Image extends Element {
    var alt: String
    var src: String
    val complete: Boolean
    var height: Double
    var width: Double
}

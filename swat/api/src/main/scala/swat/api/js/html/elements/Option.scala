package swat.api.js.html.elements

import swat.api.js.html.Element

trait Option extends Element with InputLike {
    val defaultSelected: Boolean
    var index: Int
    var selected: Boolean
    var text: String
}

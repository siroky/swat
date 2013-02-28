package swat.api.adapters.html.elements

import swat.api.adapters.html.Element

trait Option extends Element with InputLike {
    val defaultSelected: Boolean
    var index: Int
    var selected: Boolean
    var text: String
}

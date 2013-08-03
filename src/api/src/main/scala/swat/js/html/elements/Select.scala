package swat.js.html.elements

import swat.js.html.Element

trait Select extends Element with InputLike {
    val length: Int
    var multiple: Boolean
    var selectedIndex: Int
    var size: Int

    def add(option: Option, before: Option)
    def remove(index: Int)
}

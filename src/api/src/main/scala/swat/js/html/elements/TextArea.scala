package swat.js.html.elements

import swat.js.html.Element

trait TextArea extends Element with TextInputLike {
    var rows: Int
    var cols: Int
    var readOnly: Boolean
}

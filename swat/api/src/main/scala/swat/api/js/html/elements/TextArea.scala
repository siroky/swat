package swat.api.js.html.elements

import swat.api.js.html.Element

trait TextArea extends Element with TextInputLike {
    var rows: Int
    var cols: Int
    var readOnly: Boolean
}

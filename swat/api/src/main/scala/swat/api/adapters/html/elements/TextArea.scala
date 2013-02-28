package swat.api.adapters.html.elements

import swat.api.adapters.html.Element

trait TextArea extends Element with TextInputLike {
    var rows: Int
    var cols: Int
    var readOnly: Boolean
}

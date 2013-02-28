package swat.api.adapters.html.elements

import swat.api.adapters.html.Element

trait Anchor extends Element {
    var charset: String
    var href: String
    var hreflang: String
    var name: String
    var rel: String
    var rev: String
}

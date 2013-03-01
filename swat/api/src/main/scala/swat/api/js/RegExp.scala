package swat.api.js

class RegExp(regexp: String, modifier: String) {
    val global: Boolean = ???
    val lastIndex: Int = ???
    val ignoreCase: Boolean = ???
    val multiline: Boolean = ???
    val source: String = ???

    def compile(regexp: String, modifier: String) {}
    def test(s: String): Boolean = ???
    def exec(s: String): String = ???
}

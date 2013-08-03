package swat.js.url

import swat.js

class URLQuery {
    val size: Long = ???

    def get(name: String): String = ???
    def getAll(name: String): js.Array[String] = ???
    def set(name: String, value: String) {}
    def append(name: String, value: String) {}
    def has(name: String): Boolean = ???
    def delete(name: String) {}
}

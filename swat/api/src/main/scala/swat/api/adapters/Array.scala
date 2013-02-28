package swat.api.adapters

class Array[A <: Any] {
    var length: Int = ???

    def concat(a: Array[A]) {}
    def indexOf(item: A): Int = ???
    def join(separator: String): String = ???
    def lastIndexOf(item: A): Int = ???
    def pop(): A = ???
    def push(item: A): Int = ???
    def reverse() {}
    def shift(): A = ???
    def slice(start: Int, end: Int): Array[A] = ???
    def sort() {}
    def splice(index: Int, howmany: Int): Array[A] = ???
    def unshift(item: A): Int = ???
    def valueOf(): Array[A] = ???
}

package swat.scala.collection.immutable

sealed abstract class List[+A] {
    def foreach(f: A => Unit)
    def ::[B >: A] (x: B): List[B] = new ::(x, this)
}
case class ::[A](private var hd: A, private var tl: List[A]) extends List[A] {
    def head = hd
    def tail = tl
    def foreach(f: A => Unit) {
        f(head)
        tail.foreach(f)
    }
}
case object Nil extends List[Nothing] {
    def foreach(f: Nothing => Unit) {}
}

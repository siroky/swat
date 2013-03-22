package swat.runtime.client.test

import swat.api.js._

trait A {
    def foo(x: String) = "A.foo(String)"
    def foo(x: Int) = "A.foo(Int)"
}

trait B {
    def foo(x: String) = "B.foo(String)"
    def foo(x: Double) = "B.foo(Double)"
}

class X extends A with B {
    override def foo(x: String) = "X.foo(String)"
    def test() {
        window.alert(foo(""))
        window.alert(super.foo(""))
        window.alert(super[A].foo(""))
        window.alert(super[B].foo(""))
        window.alert(foo(123))
        window.alert(foo(123.0))
        window.alert(this.isInstanceOf[A])
        window.alert(this.isInstanceOf[Int])
    }
}

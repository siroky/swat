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

class C extends A {
    override def equals(that: Any) = that != null && that.isInstanceOf[C]
}

class Tests extends A with B {
    override def foo(x: String) = "X.foo(String)"

    def testDispatch() {
        window.alert(foo(""))
        window.alert(super.foo(""))
        window.alert(super[A].foo(""))
        window.alert(super[B].foo(""))
        window.alert(foo(123))
        window.alert(foo(123.0))
    }

    def testCasts() {
        window.alert(this.isInstanceOf[A])
        window.alert(this.isInstanceOf[Int])
        window.alert(this.asInstanceOf[A])
        try {
            window.alert(this.asInstanceOf[Int])
        } catch {
            case t: Throwable => window.alert(t.getMessage)
        }
    }

    def testToString() {
        window.alert(123.toString)
        window.alert(123.45.toString)
        window.alert(true.toString)
        window.alert('x'.toString)
        window.alert("x".toString)
        window.alert((new C).toString)
        window.alert(toString)
    }

    def testEquals() {
        { val a: Any = null; val b: Any = null; window.alert(a == b); }
        { val a: Any = this; val b: Any = null; window.alert(a == b); }
        { val a: Any = null; val b: Any = this; window.alert(a == b); }
        { val a: Any = 123; val b: Any = 123; window.alert(a == b); }
        { val a: Any = 123; val b: Any = 456; window.alert(a == b); }
        { val a: Any = true; val b: Any = true; window.alert(a == b); }
        { val a: Any = true; val b: Any = false; window.alert(a == b); }
        { val a: Any = 'x'; val b: Any = 'x'; window.alert(a == b); }
        { val a: Any = 'x'; val b: Any = 'y'; window.alert(a == b); }
        { val a: Any = "abcdef"; val b: Any = "abcdef"; window.alert(a == b); }
        { val a: Any = "abcdef"; val b: Any = "fdsfsf"; window.alert(a == b); }
        { val a: Any = this; val b: Any = this; window.alert(a == b); }
        { val a: Any = this; val b: Any = (new C); window.alert(a == b); }
        { val a: Any = (new C); val b: Any = (new C); window.alert(a == b); }
    }

    def testHashCode() {
        val n: Any = null
        window.alert(n.hashCode)
        window.alert(123.hashCode)
        window.alert(123.67.hashCode)
        window.alert("abc".hashCode)
        window.alert(hashCode)
        window.alert((new C).hashCode)
    }

    override def toString = "Custom toString"

    override def hashCode = 1234
}

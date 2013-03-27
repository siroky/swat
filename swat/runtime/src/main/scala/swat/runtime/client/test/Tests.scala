package swat.runtime.client.test

class Tests {
    def run() {
        (new MethodDispatchTest).run()
        (new AnyMethodAndOperatorTest).run()
    }
}

trait A {
    def foo(x: String) = "A.foo(String)"
    def foo(x: Int) = "A.foo(Int)"
}

trait B {
    def foo(x: String) = "B.foo(String)"
    def foo(x: Double) = "B.foo(Double)"
}

class MethodDispatchTest extends A with B with Test {
    def test() {
        assert(foo("") == "X.foo(String)", "Dispatch of method of this type.")
        assert(super.foo("") == "B.foo(String)", "Dispatch of method of super type.")
        assert(super[A].foo("") == "A.foo(String)", "Dispatch of method of concrete super type.")
        assert(super[B].foo("") == "B.foo(String)", "Dispatch of method of concrete super type.")
        assert(foo(123) == "A.foo(Int)", "Dispatch of overloaded method.")
        assert(foo(123.0) == "B.foo(Double)", "Dispatch of overloaded method.")
    }

    override def foo(x: String) = "X.foo(String)"
}

class C extends A {
    override def equals(that: Any) = that != null && that.isInstanceOf[A]
}

class AnyMethodAndOperatorTest extends A with Test {
    def test() {
        testEquals()
        testHashCode()
        testToString()
        testTypeChecks()
    }

    def testTypeChecks() {
        assert(this.isInstanceOf[A], "Successful type check.")
        assert(!this.isInstanceOf[Int], "Unsuccessful type check.")
        assert(this.asInstanceOf[A] eq this, "Successful cast.")

        val classCastThrown =
            try {
                this.asInstanceOf[Int]
                false
            } catch {
                case _: ClassCastException => true
                case _: Throwable => false
            }
        assert(classCastThrown, "Unsuccessful cast.")
    }

    override def toString = "42"

    def testToString() {
        assert(123.toString == "123", "Integral type toString.")
        assert(123.45.toString == "123.45", "Floating type toString.")
        assert(true.toString == "true", "Boolean type toString.")
        assert('x'.toString == "x", "Char type toString")
        assert("x".toString == "x", "String type toString")
        assert(toString == "42", "Class type toString.")
    }

    def testEquals() {
        { val a: Any = null; val b: Any = null; assert(a == b, "Null vs null equals."); }
        { val a: Any = this; val b: Any = null; assert(a != b, "Null vs object equals."); }
        { val a: Any = null; val b: Any = this; assert(a != b, "Object vs null equals."); }
        { val a: Any = 123; val b: Any = 123; assert(a == b, "Same integers equals."); }
        { val a: Any = 123; val b: Any = 456; assert(a != b, "Different integers equals."); }
        { val a: Any = true; val b: Any = true; assert(a == b, "Same boolean equals."); }
        { val a: Any = true; val b: Any = false; assert(a != b, "Different boolean equals."); }
        { val a: Any = 'x'; val b: Any = 'x'; assert(a == b, "Same char equals."); }
        { val a: Any = 'x'; val b: Any = 'y'; assert(a != b, "Different char equals."); }
        { val a: Any = "abcdef"; val b: Any = "abcdef"; assert(a == b, "Same string equals."); }
        { val a: Any = "abcdef"; val b: Any = "fdsfsf"; assert(a != b, "Different string equals."); }
        { val a: Any = this; val b: Any = this; assert(a == b, "Same object default equals."); }
        { val a: Any = (new C); val b: Any = (new C); assert(a == b, "Same object custom equals."); }
        { val a: Any = this; val b: Any = (new C); assert(a != b, "Different object default equals."); }
        { val a: Any = (new C); val b: Any = this; assert(a == b, "Different object custom equals."); }
    }

    override def hashCode = 1234

    def testHashCode() {
        val n: Any = null
        assert(n.hashCode == 0, "Null hashCode.")
        assert(123.hashCode == 123, "Integral type hashCode.")
        assert(123.67.hashCode == 124, "Floating type hashCode.")
        assert("abc".hashCode > 0, "String type hashCode.")
        assert((new C).hashCode > 0, "Object default hashCode.")
        assert(hashCode == 1234, "Object custom hashCode.")
    }
}


package swat.runtime.client.test

trait A {
    def foo(x: String) = "A.foo(String)"
    def foo(x: Int) = "A.foo(Int)"
}

trait B {
    def foo(x: String) = "B.foo(String)"
    def foo(x: Double) = "B.foo(Double)"
}

class MethodDispatchTests extends A with B with TestSuite {
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

package swat.runtime.client.test

class ArrayTests extends TestSuite {
    def test() {
        val a = new Array[String](10)
        a(0) = "foo"
        a(1) = "bar"
        assert(a.length == 10, "Array length.")
        assert(a(0) == "foo" && a(1) == "bar", "Array set and get.")
        a(1) = "barbar"
        assert(a(1) == "barbar", "Array update.")
    }
}

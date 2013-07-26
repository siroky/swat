package swat.web.tests

import swat.js.DefaultScope._

trait TestSuite {
    protected def test()

    def run() {
        console.log(getClass.getName)
        test()
    }

    def assert(value: Boolean, description: String = "") {
        console.log("  " + (if (value) "OK" else "ERROR") + " - " + description)
    }

    def success(description: String) {
        assert(true, description)
    }

    def fail(description: String) {
        assert(false, description)
    }
}

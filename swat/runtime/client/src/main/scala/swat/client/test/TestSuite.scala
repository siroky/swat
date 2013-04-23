package swat.client.test

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
}

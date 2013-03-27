package swat.runtime.client.test

import swat.api.js.console

trait Test {
    protected def test()

    def run() {
        console.log(getClass.getName)
        test()
    }

    def assert(value: Boolean, description: String = "") {
        console.log("  " + (if (value) "OK" else "ERROR") + " - " + description)
    }
}

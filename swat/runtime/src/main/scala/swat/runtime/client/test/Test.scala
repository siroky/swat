package swat.runtime.client.test

import swat.api.js._

class A(val x: String)

class B(val y: String, x: String) extends A(x) {
    def foo() {
        val printer = new Printer
        printer.print(y + x)
    }
}

class Printer {
    def print(message: String) {
        console.log(message)
    }
}

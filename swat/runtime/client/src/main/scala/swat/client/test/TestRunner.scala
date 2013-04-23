package swat.client.test

object TestRunner extends App {
    (new MethodDispatchTests).run()
    (new AnyMethodTests).run()
    (new ArrayTests).run()
}

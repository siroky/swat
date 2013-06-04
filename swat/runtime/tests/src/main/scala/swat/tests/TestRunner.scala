package swat.tests

object TestRunner extends App {
    (new MethodDispatchTests).run()
    (new AnyMethodTests).run()
    (new ArrayTests).run()
    (new RpcTests).run()
}

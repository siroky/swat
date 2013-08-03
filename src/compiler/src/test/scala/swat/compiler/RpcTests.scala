package swat.compiler

class RpcTests extends CompilerSuite {

    test("Remote methods invocations are transformed to swat.invokeRemote.") {
        """
            import scala.concurrent.Future
            import scala.concurrent.ExecutionContext.Implicits.global
            import swat.remote

            @remote object Server {
                def foo(s: String): Future[Int] = Future(s.length)
            }

            class A {
                Server.foo("bar")
            }
        """ shouldCompileTo Map(
            "A" ->
                """
                    swat.provide('A');
                    swat.require('java.lang.Object', true);
                    swat.require('rpc.RpcProxy$', false);
                    swat.require('scala.Any', true);
                    swat.require('scala.Int', false);
                    swat.require('scala.Tuple1', false);

                    A.$init$ = (function() {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'A');
                        swat.invokeRemote('Server.foo', new scala.Tuple1('bar'));
                    });
                    A = swat.type('A', [A, java.lang.Object, scala.Any]);
                """
        )
    }
}

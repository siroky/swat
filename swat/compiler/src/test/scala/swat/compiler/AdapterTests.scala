package swat.compiler

class AdapterTests extends CompilerSuite
{
    test("Packages are properly stripped") {
        """
            swat.api.js.GlobalScope.window
            swat.api.js.dom.Node

            new swat.api.js.Array()
            new swat.api.js.applications.XMLHttpRequest()
        """ fragmentShouldCompileTo """
            window;
            Node;

            new Array();
            new XMLHttpRequest();
        """
    }

    test("Accessors are compiled to field access and assignment") {
        """
            val a = new swat.api.js.Array()
            a.length
            a.length = 10

            swat.api.js.GlobalScope.window.status
            swat.api.js.GlobalScope.window.status = "ok"
            swat.api.js.GlobalScope.window.opener.status
            swat.api.js.GlobalScope.window.opener.status = "fail"
        """ fragmentShouldCompileTo """
            var a = new Array();
            a.length;
            a.length = 10;

            window.status;
            window.status = 'ok';
            window.opener.status;
            window.opener.status = 'fail';
        """
    }

    test("Globals are properly qualified") {
        """
            swat.api.js.GlobalScope.NaN
            swat.api.js.GlobalScope.eval("code")
            swat.api.js.GlobalScope.isNaN(123)
        """ fragmentShouldCompileTo """
            NaN;
            eval('code');
            isNaN(123);
        """
    }

    test("Overloaded methods aren't provided the type hint") {
        """
            val r = new swat.api.js.applications.XMLHttpRequest()
            r.open("GET", "http://api.com")
            r.open("GET", "http://api.com", true)
            r.open("GET", "http://api.com", true, "username", "password")
        """ fragmentShouldCompileTo """
            var r = new XMLHttpRequest();
            r.open('GET', 'http://api.com');
            r.open('GET', 'http://api.com', true);
            r.open('GET', 'http://api.com', true, 'username', 'password');
        """
    }
}

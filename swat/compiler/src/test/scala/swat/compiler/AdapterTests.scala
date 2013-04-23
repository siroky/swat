package swat.compiler

class AdapterTests extends CompilerSuite {

    test("Packages are properly stripped") {
        """
            swat.js.DefaultScope.window
            swat.js.dom.Node

            new swat.js.Array()
            new swat.js.applications.XMLHttpRequest()
        """ fragmentShouldCompileTo """
            window;
            Node;

            new Array();
            new XMLHttpRequest();
        """
    }

    test("Accessors are compiled to field access and assignment") {
        """
            val a = new swat.js.Array()
            a.length
            a.length = 10

            swat.js.DefaultScope.window.status
            swat.js.DefaultScope.window.status = "ok"
            swat.js.DefaultScope.window.opener.status
            swat.js.DefaultScope.window.opener.status = "fail"
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
            swat.js.DefaultScope.NaN
            swat.js.DefaultScope.eval("code")
            swat.js.DefaultScope.isNaN(123)
        """ fragmentShouldCompileTo """
            NaN;
            eval('code');
            isNaN(123);
        """
    }

    test("Overloaded methods aren't provided the type hint") {
        """
            val r = new swat.js.applications.XMLHttpRequest()
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

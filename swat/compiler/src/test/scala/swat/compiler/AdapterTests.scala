package swat.compiler

class AdapterTests extends CompilerSuite
{
    test("Packages are properly stripped") {
        """
            swat.api.js.window
            swat.api.js.dom.Node

            new swat.api.js.Array()
            new swat.api.js.browser.XMLHttpRequest()
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

            swat.api.js.window.status
            swat.api.js.window.status = "ok"
            swat.api.js.window.opener.status
            swat.api.js.window.opener.status = "fail"
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
            swat.api.js.NaN
            swat.api.js.eval("code")
            swat.api.js.isNaN(123)
        """ fragmentShouldCompileTo """

                                    """
    }
}

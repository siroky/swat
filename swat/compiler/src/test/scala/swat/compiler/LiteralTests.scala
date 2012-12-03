package swat.compiler

class LiteralTests extends CompilerSuite
{
    test("Unit and null") {
        """
            val x = ()
            val y = null
        """ fragmentShouldCompileTo """
            var x = undefined;
            var y = null;
        """
    }

    test("Boolean literals") {
        """
            true
            false
        """ fragmentShouldCompileTo """
            true;
            false;
        """
    }

    test("String and char literals") {
        """
            'x'
            '\b'
            '\f'
            '\n'
            '\r'
            '\t'
            '\''
            '\"'
            '\\'
            ""
            "xyz"
            "x\ny\\z"
            """ + "\"\"\" multi\nline\ntext \"\"\"" + """
        """ fragmentShouldCompileTo """
            'x';
            '\b';
            '\f';
            '\n';
            '\r';
            '\t';
            '\'';
            '\"';
            '\\';
            '';
            'xyz';
            'x\ny\\z';
            ' multi\nline\ntext ';
        """
    }

    test("Numeric literals") {
        """
            0: Byte
            0: Short
            0: Int
            0L
            0.0F
            0.0
            64: Byte
            2048: Short
            524288: Int
            1099511627776L
            12.34F
            123456789.01234567
            -64: Byte
            -2048: Short
            -524288: Int
            -1099511627776L
            -12.34F
            -123456789.01234567
        """ fragmentShouldCompileTo """
            0;
            0;
            0;
            0;
            0.0;
            0.0;
            64;
            2048;
            524288;
            1099511627776;
            12.34;
            1.2345678901234567E8;
            -64;
            -2048;
            -524288;
            -1099511627776;
            -12.34;
            -1.2345678901234567E8;
        """
    }

    test("Class literals") {
        """
            classOf[Some[_]]
            classOf[Int]
        """ fragmentShouldCompileTo """
            swat.classOf(scala.Some);
            swat.classOf(scala.Int);
        """
    }

    test("XML literals") {
        """
            <foo>
                <foo/>
                <foo attr="bar"/>
                <bar>baz</bar>
                <bar>baz</bar>
            </foo>

            val a = "foo"
            <foo>
                { a }
                <foo attr={ a }/>
                { List("x", "y", "z").map(v => <bar>{ v }</bar>) }
            </foo>
        """ fragmentShouldCompileTo """
            (function() {
                return new scala.xml.Elem(null, 'foo', scala.xml.Null, scala.Predef.$scope(), false, (function() {
                    var $buf = new scala.xml.NodeBuffer(); $buf.$amp$plus(new scala.xml.Text('\n                '), [scala.Any]);
                    $buf.$amp$plus((function() {
                        return new scala.xml.Elem(null, 'foo', scala.xml.Null, scala.Predef.$scope(), true, scala.Predef.wrapRefArray([], scala.xml.Node, [scala.Array]));
                    })(), [scala.Any]);
                    $buf.$amp$plus(new scala.xml.Text('\n                '), [scala.Any]);
                    $buf.$amp$plus((function() {
                        var $md = scala.xml.Null;
                        $md = new scala.xml.UnprefixedAttribute('attr', new scala.xml.Text('bar'), $md);
                        return new scala.xml.Elem(null, 'foo', $md, scala.Predef.$scope(), true, scala.Predef.wrapRefArray([], scala.xml.Node, [scala.Array]));
                    })(), [scala.Any]);
                    $buf.$amp$plus(new scala.xml.Text('\n                '), [scala.Any]);
                    $buf.$amp$plus((function() {
                        return new scala.xml.Elem(null, 'bar', scala.xml.Null, scala.Predef.$scope(), false, (function() {
                            var $buf = new scala.xml.NodeBuffer();
                            $buf.$amp$plus(new scala.xml.Text('baz'), [scala.Any]);
                            return $buf;
                        })());
                    })(), [scala.Any]);
                    $buf.$amp$plus(new scala.xml.Text('\n                '), [scala.Any]);
                    $buf.$amp$plus((function() {
                        return new scala.xml.Elem(null, 'bar', scala.xml.Null, scala.Predef.$scope(), false, (function() {
                            var $buf = new scala.xml.NodeBuffer();
                            $buf.$amp$plus(new scala.xml.Text('baz'), [scala.Any]);
                            return $buf;
                        })());
                    })(), [scala.Any]);
                    $buf.$amp$plus(new scala.xml.Text('\n            '), [scala.Any]);
                    return $buf;
                })());
            })();

            var a = 'foo';
            (function() {
                new scala.xml.Elem(null, 'foo', scala.xml.Null, scala.Predef.$scope(), false, (function() {
                    var $buf = new scala.xml.NodeBuffer();
                    $buf.$amp$plus(new scala.xml.Text('\n                '), [scala.Any]);
                    $buf.$amp$plus(a, [scala.Any]); $buf.$amp$plus(new scala.xml.Text('\n                '), [scala.Any]);
                    $buf.$amp$plus((function() {
                        var $md = scala.xml.Null;
                        $md = new scala.xml.UnprefixedAttribute('attr', a, $md);
                        return new scala.xml.Elem(null, 'foo', $md, scala.Predef.$scope(), true, scala.Predef.wrapRefArray([], scala.xml.Node, [scala.Array]));
                    })(), [scala.Any]);
                    $buf.$amp$plus(new scala.xml.Text('\n                '), [scala.Any]);
                    $buf.$amp$plus(scala.collection.immutable.List.apply(scala.Predef.wrapRefArray(['x', 'y', 'z'], java.lang.String, [scala.Array]), java.lang.String, [scala.collection.Seq]).map((function(v) {
                        return new scala.xml.Elem(null, 'bar', scala.xml.Null, scala.Predef.$scope(), false, (function() {
                            var $buf = new scala.xml.NodeBuffer();
                            $buf.$amp$plus(v, [scala.Any]);
                            return $buf;
                        })());
                    }), scala.collection.immutable.List.canBuildFrom(scala.xml.Elem), scala.xml.Elem, scala.Any, [scala.Function1, scala.collection.generic.CanBuildFrom]), [scala.Any]);
                    $buf.$amp$plus(new scala.xml.Text('\n            '), [scala.Any]);
                    return $buf;
                })());
            })();
        """
    }
}

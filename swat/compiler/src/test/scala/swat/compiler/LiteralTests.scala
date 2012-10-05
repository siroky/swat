package swat.compiler

class LiteralTests extends CompilerSuite
{
    test("Unit and null") {
        """
            ()
            null
        """ fragmentShouldCompileTo """
            undefined;
            null;
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
}

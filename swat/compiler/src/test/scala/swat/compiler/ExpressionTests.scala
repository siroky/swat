package swat.compiler

class ExpressionTests extends CompilerSuite
{
    test("Operators on primitive types compile to primitive operators") {
        """
            val i: Int = 1
            val j: Int = 2
            +i
            -i
            i + j
            i - j
            i * j
            i / j
            i % j
            ~i
            i & j
            i | j
            i ^ j
            i << j
            i >> j
            i >>> j
            i < j
            i > j
            i <= j
            i >= j
            i == j
            i != j
            i equals j

            val b: Double = 1.0
            val c: Double = 2.0
            -b
            b / c
            b < c
            b != c
            b equals c

            val m: Char = 'm'
            val n: Char = 'n'
            -m
            m * n
            ~m
            m & n
            m << n
            m < n
            m >= n
            m != n
            m equals n

            val x: Boolean = true
            val y: Boolean = false
            !x
            x && y
            x || y
            x & y
            x | y
            x ^ y
        """ fragmentShouldCompileTo """
            var i = 1;
            var j = 2;
            +i;
            -i;
            (i + j);
            (i - j);
            (i * j);
            Math.floor((i / j));
            (i % j);
            ~i;
            (i & j);
            (i | j);
            (i ^ j);
            (i << j);
            (i >> j);
            (i >>> j);
            (i < j);
            (i > j);
            (i <= j);
            (i >= j);
            (i == j);
            (i != j);
            (i == j);

            var b = 1.0;
            var c = 2.0;
            -b;
            (b / c);
            (b < c);
            (b != c);
            (b == c);

            var m = 'm';
            var n = 'n';
            -scala.Char.toInt(m);
            (scala.Char.toInt(m) * scala.Char.toInt(n));
            ~scala.Char.toInt(m);
            (scala.Char.toInt(m) & scala.Char.toInt(n));
            (scala.Char.toInt(m) << scala.Char.toInt(n));
            (scala.Char.toInt(m) < scala.Char.toInt(n));
            (scala.Char.toInt(m) >= scala.Char.toInt(n));
            (scala.Char.toInt(m) != scala.Char.toInt(n));
            (m == n);

            var x = true;
            var y = false;
            !x;
            (x && y);
            (x || y);
            Boolean((x & y));
            Boolean((x | y));
            Boolean((x ^ y));
        """
    }
}

package swat.runtime.server

import org.scalatest.FunSuite

class TypeLoaderTests extends FunSuite
{
    test("Types are properly loaded") {
        val result = TypeLoader.get(List("test.Printer"))
        println(result)
    }

    test("Excluded types are really excluded") {

    }
}

package swat.compiler.js

trait TreeBuilder
{
    def memberChain(identifiers: String*): Expression = {
        require(identifiers.length > 1)
        memberChain(Identifier(identifiers.head), identifiers.tail: _*)
    }

    def memberChain(expr: Expression, identifiers: String*): Expression = {
        require(identifiers.length > 0)
        identifiers.map(Identifier(_)).foldLeft[Expression](expr)(MemberExpression(_, _))
    }

    def callStatement(expr: Expression, arguments: Expression*): Statement = {
        ExpressionStatement(CallExpression(expr, arguments))
    }

    def scoped(body: Statement): Expression = scoped(List(body))

    def scoped(body: Seq[Statement]): Expression = {
        CallExpression(FunctionExpression(None, Nil, body), Nil)
    }

    def unscoped(expression: Expression): Seq[Statement] = expression match {
        case CallExpression(FunctionExpression(None, Nil, body), Nil) => body
        case e => List(ExpressionStatement(e))
    }
}

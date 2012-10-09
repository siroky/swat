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

    def methodCall(target: Expression, methodName: String, args: Expression*): Expression = {
        CallExpression(memberChain(target, methodName), args)
    }

    def scoped(body: Statement): Expression = scoped(List(body))

    def scoped(body: Seq[Statement]): Expression = CallExpression(FunctionExpression(None, Nil, body), Nil)

    def unScoped(expression: Expression): Seq[Statement] = expression match {
        case CallExpression(FunctionExpression(None, Nil, body), Nil) => body
        case e => List(ExpressionStatement(e))
    }

    def unScoped(statement: Statement): Seq[Statement] = statement match {
        case ExpressionStatement(e) => unScoped(e)
        case ReturnStatement(Some(CallExpression(FunctionExpression(None, Nil, body), Nil))) => body
        case s => List(s)
    }
}

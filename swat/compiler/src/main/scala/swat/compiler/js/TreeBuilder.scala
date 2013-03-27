package swat.compiler.js

trait TreeBuilder {

    def memberChain(expr: Expression, identifiers: Identifier*): Expression = {
        require(identifiers.length > 0)
        identifiers.foldLeft[Expression](expr)(MemberExpression(_, _))
    }

    def methodCall(target: Expression, methodName: Identifier, args: Expression*): Expression = {
        CallExpression(memberChain(target, methodName), args.toList)
    }

    def scoped(body: Statement): Expression = scoped(List(body))

    def scoped(body: List[Statement]): Expression = CallExpression(FunctionExpression(None, Nil, body), Nil)

    def unScoped(expression: Expression): List[Statement] = expression match {
        case CallExpression(FunctionExpression(None, Nil, body), Nil) => body
        case e => List(ExpressionStatement(e))
    }

    def unScoped(statement: Statement): List[Statement] = statement match {
        case ExpressionStatement(e) => unScoped(e)
        case ReturnStatement(Some(CallExpression(FunctionExpression(None, Nil, body), Nil))) => body
        case s => List(s)
    }

    def throwNew(identifier: Identifier, args: List[Expression]): Statement = {
        ThrowStatement(NewExpression(CallExpression(identifier, args)))
    }
}

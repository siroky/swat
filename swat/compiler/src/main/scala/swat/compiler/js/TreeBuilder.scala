package swat.compiler.js

trait TreeBuilder
{
    def memberChain(identifiers: String*): Expression = {
        require(identifiers.length > 1)
        identifiers.map(Identifier(_)).reduceLeft[Expression](MemberExpression(_, _))
    }

    def callStatement(expr: Expression, arguments: Expression*): Statement = {
        ExpressionStatement(CallExpression(expr, arguments))
    }
}

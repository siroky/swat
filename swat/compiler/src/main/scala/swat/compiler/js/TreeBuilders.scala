package swat.compiler.js

trait TreeBuilders
{
    def memberSelectionChain(identifiers: String*): Expression = {
        require(identifiers.length > 1)
        identifiers.map(Identifier(_)).reduceLeft[Expression](MemberExpression(_, _))
    }
}

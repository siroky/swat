package swat.compiler.js

sealed abstract class Expression

case class CommaExpression(exprs: List[Expression]) extends Expression

sealed abstract class Literal extends Expression

case object NullLiteral extends Literal
case object UndefinedLiteral extends Literal
case class BooleanLiteral(value: Boolean) extends Literal
case class NumericLiteral[A <% Double](value: A) extends Literal
case class StringLiteral(value: String) extends Literal
case class RegExpLiteral(value: String) extends Literal
case class ArrayLiteral(items: Seq[Expression]) extends Literal
case class ObjectLiteral(items: Map[String, Expression]) extends Literal

case class FunctionExpression(
    name: Option[Identifier],
    parameters: Seq[Identifier],
    body: Seq[SourceElement]
) extends Expression

case object ThisReference extends Expression

case class Identifier(name: String) extends Expression

case class MemberExpression(expr: Expression, name: Identifier) extends Expression
case class CallExpression(expr: Expression) extends Expression
case class NewExpression(expr: Expression, arguments: Seq[Expression]) extends Expression

case class PrefixExpression(operator: String, expr: Expression) extends Expression
case class InfixExpression(lhs: Expression, operator: String, rhs: Expression) extends Expression
case class PostfixExpression(expr: Expression, operator: String) extends Expression
case class ConditionalExpression(condition: Expression, thenExpr: Expression, elseExpr: Expression) extends Expression

case class Program(elements: Seq[SourceElement])

sealed abstract class SourceElement

case class FunctionDeclaration(
    name: Identifier,
    parameters: Seq[Identifier],
    body: Seq[SourceElement]
) extends SourceElement

sealed abstract class Statement extends SourceElement

case class Block(stmts: Seq[Statement]) extends Statement

case class VariableStatement(variables: List[(Identifier, Option[Expression])]) extends Statement

case object EmptyStatement extends Statement

case class ExpressionStatement(expr: Expression) extends Statement

case class IfStatement(condition: Expression, thenStmts: Seq[Statement], elseStmts: Seq[Statement]) extends Statement

case class WhileStatement(
    condition: Expression,
    body: Seq[Statement],
    isDoWhile: Boolean = false
) extends Statement

case class ForStatement(
    initializer: Option[Expression],
    condition: Option[Expression],
    step: Option[Expression],
    body: Seq[Statement]
) extends Statement

case class ForeachStatement(
    itemDeclaration: VariableStatement,
    container: Expression,
    body: Seq[Statement]
) extends Statement

case object ContinueStatement extends Statement

case object BreakStatement extends Statement

case class ReturnStatement(value: Option[Expression]) extends Statement

case class WithStatement(environment: Expression, body: Seq[Statement]) extends Statement

case class SwitchStatement(
    expr: Expression,
    cases: Seq[(Expression, Seq[Statement])],
    default: Seq[Statement]
) extends Statement

case class LabelledStatement(name: Identifier, body: Seq[Statement]) extends Statement

case class ThrowStatement(expr: Expression) extends Statement

case class TryStatement(
    body: Seq[Statement],
    catcher: Option[(Identifier, Seq[Statement])],
    finalizer: Seq[Statement]
) extends Statement

case object DebuggerStatement extends Statement

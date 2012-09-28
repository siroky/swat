package swat.compiler.backend

import swat.compiler.js._

class CodeGenerator
{
    def run(ast: Ast): String = process(ast)(Indent("    "))

    private def process(ast: Ast)(implicit indent: Indent): String = {
        ast match {
            case Program(elements) => elements.map(process _).mkString("\n\n")
            case FunctionDeclaration(name, parameters, body) => {
                "function " + process(name) + process(parameters).mkString("(", ", ", ")") +
                    processBlock(body) + "\n"
            }
            case stmt: Statement => processStatement(stmt)
            case expr: Expression => processExpression(expr)
        }
    }

    private def process(ast: Option[Ast])(implicit indent: Indent): String = ast.map(process(_)).mkString

    private def process(asts: Seq[Ast])(implicit indent: Indent): Seq[String] = asts.map(process(_))

    private def processStatement(statement: Statement)(implicit indent: Indent): String = {
        indent + (statement match {
            case Block(stmts) => processBlock(stmts)
            case VariableStatement(variables) => {
                "var " + variables.map(v => process(v._1) + " = " + process(v._2)).mkString(", ") + ";"
            }
            case EmptyStatement => ";"
            case AssignmentStatement(target, expr) => process(target) + " = " + process(expr) + ";"
            case ExpressionStatement(expr) => "(" + expr + ");"
            case IfStatement(condition, thenStmts, elseStmts) => {
                "if (" + process(condition) + ") " + processBlock(thenStmts) +
                elseStmts.map(e => " else " + processBlock(e)).mkString
            }
            case WhileStatement(condition, body, isDoWhile) => {
                val processedCondition = "while (" + process(condition) + ")"
                (if (isDoWhile) "do" else processedCondition) + " " + processBlock(body) +
                (if (isDoWhile) " " + processedCondition else "")
            }
            case ForStatement(initializer, condition, step, body) => {
                "for (" + process(initializer) + "; " + process(condition) + "; " + process(step) + ") " +
                    processBlock(body)
            }
            case ForeachStatement(VariableStatement(variables), container, body) => {
                "for (var " + process(variables.head._1) + " in " + process(container) + ") " + processBlock(body)
            }
            case ContinueStatement => "continue;"
            case BreakStatement => "break;"
            case ReturnStatement(value) => "return " + process(value) + ";"
            case WithStatement(environment, body) => "with (" + process(environment) + ") " + processBlock(body)
            case SwitchStatement(expr, cases, default) => {
                "switch (" + process(expr) + ") {\n" +
                    cases.map { c =>
                        indent.increased + "case " + process(c._1) + ": " + processBlock(c._2)(indent.increased) + "\n"
                    }.mkString
                    default.map { d =>
                        indent.increased + "default: " + processBlock(d)(indent.increased) + "\n"
                    }.mkString
                indent + "}"
            }
            case LabelledStatement(name, body) => process(name) + ": " + processBlock(body)
            case ThrowStatement(expr) => "throw " + process(expr) + ";"
            case TryStatement(body, catcher, finalizer) => {
                "try " + processBlock(body) +
                catcher.map(c => " catch (" + process(c._1) + ") " + processBlock(c._2)).mkString +
                finalizer.map(f => " finally " + processBlock(f))
            }
            case DebuggerStatement => "debugger;"
        }) + "\n"
    }

    private def processBlock(stmts: Seq[Statement])(implicit indent: Indent): String = {
        if (stmts.isEmpty) "{ }" else {
            "{\n" +
                process(stmts)(indent.increased).mkString
            indent + "}"
        }
    }

    private def processExpression(expression: Expression)(implicit indent: Indent): String = {
        expression match {
            case CommaExpression(exprs) => process(exprs).mkString(", ")
            case literal: Literal => processLiteral(literal)
            case FunctionExpression(name, parameters, body) => {
                "function " + process(name) + process(parameters).mkString("(", ", ", ")") + "{\n" +
                process(body)(indent.increased) +
                indent + "}"
            }
            case ThisReference => "this"
            case Identifier(name) => name
            case MemberExpression(expr, name) => process(expr) + "." + process(name)
            case CallExpression(expr, parameters) => process(expr) + process(parameters).mkString("(", ", ", ")")
            case NewExpression(constructor) => "new " + process(constructor)
            case PrefixExpression(operator, expr) => operator + process(expr)
            case InfixExpression(lhs, operator, rhs) => "(" + process(lhs) + " " + operator + " " + process(rhs) + ")"
            case PostfixExpression(expr, operator) => process(expr) + operator
            case ConditionalExpression(condition, thenExpr, elseExpr) => {
                "(" + process(condition) + " ? " + process(thenExpr) + " : " + process(elseExpr) + ")"
            }
        }
    }

    private def processLiteral(literal: Literal)(implicit indent: Indent): String = {
        literal match {
            case NullLiteral => "null"
            case UndefinedLiteral => "undefined"
            case BooleanLiteral(value) => if (value) "true" else "false"
            case NumericLiteral(value) => value.toString
            case StringLiteral(value) => "'" + escapeString(value) + "'"
            case RegExpLiteral(pattern, modifiers) => "/" + pattern + "/" + modifiers
            case ArrayLiteral(items) => process(items).mkString("[", ", ", "]")
            case ObjectLiteral(items) => {
                items.map { case (n, v) => "'%s': %s".format(n, process(v)) }.mkString("{", ", ", "}")
            }
        }
    }

    private def escapeString(value: String) = {
        val replacementMap = Map(
            '\\' -> """\\""",
            '\b' -> """\b""",
            '\f' -> """\f""",
            '\n' -> """\n""",
            '\r' -> """\r""",
            '\t' -> """\t""",
            '\'' -> """\'""",
            '\"' -> """\""""
        ).withDefault(c => c)

        value.map(replacementMap).mkString
    }
}

private case class Indent(step: String, value: String = "")
{
    def increased = Indent(step, value + step)

    override def toString = value
}
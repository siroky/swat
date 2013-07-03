package swat.compiler.backend

import swat.compiler.js._

class JsCodeGenerator extends Backend {

    def astToCode(ast: Ast): String = process(ast)(Indent("    "))

    private def astIsEmpty(ast: Ast): Boolean = ast match {
        case EmptyStatement => true
        case ExpressionStatement(UndefinedLiteral) => true
        case VariableStatement(List((i, Some(v)))) => i == v
        case _ => false
    }

    private def astsAreEmpty(asts: List[Ast]): Boolean = asts.foldLeft(true)(_ && astIsEmpty(_))

    private def process(ast: Ast)(implicit indent: Indent): String = {
        if (astIsEmpty(ast)) {
            ""
        } else {
            ast match {
                case Program(elements) => elements.map(process _).mkString
                case stmt: Statement => processStatement(stmt)
                case expr: Expression => processExpression(expr)
            }
        }
    }

    private def process(ast: Option[Ast])(implicit indent: Indent): String = ast.map(process(_)).mkString

    private def process(asts: List[Ast])(implicit indent: Indent): List[String] = {
        val (liveAsts, deadAsts) = asts.span {
            case _: ReturnStatement | _: ThrowStatement => false
            case _ => true
        }

        // Omit all statements after the first occurance of return or throw statement.
        (liveAsts ++ deadAsts.headOption.toList).map(process(_))
    }

    private def processStatement(statement: Statement)(implicit indent: Indent): String = {
        indent + (statement match {
            case Block(stmts) => processBlock(stmts, enclosed = false)
            case RawCodeBlock(code) => {
                val lines = code.lines.toList
                val indentPrefix = lines.headOption.map(_.takeWhile(_ == ' ')).getOrElse("")
                if (lines.forall(_.startsWith(indentPrefix))) {
                    lines.map(indent + _.drop(indentPrefix.length)).mkString("\n")
                } else {
                    code
                }
            }
            case FunctionDeclaration(name, parameters, body) => {
                "function " + process(name) + process(parameters).mkString("(", ", ", ") ") +
                    processBlock(body) + ";"
            }
            case VariableStatement(variables) => {
                "var " + variables.map(v => process(v._1) + " = " + process(v._2)).mkString(", ") + ";"
            }
            case EmptyStatement => ";"
            case AssignmentStatement(target, expr) => process(target) + " = " + process(expr) + ";"
            case ExpressionStatement(expr) => process(expr) + ";"
            case IfStatement(condition, thenStmts, elseStmts) => {
                "if (" + process(condition) + ") " + processBlock(thenStmts) +
                (if (astsAreEmpty(elseStmts)) "" else " else " + processBlock(elseStmts))
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
            case ReturnStatement(value) => value match {
                case Some(UndefinedLiteral) => ""
                case _ => "return" + value.map(" " + process(_)).getOrElse("") + ";"
            }
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
                finalizer.map(f => " finally " + processBlock(f)).mkString
            }
            case DebuggerStatement => "debugger;"
        }) + "\n"
    }

    private def processBlock(stmts: List[Statement], enclosed: Boolean = true)(implicit indent: Indent): String = {
        if (stmts.isEmpty) {
            if (enclosed) "{ }" else ""
        } else {
            val (start, bodyIndent, end) = if (enclosed) ("{\n", indent.increased, indent + "}") else ("", indent, "")
            start +
                process(stmts)(bodyIndent).mkString +
            end
        }
    }

    private def processExpression(expression: Expression)(implicit indent: Indent): String = {
        expression match {
            case CommaExpression(exprs) => process(exprs).mkString(", ")
            case literal: Literal => processLiteral(literal)
            case RawCodeExpression(code) => code
            case FunctionExpression(name, parameters, body) => {
                val processedName = name.map(" " + process(_)).getOrElse("")
                "(function" + processedName + process(parameters).mkString("(", ", ", ") ") +
                    processBlock(body) + ")"
            }
            case ThisReference => "this"
            case Identifier(name) => name
            case MemberExpression(expr, name) => process(expr) match {
                case "" => process(name)
                case e => e + "." + process(name)
            }
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
            '\"' -> """\"""")

        value.map(replacementMap.withDefault(c => c)).mkString
    }
}



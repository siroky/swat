package swat.compiler.backend

import swat.compiler.js.Ast

trait Backend {
    def astToCode(ast: Ast): String
}

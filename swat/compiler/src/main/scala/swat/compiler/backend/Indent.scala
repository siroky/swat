package swat.compiler.backend

case class Indent(step: String, value: String = "") {
    def increased = Indent(step, value + step)

    override def toString = value
}

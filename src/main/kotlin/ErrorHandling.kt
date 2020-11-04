data class Error(
        val message: String,
        val startPos: TokenPos,
        val endPos: TokenPos
)

class ErrorHandling(val input: String, val path: String){
    private val errors = arrayListOf<Error>()
    val hasError get() = errors.isNotEmpty()

    fun pushError(message: String, startPos: TokenPos, endPos: TokenPos){
        errors += Error(message, startPos, endPos)
    }

    override fun toString(): String =
            buildString {
                errors.forEach {
                    appendLine("${it.startPos.line}:${it.startPos.col} - ${it.message}")
                    input.lines()
                            .withIndex()
                            .filter {
                                (idx, _) ->
                                idx == it.startPos.line - 1
                            }
                            .forEach { (_, line) ->
                                appendLine(line)
                            }
                    for(i in 1 until it.startPos.col){
                        append(' ')
                    }
                    for(i in it.startPos.offset until it.endPos.offset){
                        append('~')
                    }
                    appendLine()
                }
            }
}
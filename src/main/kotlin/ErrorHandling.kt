/**
 * This represents an error, and the source code range in which it resides. This is useful for creating
 * a squiggly line underneath that range of characters.
 *
 * @param message The message to be displayed above the code
 * @param startPos The starting position of the code throwing the error
 * @param endPos The ending position of the code throwing the error
 */
data class Error(
        val message: String,
        val startPos: TokenPos,
        val endPos: TokenPos
)

/**
 * This is used for gathering error messages before being displayed to the user. This will use the [input] parameter
 * for getting the line of code the error exists, and then display a squiggly line underneath the problematic part
 * of the code from the startPos to the endPos.
 */
class ErrorHandling(val input: String, val path: String){
    private val errors = arrayListOf<Error>()
    val hasError get() = errors.isNotEmpty()

    fun pushError(message: String, startPos: TokenPos, endPos: TokenPos){
        errors += Error(message, startPos, endPos)
    }

    override fun toString(): String =
            buildString {
                errors.forEach {
                    appendLine("$path - ${it.startPos.line}:${it.startPos.col} - ${it.message}")
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
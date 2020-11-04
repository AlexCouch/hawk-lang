import java.io.File
import java.util.*

/**
 * A *tokenizer* is an algorithm for converting a character string (a string in this case) into a stream of tokens.
 * A [Token] is a data structure that represents a recognized primitive piece of a grammar, called a lexicon.
 * Within grammars (see [parser.ParseRule] for info on grammars), we have a lexicon. A lexicon provides the pieces to build
 * a grammar. Within that lexicon, we have
 *  - Identifiers
 *  - ASCII
 *  - Whitespace
 *  - Digits
 *  - Punctuation
 *
 * Depending on your lexicon, you may treat these differently. Some lexicons count whitespace such as tabs as parts of
 * the grammar (see [symres.SymbolTable] for info on scoping). In this lexicon, we are focusing on Identifiers, Digits,
 * and Punctuation. Digits are just collections of individual digits, and punctuation is all non-letter and non-digit and
 * non-whitespace character. See [Tokenizer.punctuation] for a list of recognized punctuation.
 *
 * This tokenizer will iterate through each character and scan for different kinds of lexical rules
 *
 * Is this whitespace? Advance to next character, essentially skipping over it.
 * Is this a letter? Build a substring out of every next character until we reach a non-letter and non-digit.
 * For identifiers, we may recognize digits inside of it as long as we first establish the first character as a letter.
 * Valid identifiers must start with a letter, and be preceeded by zero or more letters or digits.
 * Punctuation are recognized as units, not as groups like identifiers and digits and whitespace. If we punctuation
 * is recognized, it will immediately be built into a token, and we advance to the next character, regardless if the next
 * character is also a punctuation.
 *
 * After we reach the end of the string, we will return the composed [TokenStream] back to the caller.
 */
class Tokenizer(private val path: String, private val input: String){
    private var inputPos = 0
    private var char = input[inputPos]
    private var position: TokenPos = TokenPos(1, 1, inputPos, path)

    private fun advance(){
        inputPos++
        if(inputPos !in input.indices){
            return
        }
        char = input[inputPos]
    }

    private fun nextCol(){
        advance()
        position = TokenPos(position.line, position.col + 1, inputPos, path)
    }

    private fun nextLine(){
        advance()
        position = TokenPos(position.line + 1, 1, inputPos, path)
    }

    private val Char.punctuation: Token<*>?
        get()=
            when(this){
                '!' -> Token(TokenKind.Bang, null, position, position)
                '@' -> Token(TokenKind.At, null, position, position)
                '#' -> Token(TokenKind.Hash, null, position, position)
                '$' -> Token(TokenKind.Dollar, null, position, position)
                '%' -> Token(TokenKind.Mod, null, position, position)
                '^' -> Token(TokenKind.Caret, null, position, position)
                '&' -> Token(TokenKind.Amp, null, position, position)
                '*' -> Token(TokenKind.Star, null, position, position)
                '(' -> Token(TokenKind.LParen, null, position, position)
                ')' -> Token(TokenKind.RParen, null, position, position)
                '-' -> Token(TokenKind.Hyphen, null, position, position)
                '_' -> Token(TokenKind.Underscore, null, position, position)
                '=' -> Token(TokenKind.Equal, null, position, position)
                '+' -> Token(TokenKind.Plus, null, position, position)
                '[' -> Token(TokenKind.LSquare, null, position, position)
                ']' -> Token(TokenKind.RSquare, null, position, position)
                '{' -> Token(TokenKind.LCurly, null, position, position)
                '}' -> Token(TokenKind.RCurly, null, position, position)
                ';' -> Token(TokenKind.Semicolon, null, position, position)
                ':' -> Token(TokenKind.Colon, null, position, position)
                '\'' -> Token(TokenKind.Apost, null, position, position)
                '"' -> Token(TokenKind.Quote, null, position, position)
                '<' -> Token(TokenKind.LAngle, null, position, position)
                '>' -> Token(TokenKind.RAngle, null, position, position)
                '?' -> Token(TokenKind.Question, null, position, position)
                ',' -> Token(TokenKind.Comma, null, position, position)
                '.' -> Token(TokenKind.Dot, null, position, position)
                '/' -> Token(TokenKind.FSlash, null, position, position)
                '\\' -> Token(TokenKind.BSlash, null, position, position)
                '|' -> Token(TokenKind.Pipe, null, position, position)
                '`' -> Token(TokenKind.Tick, null, position, position)
                '~' -> Token(TokenKind.Tilde, null, position, position)
                else -> null
            }

    fun start(): TokenStream{
        val stream = TokenStream()
        while(inputPos in input.indices){
            when{
                char == '\n' -> {
                    nextLine()
                    continue
                }
                char == ' ' -> {
                }
                char == '\t' -> {
                    for(i in 0 until 3){
                        nextCol()
                    }
                }
                char.isDigit() -> {
                    val start = position
                    val digit = buildString {
                        do{
                            append(char)
                            nextCol()
                        }while(char.isDigit() && inputPos in input.indices)
                    }
                    try{
                        stream.addToken(Token(TokenKind.Integer, digit.toInt(), start, position))
                    }catch(e: Exception){
                        throw RuntimeException(e)
                    }
                }
                char.isLetter() -> {
                    val start = position
                    val ident = buildString {
                        do{
                            append(char)
                            nextCol()
                        }while((char.isLetterOrDigit() || char == '_') && inputPos in input.indices)
                    }
                    try{
                        stream.addToken(Token(TokenKind.Identifier, ident, start, position))
                    }catch(e: Exception){
                        throw RuntimeException(e)
                    }
                }
                char.punctuation != null -> {
                    stream.addToken(char.punctuation!!)
                }
            }
            nextCol()
        }
        return stream
    }

    companion object{
        fun load(path: String) =
            load(java.io.File(path))
        fun load(file: File) =
            Tokenizer(file.path, file.readText())
    }
}

/**
 * A list of kinds of tokens. This is to simplify code when checking for what kind of token we have detected inside the
 * parser.
 */
enum class TokenKind{
    Identifier,
    Integer,
    Float,
    Equal,
    Plus,
    Hyphen,
    Underscore,
    Star,
    FSlash,
    BSlash,
    Bang, At, Hash, Dollar, Mod, Caret, Amp,
    LParen, RParen,
    Tick,
    Tilde,
    RAngle, LAngle,
    Comma, Dot,
    Question,
    Semicolon, Colon,
    Apost,
    Quote,
    LCurly, RCurly, LSquare, RSquare,
    Pipe,
}

/**
 * An individual lexical piece of an entire grammar. See [Tokenizer] for info on how it's produced, and how it's used.
 */
data class Token<T>(val kind: TokenKind, val data: T? = null, val startPos: TokenPos, val endPos: TokenPos)

/**
 * The position in the string a token resides. This is useful for debugging on the user's side and on the implementation
 * developer's side. A token pos has a 1-based line and column. A line is incremented when a linefeed is detected, and
 * the column is returned back to 1. A column is incremented at every non-linefeed scan inside the [Tokenizer].
 * The [offset] is used for debugging where exactly in the string this token exists, and when used with another [TokenPos],
 * and entire range within the string can be created and used within an error handler to show the exact code along with an
 * annotation/message describing the problem.
 */
data class TokenPos(val line: Int, val col: Int, val offset: Int, val path: String)

/**
 * An iterator over an [ArrayList] of [Token]'s. This iterator provides a peek token, a set-private [tokenPtr]
 * for getting the current [Token], and an implementation of [Iterator] such that [hasNext] checks if the next
 * value of [tokenPtr] is within the [Collection.indices] of [tokens]. [next] will increment [tokenPtr] before returning
 * back the token at the index within [tokens]. If [hasNext] is false, [next] will return null. [peek] just simulates
 * a [next] without incrementing.
 */
class TokenStream: Iterator<Token<*>?>{
    private val tokens = arrayListOf<Token<*>>()
    var tokenPtr = -1
        private set

    val peek get() = if(hasNext()) tokens[tokenPtr+1] else null

    private var checkpoint = LinkedList<Int>()

    /**
     * Check if the next index ([tokenPtr] + 1) is within the bounds of the tokens size
     * If it's less than [tokens], return true
     */
    override fun hasNext(): Boolean =
        tokenPtr + 1 in tokens.indices

    /**
     * Checks if [hasNext] is true, if so, increment [tokenPtr] then return the element in [tokens] at [tokenPtr]
     * Otherwise, return null
     */
    override fun next(): Token<*>? =
        if(hasNext()) tokens[++tokenPtr] else null

    fun saveCheckpoint(){
        checkpoint.add(tokenPtr)
    }

    fun restoreCheckpoint(){
        tokenPtr = checkpoint.removeLast()
    }

    fun popCheckpoint(){
        checkpoint.removeLast()
    }

    fun addToken(token: Token<*>) = tokens.add(token)

}
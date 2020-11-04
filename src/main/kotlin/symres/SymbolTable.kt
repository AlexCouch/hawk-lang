package symres

import TokenPos
import parser.ASTKind
import parser.ASTNode
import java.util.*
import kotlin.collections.ArrayList

/**
 * A property of the symbol. This is here for demonstrative purposes also as a hint for anyone looking to improve the
 * compiler architecture.
 *
 * One thing about type inference and checking is that, instead of producing a type map, properties describing
 * the types of symbols as a means of producing a map within the symbol table could be done as an alternative.
 *
 * This could also be used for optimizations like SSA/CFA.
 */
interface SymbolProperty

/**
 * A representation of a symbolic grammar element, such as variables. This provides the start and end positions, and
 * also has properties.
 *
 * @see SymbolProperty
 */
data class Symbol(val ident: String, val startPos: TokenPos, val endPos: TokenPos, val properties: ArrayList<SymbolProperty> = arrayListOf()){
    override fun toString(): String =
            buildString {
                appendLine("Ident: $ident")
                appendLine("Pos: ${startPos.line}:${startPos.col}-${endPos.line}:${endPos.col}")
                appendLine("Properties: [")
                properties.forEach {
                    append("\t")
                    appendLine(it.toString())
                }
                appendLine("]")
            }
}

/**
 * This represents a source level scope. Scopes are let-do blocks where variables are declared and used within expressions.
 * Scopes could be identified for any reason. Scope identifiers can be randomly generated or be named after the named
 * grammar element providing the scope, such as functions. This identifier can be used for dumping the symbol table
 * to see what scopes currently exist and what symbols are bound to them.
 */
data class Scope(val ident: String, val symbols: ArrayList<Symbol> = arrayListOf())

/**
 * A table of symbols and scopes. This keeps a [LinkedList] of [Scope]'s which are then used for defining and keeping
 * track of symbols and when they exist within the code. See [SymbolResolution] for more info on how symbols are defined
 * and analysed.
 */
class SymbolTable{
    /**
     * A stack of scopes, which are never freed but are instead kept for reuse during other passes.
     */
    private val scopeStack = LinkedList<Scope>()

    /**
     * Which index in the [scopeStack] we are currently working in. This is used for defining and finding symbols
     */
    private var scopeStackPtr = -1


    /**
     * Creates a new scope and enters it via [enterScope]
     */
    fun createScope(ident: String) {
        scopeStack.add(Scope(ident))
        enterScope()
    }

    /**
     * Enter the next scope
     */
    fun enterScope(){
        scopeStackPtr++
    }

    /**
     * Leave and enter the previous scope
     */
    fun leaveScope(){
        scopeStackPtr--
    }

    /**
     * Define a symbol on the [scopeStack] at the current [index][scopeStackPtr]
     *
     * This does not do any checking for duplication because name shadowing is allowed currently.
     * This is coupled with [findSymbol] when searching from the current scope upwards.
     */
    fun defineSymbol(node: ASTNode<String>) =
        if(node.astKind != ASTKind.Identifier) false
        else scopeStack[scopeStackPtr].symbols.add(Symbol(node.data!!, node.startPos, node.endPos))

    /**
     * Find a symbol within the current scope stack up to the current [index][scopeStackPtr].
     * This searches from the current scope up, so that it can find the most recently defined symbol.
     * This is because name shadowing is allowed and the most recent symbol is most likely the symbol desired.
     *
     * However, name shadowing can often get in the way if you'd like to reference an older version of the same symbol.
     */
    fun findSymbol(ident: String): Symbol?{
        for(scope in scopeStack.slice(0..scopeStackPtr).reversed()){
            val symbol = scope.symbols.find { sym ->
                sym.ident == ident
            }
            if(symbol != null){
                return symbol
            }
        }
        return null
    }


}
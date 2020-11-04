package symres

import TokenPos
import parser.ASTKind
import parser.ASTNode
import java.util.*
import kotlin.collections.ArrayList

interface SymbolProperty
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
data class Scope(val ident: String, val symbols: ArrayList<Symbol> = arrayListOf())

class SymbolTable{
    private val scopeStack = LinkedList<Scope>()
    private var scopeStackPtr = -1

    fun createScope(ident: String) {
        scopeStack.add(Scope(ident))
        enterScope()
    }

    fun enterScope(){
        scopeStackPtr++
    }

    fun leaveScope(){
        scopeStackPtr--
    }

    fun defineSymbol(node: ASTNode<String>) =
        if(node.astKind != ASTKind.Identifier) false
        else scopeStack[scopeStackPtr].symbols.add(Symbol(node.data!!, node.startPos, node.endPos))

    fun findSymbol(ident: String): Symbol?{
        for(scope in scopeStack.slice(0..scopeStackPtr)){
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
import PrettyPrinter
import parser.ASTNode
import parser.walk
import parser.walkChildren
import symres.SymbolTable
import typeck.TypeMap
import typeck.TypeMapNode

class ASTDebug{
    @ExperimentalStdlibApi
    fun debugAST(ast: ASTNode<*>): String =
         buildPrettyString {
            ast.walk {
                appendWithNewLine("Kind: ${it.astKind}")
                appendWithNewLine("Start Pos: ${it.startPos}")
                appendWithNewLine("End Pos: ${it.endPos}")
                appendWithNewLine("Children: [")
                indent {
                    it.walkChildren { child ->
                        indent{
                            append(debugAST(child))
                        }
                    }
                }
                appendWithNewLine("]")
            }
        }
}

class SymTabDebug{
    @ExperimentalStdlibApi
    fun debugSymtab(symtab: SymbolTable): String =
            buildPrettyString {
                symtab.scopeStack.forEach {
                    appendWithNewLine("========================= ${it.ident} =========================")
                    it.symbols.forEach { sym ->
                        appendWithNewLine(sym.ident)
                        appendWithNewLine("Start Pos: ${sym.startPos}")
                        appendWithNewLine("End Pos: ${sym.endPos}")
                        appendWithNewLine("Properties: [")
                        indent {
                            sym.properties.forEach { prop ->
                                appendWithNewLine(prop.toString())
                            }
                        }
                        appendWithNewLine("]")
                    }
                }
            }
}

class TypemapDebug{
    @ExperimentalStdlibApi
    fun debugTypemap(tymap: TypeMap): String =
            buildPrettyString {
                tymap.nodes.forEach {
                    append("Id: ${it.id} - Ident: ${it.symbol.ident} - ${it.nodeKind} - ${it.type.typeName}")
                    if(it is TypeMapNode.TypeMapBranch){
                        it.children.forEach {
                            appendWithNewLine(  " => Id: ${it.id} - Ident: ${it.symbol.ident} - ${it.nodeKind} - ${it.type.typeName}")
                        }
                    }
                    appendWithNewLine("")
                }
            }
}
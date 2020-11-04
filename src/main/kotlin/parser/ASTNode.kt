package parser

import TokenPos

/**
 * A list of kinds of AST. Some aren't currently used but that is okay. This is not meant to represent a final list of
 * kinds of AST nodes. Every [ASTNode] must have an entry here as its value for [ASTNode.astKind].
 */
enum class ASTKind{
    Identifier,
    Let,
    Do,
    Var,
    Expression,
    VarRef,
    IntLiteral,
    BinaryPlus,
    BinaryMinus,
    BinaryMul,
    BinaryDiv
}

/**
 * A node within an Abstract Syntax Tree. An Abstract Syntax Tree is a tree produced by a parser (see [parser.ParseRule]
 * after the grammar of the input has been validated with a parse tree. (see [parser.ParseRule] for Parse Tree vs AST.
 *
 * Every node within the AST has a [ASTKind] for checking what kind of node this is.
 *
 * Not all nodes may have a [parent].
 * Nodes such as [ASTKind.Let] may not have a parent, however, variables have parents of [ASTKind.Let].
 * [ASTKind.Expression] based nodes will have some kind of parent, such as a [ASTKind.Do] or [ASTKind.Expression] (binary
 * expressions, compounds, etc) or [ASTKind.Var].
 *
 * Not all nodes will have children. For example, [ASTKind.IntLiteral] will not have children. Same with [ASTKind.Identifier].
 * Some nodes such as [ASTKind.Var] and binary [ASTKind.Expression]'s have children. [ASTKind.Let] and [ASTKind.Do] also
 * have children.
 *
 * Some AST nodes may contain [data] but most don't. [ASTKind.IntLiteral] has [Int] data, while [ASTKind.Identifier]
 * has [String] data. Everything else has null [data]. This data can be used for assigning types or generating bytecode
 * that pushes data onto the stack.
 *
 * [startPos] and [endPos] are immediately borrowed from its corresponding token substream. The [startPos] indicates where
 * in the source file this node starts, and [endPos] indicates where it ends. This is used for debugging and reporting
 * errors to the user.
 *
 */
data class ASTNode<T>(
        val astKind: ASTKind,
        val parent: ASTNode<*>? = null,
        val children: ArrayList<ASTNode<*>>,
        val data: T? = null,
        val startPos: TokenPos,
        val endPos: TokenPos
)

fun ASTNode<*>.assignParents(): ASTNode<*>{
    return transformChildren{
        it.transformASTParent {
            this
        }
        it.assignParents()
    }
}

fun ASTNode<*>.walk(block: (ASTNode<*>) -> Unit){
    block(this)
    walkChildren(block)
}

fun ASTNode<*>.walkChildren(block: (ASTNode<*>) -> Unit){
    children.forEach {
        it.walk(block)
    }
}

/**
 * This allows for easy transformation of an [ASTNode]
 */
fun <T, R> ASTNode<T>.transformASTNode(block: (ASTNode<T>) -> ASTNode<R>): ASTNode<R> =
        block(this)

fun <T> ASTNode<T>.transformASTParent(block: (ASTNode<*>?) -> ASTNode<*>): ASTNode<T> =
        transformASTNode {
            val parent = block(it.parent)
            ASTNode(it.astKind, parent, it.children, it.data, it.startPos, it.endPos)
        }

fun <T> ASTNode<T>.transformChildren(block: (ASTNode<*>) -> ASTNode<*>): ASTNode<T> =
        transformASTNode {
            val children = it.children.map { child ->
                val newChild = block(child)
                newChild.transformASTParent {
                    this
                }
            }
            ASTNode(it.astKind, it.parent, ArrayList(children), it.data, it.startPos, it.endPos)
        }

fun <T, R> ASTNode<T>.transformData(block: (T?) -> R?): ASTNode<R> =
        transformASTNode {
            val data = block(it.data)
            ASTNode(it.astKind, it.parent, it.children, data, it.startPos, it.endPos)
        }
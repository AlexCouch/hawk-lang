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

/**
 * This will transform all the children to reassign each parent to each of the children. This should only be used
 * when you have transformed the parent using one of the transform methods below. The methods result in the parent
 * and its children being separated, and the children still remember their original parent.
 *
 * This may seem quite inefficient, but kotlin does a lot of optimizations. In languages like C/C++, you would *NEVER
 * EVER DO THIS EVER*. This would mean having to copy and move references and do a tree traversal for every single child,
 * change references of each parent in each child to the new parent. Every time you transform the children of a parent,
 * you get new children replacing the old ones.
 *
 * It is genereally accepted that you shouldn't do this in a lower level language, but kotlin makes it easier for us
 * to use this pattern without performance degradations. Due to Kotlin's functional nature, and the AST's being data classes,
 * this is a more suitable approach.
 *
 * Be wise in other languages, like Java or C#. Do not use this kind of algorithm and instead use direct field mutations.
 * You'll thank me later.
 */
fun ASTNode<*>.assignParents(): ASTNode<*>{
    return transformChildren{
        it.transformASTParent {
            this
        }
        it.assignParents()
    }
}

/**
 * A simple walk method, which applies a callback to each node visited. Every node being walked will walk the children
 * and apply the same callback as wel.
 */
fun ASTNode<*>.walk(block: (ASTNode<*>) -> Unit){
    block(this)
    walkChildren(block)
}

/**
 * Walks the children and calls their [walk] method, applying the [block] callback. This recursively calls [walk],
 * as [walk] calls this method as well.
 */
fun ASTNode<*>.walkChildren(block: (ASTNode<*>) -> Unit){
    children.forEach {
        it.walk(block)
    }
}

/**
 * This allows for easy transformation of an [ASTNode]. This is more of a simple transformation that can be
 * used in a method chain.
 */
fun <T, R> ASTNode<T>.transformASTNode(block: (ASTNode<T>) -> ASTNode<R>): ASTNode<R> =
        block(this)

/**
 * This is a wrapper around [transformASTNode] but instead calls [block] callback and transforms this
 * [ASTNode] so that the result of [block] becomes this node's new parent.
 */
fun <T> ASTNode<T>.transformASTParent(block: (ASTNode<*>?) -> ASTNode<*>): ASTNode<T> =
        transformASTNode {
            val parent = block(it.parent)
            ASTNode(it.astKind, parent, it.children, it.data, it.startPos, it.endPos)
        }

/**
 * This is used for iterating each child and applying a transformation [block], which results in a new [ASTNode]
 * that will replace the old child, and changing its parent to the current node.
 */
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

/**
 * This is used for transforming the data of an [ASTNode]. This can be used for optimization passes.
 * This will pass into [block] the old data and replacing it with the result of [block].
 */
fun <T, R> ASTNode<T>.transformData(block: (T?) -> R?): ASTNode<R> =
        transformASTNode {
            val data = block(it.data)
            ASTNode(it.astKind, it.parent, it.children, data, it.startPos, it.endPos)
        }
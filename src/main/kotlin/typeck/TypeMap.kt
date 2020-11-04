package typeck

import symres.Symbol

/**
 * A representation of a type. This contains a unique id and a name. Currently, id's are generated dynamically based on
 * use, however, this can be improved upon with the addition of user defined data structures so that [id] is based on
 * the declared type itself rather than when its used.
 */
data class Type(val id: Int, val typeName: String){
    companion object{
        /**
         * Get a dynamically typed representation. This is used for pre-typing and checking if something has been typed
         */
        fun getDyn(id: Int) = Type(id, "dyn")
    }
}

/**
 * What kind of [TypeMap] node this is. [Assignment] means it's a first time definition, and should be typed based on its
 * expression against all other possible uses. [Reassignment] is for mutations of variables so that when checking the
 * assigned expression of the variable reassignment, it must be matched to a previously typed node of the same variable.
 *
 * If we create a variable called 'age' and set it to '22', and then later set it to '23', the setter will tell the
 * type checker to go and look at the assignment form, higher up in the tree, defined earlier. If the reassignment type
 * matches the assignment type, then it can pass, otherwise an error should be produced.
 *
 * Note: Reassignments and set blocks have not be implemented. Let this be an exercise.
 */
enum class TypeMapNodeKind{
    Assignment,
    Reassignment
}

/**
 * A node within the type map. This can either be a branch or a leaf. A leaf is a typed element such as a variable that
 * has no references to other variables within its assignment.
 * ```
 *  let
 *      a = 5
 *  do
 *      a
 * ```
 * `a` will be a leaf since it's only assignment is a constant. In a language with data structures, if `a` were assigned
 * an instance of a data structure, it would still be a leaf because its being assigned to a unique type.
 *
 * If a were referenced by another variable `b`, then `b` would be a branch, since `b` isn't assigned to anything known
 * at compiletime, unlike with constants and data structures.
 * ```
 *  let
 *      a = 5
 *  do
 *      let
 *          b = a
 *      do
 *          b
 * ```
 * Since b does not point to anything immediately typeable, we must look to its reference to `a`, hoping that `a` is
 * already typed. If `a` has not been typed, then we can assume that something went wrong with type inference. This would
 * include circular dependency, however, variables cannot be circularly dependent on one another because the symbol
 * resolution pass would not allow a to reference b when b hasn't been declared yet.
 *
 * Branches can have multiple children. These children could be multiple references, aka, binary expressions such as
 * ```
 *  let
 *      a = 5
 *      b = 3
 *      c = a + b
 *  do
 *      c
 * ```
 * C would have two children, `a`, and `b`. If `a` is 'int', and `b` is `int`, then `c` should also be `int`.
 * Currently there is no checking for binary expressions however it may be an exercise to add checks on binary expressions
 * for whether the left node and right node of a binary operation are the same type. It may also be an exercise to add
 * other data types and do comparisons of them to ensure that all the children are of the same type before inferring the
 * type of a branch.
 */
sealed class TypeMapNode(
        open val id: Int,
        open val symbol: Symbol,
        open val type: Type,
        open val nodeKind: TypeMapNodeKind
){
    data class TypeMapBranch(
            override val id: Int,
            override val symbol: Symbol,
            override val type: Type,
            override val nodeKind: TypeMapNodeKind,
            val children: ArrayList<TypeMapNode>
    ): TypeMapNode(id, symbol, type, nodeKind){
        override fun toString(): String =
                buildString {
                    appendLine(this@TypeMapBranch.javaClass.name)
                    appendLine("Id: $id")
                    appendLine()
                    appendLine("Symbol: \n$symbol")
                    appendLine("Type: {")
                    appendLine("\tId: $type")
                    appendLine("\tName: ${type.typeName}")
                    appendLine("}")
                    appendLine("Kind: $nodeKind")
                    appendLine()
                    appendLine("Children: [")
                    children.forEach { child ->
                        appendLine("\t")
                        appendLine(child.toString())
                    }
                }
    }
    data class TypeMapLeaf(
            override val id: Int,
            override val symbol: Symbol,
            override val type: Type,
            override val nodeKind: TypeMapNodeKind,
    ): TypeMapNode(id, symbol, type, nodeKind){
        override fun toString(): String =
                buildString {
                    appendLine(this@TypeMapLeaf.javaClass.name)
                    appendLine("Id: $id")
                    appendLine()
                    appendLine("Symbol: \n$symbol")
                    appendLine("Type: {")
                    appendLine("\tId: $type")
                    appendLine("\tName: ${type.typeName}")
                    appendLine("}")
                    appendLine("Kind: $nodeKind")
                    appendLine()
                }
    }

}

/**
 * A map of all the typed elements in the source code. This will use a Hindley-Milner inference algorithm to search
 * through the AST to construct a map of all the typed elements, and infer their typed, and ensure that elements
 * are being used correctly according to their typed.
 *
 * The only type in Hawk currently is `int`, so as an exercise, some may want to expand upon this and add other types.
 */
class TypeMap{
    private val nodes = arrayListOf<TypeMapNode>()
    private var nextId = 0

    /**
     * Add a new root element, which is a symbol whose type hasn't been established yet. This root is the start
     * of either a branch or a leaf. By default, it is set to a leaf, until it is transformed later by [TypeMapCreator].
     */
    fun addRoot(symbol: Symbol): TypeMapNode{
        val node = TypeMapNode.TypeMapLeaf(nextId, symbol, Type.getDyn(nextId), TypeMapNodeKind.Assignment)
        nextId += 1
        nodes.add(node)
        return node
    }

    /**
     * Add a child to a given [node][TypeMapNode]. This takes the name of the parent, and a node to be added as a child.
     *
     * If we create variables `a` and `b`, and `a` is set to `5` and `b` is set to a reference to `a`, then `a`
     * would be a root leaf node of type `int`, and `b` would be a branch whose child is a pointer to the node for `a`.
     * This is then used to infer the type of `b`.
     */
    fun addChild(parent: String, child: TypeMapNode): TypeMapNode? =
            findNode(parent)?.transform {
                TypeMapNode.TypeMapBranch(it.id, it.symbol, it.type, it.nodeKind, arrayListOf(child))
            }

    /**
     * Transform a [TypeMapNode] with the given [callback][block]
     */
    fun TypeMapNode.transform(block: (TypeMapNode) -> TypeMapNode): TypeMapNode =
            block(this)

    /**
     * Transform a node of a given id with the given [callback][block]
     */
    fun transformNode(id: Int, block: (TypeMapNode) -> TypeMapNode): Boolean{
        val (idx, node) = nodes.withIndex().find { (_, node) -> node.id == id } ?: return false
        val new = block(node)
        nodes[idx] = new
        return true
    }

    /**
     * Find a node with the given name. This searches for the last occurrence of a node whose symbol matches [name]
     */
    fun findNode(name: String): TypeMapNode? {
        val node = nodes.findLast { it.symbol.ident == name }
        if(node != null){
            return node
        }
        for(n in nodes){
            if(n is TypeMapNode.TypeMapBranch){
                val child = n.findChild(name)
                if(child != null){
                    return child
                }
            }
        }
        return null
    }

    override fun toString(): String =
            buildString {
                nodes.forEach {
                    append(it.toString())
                }
            }
}

/**
 * Finds a child of a node with the given name
 */
fun TypeMapNode.TypeMapBranch.findChild(name: String) =
        children.findLast {
            it.symbol.ident == name
        }

/**
 * Another [TypeMap.transform] ????
 */
fun TypeMapNode.transform(block: (TypeMapNode) -> TypeMapNode): TypeMapNode =
        block(this)

/**
 * Transforms the type of a node directly
 */
fun TypeMapNode.transformType(block: (Type) -> Type): TypeMapNode =
        transform {
            when(this){
                is TypeMapNode.TypeMapBranch ->
                    TypeMapNode.TypeMapBranch(id, symbol, block(type), nodeKind, children)
                is TypeMapNode.TypeMapLeaf ->
                    TypeMapNode.TypeMapLeaf(id, symbol, block(type), nodeKind)

            }
        }

/**
 * Transforms the kind of node. Currently not used. Not sure what I was thinking but if you can find use of it, go ahead!
 */
fun TypeMapNode.transformKind(block: (TypeMapNodeKind) -> TypeMapNodeKind): TypeMapNode =
        transform {
            when(this){
                is TypeMapNode.TypeMapBranch ->
                    TypeMapNode.TypeMapBranch(id, symbol, type, block(nodeKind), children)
                is TypeMapNode.TypeMapLeaf ->
                    TypeMapNode.TypeMapLeaf(id, symbol, type, block(nodeKind))

            }
        }

/**
 * Transforms the children. This is useful for corecion and casting, and may also be useful for doing static dispatching
 * of generic functions/data structures whose inner fields reference generic types, or even external uses of an instance
 * of a generic type whose use is a child of a branch.
 */
fun TypeMapNode.TypeMapBranch.transformChildren(block: (TypeMapNode) -> TypeMapNode): TypeMapNode.TypeMapBranch =
        transform{
            val children = children.map {
                it.transform(block)
            }
            TypeMapNode.TypeMapBranch(id, symbol, type, nodeKind, ArrayList(children))
        } as TypeMapNode.TypeMapBranch
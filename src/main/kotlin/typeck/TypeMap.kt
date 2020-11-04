package typeck

import symres.Symbol

data class Type(val id: Int, val typeName: String){
    companion object{
        fun getDyn(id: Int) = Type(id, "dyn")
    }
}

enum class TypeMapNodeKind{
    Assignment,
    Reassignment
}

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

class TypeMap{
    private val nodes = arrayListOf<TypeMapNode>()
    private var nextId = 0

    fun addRoot(symbol: Symbol): TypeMapNode{
        val node = TypeMapNode.TypeMapLeaf(nextId, symbol, Type.getDyn(nextId), TypeMapNodeKind.Assignment)
        nextId += 1
        nodes.add(node)
        return node
    }

    fun addChild(parent: String, child: TypeMapNode): TypeMapNode? =
            findNode(parent)?.transform {
                TypeMapNode.TypeMapBranch(it.id, it.symbol, it.type, it.nodeKind, arrayListOf(child))
            }

    fun TypeMapNode.transform(block: (TypeMapNode) -> TypeMapNode): TypeMapNode =
            block(this)

    fun transformNode(id: Int, block: (TypeMapNode) -> TypeMapNode): Boolean{
        val (idx, node) = nodes.withIndex().find { (_, node) -> node.id == id } ?: return false
        val new = block(node)
        nodes[idx] = new
        return true
    }

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

fun TypeMapNode.TypeMapBranch.findChild(name: String) =
        children.findLast {
            it.symbol.ident == name
        }

fun TypeMapNode.transform(block: (TypeMapNode) -> TypeMapNode): TypeMapNode =
        block(this)

fun TypeMapNode.transformType(block: (Type) -> Type): TypeMapNode =
        transform {
            when(this){
                is TypeMapNode.TypeMapBranch ->
                    TypeMapNode.TypeMapBranch(id, symbol, block(type), nodeKind, children)
                is TypeMapNode.TypeMapLeaf ->
                    TypeMapNode.TypeMapLeaf(id, symbol, block(type), nodeKind)

            }
        }

fun TypeMapNode.transformKind(block: (TypeMapNodeKind) -> TypeMapNodeKind): TypeMapNode =
        transform {
            when(this){
                is TypeMapNode.TypeMapBranch ->
                    TypeMapNode.TypeMapBranch(id, symbol, type, block(nodeKind), children)
                is TypeMapNode.TypeMapLeaf ->
                    TypeMapNode.TypeMapLeaf(id, symbol, type, block(nodeKind))

            }
        }

fun TypeMapNode.TypeMapBranch.transformChildren(block: (TypeMapNode) -> TypeMapNode): TypeMapNode.TypeMapBranch =
        transform{
            val children = children.map {
                it.transform(block)
            }
            TypeMapNode.TypeMapBranch(id, symbol, type, nodeKind, ArrayList(children))
        } as TypeMapNode.TypeMapBranch
package typeck

import ErrorHandling
import parser.*
import symres.SymbolTable

class TypeMapCreator: ASTVisitor2<SymbolTable, TypeMap, ASTNode<*>?> {
    override fun visitFile(errorHandler: ErrorHandling, astNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap) =
            visitLet(errorHandler, astNode, data1, data2)

    /**
     * Enter the next scope, get the variables and visit them. Visit the do block, and return the current ast node.
     * This does not do any ast transformations which is why it's okay to return the original ast node [letNode]
     */
    override fun visitLet(errorHandler: ErrorHandling, letNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        data1.enterScope()
        val children = letNode.children
        for(child in children.filter { it.astKind == ASTKind.Var }){
            visitVar(errorHandler, child, data1, data2)
        }
        val doBlock = children.find { it.astKind == ASTKind.Do }
        if(doBlock == null){
            errorHandler.pushError("Expected a do block after let statement but couldn't find it", letNode.startPos, letNode.endPos)
            return null
        }
        visitDo(errorHandler, doBlock, data1, data2)
        return letNode
    }

    /**
     * Visit the given expression and leave the current scope.
     */
    override fun visitDo(errorHandler: ErrorHandling, doNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        visitExpression(errorHandler, doNode.children[0], data1, data2)
        data1.leaveScope()
        return doNode
    }

    /**
     * Get the identifier of the current variable node. Get its symbol, and add it as a root in the type map.
     * Take its expression and visit it.
     */
    override fun visitVar(errorHandler: ErrorHandling, varNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        val ident = varNode.children[0]
        if(ident.astKind != ASTKind.Identifier){
            errorHandler.pushError("Expected an identifier as 0th child index of variable but instead found ${ident.astKind}", ident.startPos, ident.endPos)
            return null
        }
        val name = ident.data as String
        val varSymbol = data1.findSymbol(name)
        if(varSymbol == null){
            errorHandler.pushError("Expected to find symbol in symbol table with name $name", ident.startPos, ident.endPos)
            return null
        }
        data2.addRoot(varSymbol)
        val expr = varNode.children[1]
        visitExpression(errorHandler, expr, data1, data2)
        return varNode
    }

    /**
     * Resolves the types of parents for a given child node. This is useful for walking back up the tree to infer the
     * types of parent nodes. For example, if we have `a` be `int`, and `b` also be `int`, and `c` reference both `a` and `b`,
     * then we want to walk back up from the references in c, to the variable c's node and infer its type directly in
     * the type map. This is also useful for inferring the type of variables from within let-do blocks, where the
     * expression of a do block is typed, and can then be used to infer the type of the variable its assigned to.
     *
     * ```
     *  let
     *      a = 5
     *      b =
     *          let
     *              c = a
     *          do
     *              c
     *  do
     *      a + b
     * ```
     * `b` will be typed to whatever the expression of its last `do` block is, which is the type of `c`. `c` is typed
     * to whatever `a` is typed to, which is `int`.
     */
    private fun resolveTypeNodesForChild(errorHandler: ErrorHandling, astNode: ASTNode<*>, typeMap: TypeMap, block: (TypeMapNode) -> TypeMapNode): ASTNode<*>?{
        val parent = astNode.parent ?: return null
        return when(parent.astKind){
            ASTKind.Var -> {
                if(parent.children[0] != astNode) {
                    val ident = parent.children.getOrNull(0)
                    if (ident?.astKind == ASTKind.Identifier) {
                        val parentName = ident.data!! as String
                        val node = typeMap.findNode(parentName)!!
                        typeMap.transformNode(node.id, block)
                    }
                }
                astNode
            }
            ASTKind.BinaryPlus, ASTKind.BinaryMinus, ASTKind.BinaryMul, ASTKind.BinaryDiv -> {
                resolveTypeNodesForChild(errorHandler, parent, typeMap, block)
            }
            else -> resolveTypeNodesForChild(errorHandler, parent, typeMap, block)
        }
    }

    /**
     * Resolves the types of expressions and does inference on their parents.
     * @see [resolveTypeNodesForChild]
     */
    override fun visitExpression(errorHandler: ErrorHandling, exprNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        return when(exprNode.astKind){
            ASTKind.IntLiteral -> {
                resolveTypeNodesForChild(errorHandler, exprNode, data2){
                    it.transformType { ty ->
                        Type(ty.id, "int")
                    }
                }
            }
            ///Checks if the current var ref has been typed, and if not, yield an error, otherwise, use it to infer the
            ///type of this expression's parent, by transforming the parent with the type of the variable being reference.
            ASTKind.VarRef -> {
                resolveTypeNodesForChild(errorHandler, exprNode, data2){
                    val varRefName = exprNode.data as String
                    val refNode = data2.findNode(varRefName) ?: throw IllegalStateException("Expected to find var reference $varRefName in type map but didn't!")
                    if(refNode.type.typeName == "dyn"){
                        errorHandler.pushError(
                                "Could not infer type of var ref",
                                exprNode.startPos,
                                exprNode.endPos
                        )
                        errorHandler.pushError(
                                "because var being reference $varRefName has not been typed",
                                refNode.symbol.startPos,
                                refNode.symbol.endPos
                        )
                        return@resolveTypeNodesForChild it
                    }
                    val newParent = data2.addChild(it.symbol.ident, refNode)
                            ?: throw IllegalStateException("Attempted to transform parent type map node with name ${it.symbol.ident} but failed to do so!")
                    newParent.transformType { ty ->
                        Type(ty.id, refNode.type.typeName)
                    }
                }
            }
            ///Does type checking and inference on both the left and right nodes by recursing to [visitExpression]
            ASTKind.BinaryPlus, ASTKind.BinaryMinus, ASTKind.BinaryMul, ASTKind.BinaryDiv -> {
                val left = exprNode.children.getOrNull(0) ?: return null
                visitExpression(errorHandler, left, data1, data2)
                val right = exprNode.children.getOrNull(0) ?: return null
                visitExpression(errorHandler, right, data1, data2)
            }
            ///Calls [visitLet] to do type checking on variables and its associated do block, which then results
            ///in a type inference on the parent node of this let-do block.
            ASTKind.Let -> {
                visitLet(errorHandler, exprNode, data1, data2)
            }
            else -> null
        }
    }

    override fun visitBinary(errorHandler: ErrorHandling, binaryNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap) : ASTNode<*>?{
        TODO("Not yet implemented")
    }

    override fun visitBinaryPlus(errorHandler: ErrorHandling, plusNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitBinaryMinus(errorHandler: ErrorHandling, minusNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitBinaryMul(errorHandler: ErrorHandling, mulNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitBinaryDiv(errorHandler: ErrorHandling, divNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitInteger(errorHandler: ErrorHandling, divNode: ASTNode<*>, data1: SymbolTable, data2: TypeMap): ASTNode<*>? {
        TODO("Not yet implemented")
    }

}
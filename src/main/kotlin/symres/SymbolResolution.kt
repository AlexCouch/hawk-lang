package symres

import ErrorHandling
import parser.ASTKind
import parser.ASTNode
import parser.ASTVisitor
import java.util.Random

/**
 * An [ASTVisitor] implementation which goes through the AST and populating a symbol table.
 *
 * 1. Every let node will create and push a new scope.
 * 2. Every let has variables, and those variables will be declared in the current scope of the table
 * 3. Every variable reference [ASTKind.VarRef] will be used for checking that the identifier of the var ref has been
 * declared, and that an undeclared variable is not being used. It will only check within the current scope and parent scopes.
 * 4. Every do node will do analysis on the expression, and repeat for steps 1 - 3 if necessary. At the end of a do node,
 *  the current scope will be popped.
 */
class SymbolResolution: ASTVisitor<SymbolTable, ASTNode<*>?>{
    override fun visitFile(errorHandler: ErrorHandling, astNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? =
            visitLet(errorHandler, astNode, data)

    @Suppress("UNCHECKED_CAST")
    override fun visitLet(errorHandler: ErrorHandling, letNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        ///Create a scope, and populate scope with variable declarations
        // Create a new scope based on the current let ast. Every scope has a unique identifier, which is useful for
        ///ensuring that every scope is unique. This could also be used for debugging, and creating a table dump.
        ///Also, if functions exist in the language, the function name would be passed in for this scope instead of
        ///a randomally generated name.
        data.createScope("let_${Random().nextInt()}")
        val vars = letNode.children.filter { it.astKind == ASTKind.Var }
        val doBlock = letNode.children.find { it.astKind == ASTKind.Do }
        for(varNode in vars){
            visitVar(errorHandler, varNode, data) ?: return null
        }
        ///</end>Create a scope, and populate scope with variable declarations
        ///Check do block and visit it
        // Check if the doBlock child node is null, and if so, return null and produce an error telling the user
        ///That a do block is expected. Generally this would only happen if there is a bug in the parser.
        ///Visit the do block.
        if(doBlock == null){
            errorHandler.pushError("Expected do block after let", letNode.startPos, letNode.endPos)
            return null
        }
        visitDo(errorHandler, doBlock, data) ?: return null
        ///</end>Check do block and visit it
        return letNode
    }

    override fun visitDo(errorHandler: ErrorHandling, doNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        ///Analyze the expression, pop the scope, and return
        ///Get the child node, which should be an expression. If it fails to check for expressions, the user will be
        ///notified. After the expression has been analyzed, leave/pop the current scope and return
        val expr = doNode.children[0]
        visitExpression(errorHandler, expr, data) ?: return null
        data.leaveScope()
        ///</end>Analyze the expression, pop the scope, and return
        return doNode
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitVar(errorHandler: ErrorHandling, varNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        ///Check identifier and use it to define a new symbol
        ///Get the identifier, ensure it really is an identifier, and that its data is of String type, then use
        ///it to define a new symbol in the symbol table.
        val ident = varNode.children[0]
        if(ident.astKind != ASTKind.Identifier){
            errorHandler.pushError("Expected an identifier but instead found $ident", ident.startPos, ident.endPos)
            return null
        }
        if(ident.data !is String){
            errorHandler.pushError("Compiler Bug! Identifier data is not String type! ${ident.data}", ident.startPos, ident.endPos)
            return null
        }
        data.defineSymbol(ident as ASTNode<String>)
        ///</end>Check identifier and use it to define a new symbol

        ///Get the expression and visit it, returning null upon failure
        val expr = varNode.children[1]
        visitExpression(errorHandler, expr, data) ?: return null
        ///</end>Get the expression and visit it, returning null upon failure
        return varNode
    }

    override fun visitExpression(errorHandler: ErrorHandling, exprNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        ///Check what kind of expression this ast is.
        when(exprNode.astKind){
            ///If this is a [VarRef] node, then check if the table currently has this var identifier defined,
            // if not, push an error and return null
            ASTKind.VarRef -> {
                val symbol = data.findSymbol(exprNode.data as String)
                if(symbol == null){
                    errorHandler.pushError("Use of undeclared symbol: ${exprNode.data}", exprNode.startPos, exprNode.endPos)
                    return null
                }
                return exprNode
            }
            ///If its any of the binary operators, call the visitBinary function
            ASTKind.BinaryPlus, ASTKind.BinaryMinus, ASTKind.BinaryMul, ASTKind.BinaryDiv ->
                visitBinary(errorHandler, exprNode, data) ?: return null
            ///If its a let node, call visitLet
            ASTKind.Let -> visitLet(errorHandler, exprNode, data) ?: return null
        }
        ///</end>Check what kind of expression this ast is.
        return exprNode
    }

    override fun visitBinary(errorHandler: ErrorHandling, binaryNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        ///Get the left and right expressions and visit them, doing whatever is needed
        val left = binaryNode.children[0]
        visitExpression(errorHandler, left, data) ?: return null
        val right = binaryNode.children[1]
        visitExpression(errorHandler, right, data) ?: return null
        ///</end>Get the left and right expressions and visit them, doing whatever is needed
        return binaryNode
    }

    /*
        These methods ended up not being used and are being kept in here for demonstrative purposes that you can
        visit each kind of node individually as you see fit, but may be reduced so that each kind of binary node
        shares one single visitation method (as I've already implemented)
     */

    override fun visitBinaryPlus(errorHandler: ErrorHandling, plusNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitBinaryMinus(errorHandler: ErrorHandling, minusNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitBinaryMul(errorHandler: ErrorHandling, mulNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitBinaryDiv(errorHandler: ErrorHandling, divNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        TODO("Not yet implemented")
    }

    override fun visitInteger(errorHandler: ErrorHandling, divNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        TODO("Not yet implemented")
    }

}
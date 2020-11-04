package symres

import ErrorHandling
import parser.ASTKind
import parser.ASTNode
import parser.ASTVisitor
import java.util.Random

class SymbolResolution: ASTVisitor<SymbolTable, ASTNode<*>?>{
    override fun visitFile(errorHandler: ErrorHandling, astNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? =
            visitLet(errorHandler, astNode, data)

    @Suppress("UNCHECKED_CAST")
    override fun visitLet(errorHandler: ErrorHandling, letNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        data.createScope("let_${Random().nextInt()}")
        val vars = letNode.children.filter { it.astKind == ASTKind.Var }
        val doBlock = letNode.children.find { it.astKind == ASTKind.Do }
        for(varNode in vars){
            visitVar(errorHandler, varNode, data) ?: return null
        }
        if(doBlock == null){
            errorHandler.pushError("Expected do block after let", letNode.startPos, letNode.endPos)
            return null
        }
        visitDo(errorHandler, doBlock, data) ?: return null
        return letNode
    }

    override fun visitDo(errorHandler: ErrorHandling, doNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        val expr = doNode.children[0]
        visitExpression(errorHandler, expr, data) ?: return null
        data.leaveScope()
        return doNode
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitVar(errorHandler: ErrorHandling, varNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
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
        val expr = varNode.children[1]
        visitExpression(errorHandler, expr, data) ?: return null
        return varNode
    }

    override fun visitExpression(errorHandler: ErrorHandling, exprNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        when(exprNode.astKind){
            ASTKind.VarRef -> {
                val symbol = data.findSymbol(exprNode.data as String)
                if(symbol == null){
                    errorHandler.pushError("Use of undeclared symbol: ${exprNode.data}", exprNode.startPos, exprNode.endPos)
                    return null
                }
                return exprNode
            }
            ASTKind.BinaryPlus, ASTKind.BinaryMinus, ASTKind.BinaryMul, ASTKind.BinaryDiv -> visitBinary(errorHandler, exprNode, data) ?: return null
            ASTKind.Let -> visitLet(errorHandler, exprNode, data) ?: return null
        }
        return exprNode
    }

    override fun visitBinary(errorHandler: ErrorHandling, binaryNode: ASTNode<*>, data: SymbolTable): ASTNode<*>? {
        val left = binaryNode.children[0]
        visitExpression(errorHandler, left, data) ?: return null
        val right = binaryNode.children[1]
        visitExpression(errorHandler, right, data) ?: return null
        return binaryNode
    }

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
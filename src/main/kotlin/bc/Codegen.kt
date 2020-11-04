package bc

import ErrorHandling
import com.sun.org.apache.xerces.internal.util.SymbolTable
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import parser.ASTKind
import parser.ASTNode
import parser.ASTVisitor
import parser.ASTVisitorVoid
import java.util.*
import kotlin.math.exp

class CodegenPass: ASTVisitorVoid<BCTree>{
    override fun visitFile(errorHandler: ErrorHandling, astNode: ASTNode<*>, data: BCTree){
        visitLet(errorHandler, astNode, data)
    }

    /**
     * Enter and push a new block onto the stack.
     * Visit all the variables
     * Visit the do block
     */
    override fun visitLet(errorHandler: ErrorHandling, letNode: ASTNode<*>, data: BCTree){
        data.enterBlock()
        data.pushBlock()
        val vars = letNode.children.filter { it.astKind == ASTKind.Var }
        val doBlock = letNode.children.find { it.astKind == ASTKind.Do }
        for(vr in vars){
            visitVar(errorHandler, vr, data)
        }
        if(doBlock == null){
            errorHandler.pushError("Codegen Pass: Expected a do block after let block.", letNode.startPos, letNode.endPos)
            return
        }
        visitDo(errorHandler, doBlock, data)
    }

    /**
     * Visit the expression
     * Generate a save
     * Generate the popping of the current block
     * Generate the load
     * Leave the current block
     */
    override fun visitDo(errorHandler: ErrorHandling, doNode: ASTNode<*>, data: BCTree){
        visitExpression(errorHandler, doNode.children[0], data)
        data.writeSave()
        data.popBlock()
        data.writeLoad()
        data.leaveBlock()
    }

    /**
     * Get the identifier
     * Visit the assigned expression
     * Check if the top of the stack needs to be renamed
     * Otherwise, push a new variable onto the stack
     */
    override fun visitVar(errorHandler: ErrorHandling, varNode: ASTNode<*>, data: BCTree){
        val ident = varNode.children[0]
        val expr = varNode.children[1]
        visitExpression(errorHandler, expr, data)
        if(
                varNode.children[1].astKind == ASTKind.BinaryPlus ||
                varNode.children[1].astKind == ASTKind.BinaryMinus ||
                varNode.children[1].astKind == ASTKind.BinaryMul ||
                varNode.children[1].astKind == ASTKind.BinaryDiv
        ){
            data.nameTop(ident.data as String)
        }else{
            data.pushVariable(ident.data as String)
        }
    }

    /**
     * If it's a VarRef:
     *      Get the referenced variable's identifier, and generate a read
     * If it's an IntLiteral:
     *      Generate a push instruction
     * If it's a binary expression:
     *      Visit the left and right expressions
     *      Check if we need to push a new variable for the right or the left
     *      Solo integer constants are not represented by names but will still need to be represented on the stack
     *      Write a binary instruction
     * If it's a Let:
     *      Visit the let
     */
    override fun visitExpression(errorHandler: ErrorHandling, exprNode: ASTNode<*>, data: BCTree){
        when(exprNode.astKind){
            ASTKind.VarRef -> {
                val refIdent = exprNode.data as String
                data.refVariable(refIdent)
            }
            ASTKind.IntLiteral -> {
                val int = exprNode.data as Int
                data.writeInteger(int)
            }
            ASTKind.BinaryMul, ASTKind.BinaryDiv, ASTKind.BinaryMinus, ASTKind.BinaryPlus -> {
                val left = exprNode.children[0]
                val right = exprNode.children[1]
                visitExpression(errorHandler, right, data)
                if(right.astKind == ASTKind.IntLiteral){
                    data.pushVariable("${Random().nextInt()}")
                }
                visitExpression(errorHandler, left, data)
                if(left.astKind == ASTKind.IntLiteral){
                    data.pushVariable("${Random().nextInt()}")
                }
                data.writeBinaryExpr(exprNode.astKind)
            }
            ASTKind.Let -> visitLet(errorHandler, exprNode, data)
        }
    }

    override fun visitBinary(errorHandler: ErrorHandling, binaryNode: ASTNode<*>, data: BCTree) {
        TODO("Not yet implemented")
    }

    override fun visitBinaryPlus(errorHandler: ErrorHandling, plusNode: ASTNode<*>, data: BCTree) {
        TODO("Not yet implemented")
    }

    override fun visitBinaryMinus(errorHandler: ErrorHandling, minusNode: ASTNode<*>, data: BCTree) {
        TODO("Not yet implemented")
    }

    override fun visitBinaryMul(errorHandler: ErrorHandling, mulNode: ASTNode<*>, data: BCTree) {
        TODO("Not yet implemented")
    }

    override fun visitBinaryDiv(errorHandler: ErrorHandling, divNode: ASTNode<*>, data: BCTree) {
        TODO("Not yet implemented")
    }

    override fun visitInteger(errorHandler: ErrorHandling, divNode: ASTNode<*>, data: BCTree) {
        TODO("Not yet implemented")
    }

}
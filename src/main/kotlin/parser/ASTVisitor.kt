package parser

import ErrorHandling

/**
 * A generic AST visitor. This provides method for traversing and visiting certain [ASTNode]'s.
 * Since every kind of node is identified by [ASTKind], this could all be shrunk down to just one
 * visitor method, however, separating the individual pieces out helps with having a piece of mind, and a clear
 * indication of where each node is processed, making the code easier manage.
 *
 * However, be aware that when changing the syntax, this interface and all its implementations because obsolete.
 */
interface ASTVisitor<in Data, out Ret> {
    fun visitFile(errorHandler: ErrorHandling, astNode: ASTNode<*>, data: Data): Ret
    fun visitLet(errorHandler: ErrorHandling, letNode: ASTNode<*>, data: Data): Ret
    fun visitDo(errorHandler: ErrorHandling, doNode: ASTNode<*>, data: Data): Ret
    fun visitVar(errorHandler: ErrorHandling, varNode: ASTNode<*>, data: Data): Ret
    fun visitExpression(errorHandler: ErrorHandling, exprNode: ASTNode<*>, data: Data): Ret
    fun visitBinary(errorHandler: ErrorHandling, binaryNode: ASTNode<*>, data: Data): Ret
    fun visitBinaryPlus(errorHandler: ErrorHandling, plusNode: ASTNode<*>, data: Data): Ret
    fun visitBinaryMinus(errorHandler: ErrorHandling, minusNode: ASTNode<*>, data: Data): Ret
    fun visitBinaryMul(errorHandler: ErrorHandling, mulNode: ASTNode<*>, data: Data): Ret
    fun visitBinaryDiv(errorHandler: ErrorHandling, divNode: ASTNode<*>, data: Data): Ret
    fun visitInteger(errorHandler: ErrorHandling, divNode: ASTNode<*>, data: Data): Ret
}

/**
 * A version of the generic [ASTVisitor] whose visitor return type is Unit (aka void)
 */
interface ASTVisitorVoid<in Data>: ASTVisitor<Data, Unit>

/**
 * A generic ASTVisitor extension that provides two input data paramters, which is useful for transferring data of one
 * pass to another pass (see [typeck.TypeMapCreator].
 */
interface ASTVisitor2<in Data1, in Data2, out Ret> {
    fun visitFile(errorHandler: ErrorHandling, astNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitLet(errorHandler: ErrorHandling, letNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitDo(errorHandler: ErrorHandling, doNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitVar(errorHandler: ErrorHandling, varNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitExpression(errorHandler: ErrorHandling, exprNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitBinary(errorHandler: ErrorHandling, binaryNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitBinaryPlus(errorHandler: ErrorHandling, plusNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitBinaryMinus(errorHandler: ErrorHandling, minusNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitBinaryMul(errorHandler: ErrorHandling, mulNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitBinaryDiv(errorHandler: ErrorHandling, divNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
    fun visitInteger(errorHandler: ErrorHandling, divNode: ASTNode<*>, data1: Data1, data2: Data2): Ret
}

/**
 * The same as [ASTVisitorVoid] but with [ASTVisitor2].
 */
interface ASTVisitor2Void<in Data1, in Data2>: ASTVisitor2<Data1, Data2, Unit>
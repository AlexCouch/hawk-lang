package parser

import ErrorHandling

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

interface ASTVisitorVoid<in Data>: ASTVisitor<Data, Unit>

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

interface ASTVisitor2Void<in Data1, in Data2>: ASTVisitor2<Data1, Data2, Unit>
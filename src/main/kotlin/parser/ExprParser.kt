package parser

import ErrorHandling
import Token
import TokenStream

class ExprParser : ParseRule{
    private fun parseBinary(errorHandler: ErrorHandling, left: Token<*>, tokenStream: TokenStream): ASTNode<*>?{
        val next = tokenStream.next() ?: return null
        if(next.data is String){
            if(next.data == "do") return null
        }
        val leftNode = when(left.kind){
            TokenKind.Integer -> ASTNode(ASTKind.IntLiteral, children = arrayListOf(), data = left.data, startPos = left.startPos, endPos = left.endPos)
            TokenKind.Identifier -> {
                ASTNode(ASTKind.VarRef, children = arrayListOf(), data = left.data, startPos = left.startPos, endPos = left.endPos)
            }
            else -> {
                return null
            }
        }
        val right = ExprParser().parse(errorHandler, tokenStream, true) ?: return null
        val node = when(next.kind){
            TokenKind.Plus -> {
                ASTNode(ASTKind.BinaryPlus, children = arrayListOf(), data = null, startPos = right.startPos, endPos = right.endPos)
            }
            TokenKind.Hyphen -> {
                ASTNode(ASTKind.BinaryMinus, children = arrayListOf(), data = null, startPos = right.startPos, endPos = right.endPos)
            }
            TokenKind.Star -> {
                ASTNode(ASTKind.BinaryMul, children = arrayListOf(), data = null, startPos = right.startPos, endPos = right.endPos)
            }
            TokenKind.FSlash -> {
                ASTNode(ASTKind.BinaryDiv, children = arrayListOf(), data = null, startPos = right.startPos, endPos = right.endPos)
            }
            else -> return null
        }
        val left = leftNode.transformASTParent {
            node
        }.assignParents()
        val rightN = right.transformASTParent {
            node
        }.assignParents()
        return node.transformASTNode {
            ASTNode(it.astKind, it.parent, arrayListOf(left, rightN), it.data, it.startPos, it.endPos)
        }.assignParents()
    }

    override fun parse(errorHandler: ErrorHandling, tokenStream: TokenStream, canFail: Boolean): ASTNode<*>? {
        val peek = tokenStream.peek ?: return null
        return when(peek.kind){
            TokenKind.Integer -> {
                val next = tokenStream.next() ?: return null
                tokenStream.saveCheckpoint()
                val bin = parseBinary(errorHandler, next, tokenStream)
                if(bin != null){
                    tokenStream.popCheckpoint()
                    return bin
                }
                tokenStream.restoreCheckpoint()
                ASTNode(ASTKind.IntLiteral, children = arrayListOf(), data = peek.data, startPos = next.startPos, endPos = next.endPos)
            }
            TokenKind.Identifier -> {
                tokenStream.saveCheckpoint()
                val letParser = LetParser()
                val letAst = letParser.parse(errorHandler, tokenStream, true)
                if(letAst != null){
                    tokenStream.popCheckpoint()
                    return letAst
                }
                tokenStream.restoreCheckpoint()
                val next = tokenStream.next() ?: return null
                tokenStream.saveCheckpoint()
                val bin = parseBinary(errorHandler, next, tokenStream)
                if(bin != null){
                    tokenStream.popCheckpoint()
                    return bin
                }
                tokenStream.restoreCheckpoint()
                ASTNode(ASTKind.VarRef, children = arrayListOf(), data = next.data, startPos = next.startPos, endPos = next.endPos)
            }
            else -> {
                if(!canFail){
                    errorHandler.pushError("Failed to parse expression: unrecognized token ${peek.kind}", peek.startPos, peek.endPos)
                }
                null
            }
        }
    }

}
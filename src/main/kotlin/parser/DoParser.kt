package parser

import ErrorHandling
import TokenStream

class DoParser: ParseRule{
    override fun parse(errorHandler: ErrorHandling, tokenStream: TokenStream, canFail: Boolean): ASTNode<*>? {
        val next = tokenStream.next() ?: return null
        if(next.kind != TokenKind.Identifier) {
            if(!canFail){
                println("Expected an identifier @ ${next.startPos.line}:${next.startPos.col}-${next.endPos.line}:${next.endPos.col}")
            }
            return null
        }
        if(next.data !is String){
            if(!canFail){
                println("Tokenizer bug! Expected identifier to contain String data but instead found ${next.data} @ ${next.startPos.line}:${next.startPos.col}-${next.endPos.line}:${next.endPos.col}")
            }
            return null
        }
        val startPos = next.startPos
        val data = next.data as String
        if(data != "do"){
            if(!canFail){
                println("Expected 'do' while parsing for do block @ ${next.startPos.line}:${next.startPos.col}-${next.endPos.line}:${next.endPos.col}")
            }
            return null
        }
        val doAST = ASTNode(ASTKind.Do, children = arrayListOf(), data = null, startPos = startPos, endPos = startPos)
        val exprParser = ExprParser()
        val exprAst = exprParser.parse(errorHandler, tokenStream) ?: return null
        val expr = exprAst.transformASTParent {
            doAST
        }.assignParents()
        return doAST.transformASTNode {
            ASTNode(it.astKind, it.parent, arrayListOf(expr), it.data, it.startPos, expr.endPos)
        }.assignParents()
    }

}
package parser

import ErrorHandling
import TokenStream

class VarParser: ParseRule{
    override fun parse(errorHandler: ErrorHandling, tokenStream: TokenStream, canFail: Boolean): ASTNode<*>? {
        var next = tokenStream.next() ?: return null
        if(next.kind != TokenKind.Identifier){
            errorHandler.pushError("Expected an identifier denoting a variable name but instead found ${next.kind}", next.startPos, next.endPos)
            return null
        }
        if(next.data !is String){
            errorHandler.pushError("Tokenizer bug! Expected identifier to contain String data but instead found ${next.data}", next.startPos, next.endPos)
            return null
        }
        val varName = next.data as String
        val identAST = ASTNode(ASTKind.Identifier, children = arrayListOf(), data = varName, startPos = next.startPos, endPos = next.endPos)
        next = tokenStream.next() ?: return null
        if(next.kind != TokenKind.Equal){
            errorHandler.pushError("Expected an identifier denoting a variable assignment but instead found ${next.kind}", next.startPos, next.endPos)
            return null
        }
        val varAST = ASTNode(ASTKind.Var, children = arrayListOf(identAST), data = null, startPos = identAST.startPos, endPos = identAST.endPos)
        val exprParser = ExprParser()
        var exprAst = exprParser.parse(errorHandler, tokenStream) ?: return null
        exprAst = exprAst.transformASTParent {
            varAST
        }.assignParents()
        return varAST.transformASTNode {
            ASTNode(it.astKind, children = arrayListOf(identAST, exprAst), data = null, startPos = identAST.startPos, endPos = exprAst.endPos)
        }.assignParents()
    }

}
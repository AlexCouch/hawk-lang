package parser

import ErrorHandling
import TokenStream

class LetParser: ParseRule{
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
        if(data != "let"){
            if(!canFail){
                errorHandler.pushError("Expected 'let' while parsing for 'let bindings", startPos, next.endPos)
            }
            return null
        }
        val varParser = VarParser()
        val children = arrayListOf<ASTNode<*>>()
        do{
            val peek = tokenStream.peek ?: break
            if(peek.kind == TokenKind.Identifier){
                if((peek.data as String) == "do") break
            }
            val varNode = varParser.parse(errorHandler, tokenStream) ?: return null
            children += varNode
        }while(tokenStream.hasNext())
        val letNode = ASTNode(ASTKind.Let, children = children, data=null, startPos = startPos, endPos = startPos)
        val doParser = DoParser()
        var doAst = doParser.parse(errorHandler, tokenStream) ?: return null
        doAst = doAst.transformASTParent {
            letNode
        }.assignParents()
        children += doAst
        return letNode.transformASTNode {
            ASTNode(it.astKind, children = it.children, data = null, startPos = startPos, endPos = doAst.endPos)
        }.assignParents()
    }
}
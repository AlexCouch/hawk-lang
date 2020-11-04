package parser

import ErrorHandling
import TokenStream


interface ParseRule{
    fun parse(errorHandler: ErrorHandling, tokenStream: TokenStream, canFail: Boolean = false): ASTNode<*>?
}
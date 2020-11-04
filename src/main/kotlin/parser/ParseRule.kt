package parser

import ErrorHandling
import TokenStream

/**
 * This is an abstraction over all parse rules. Together, the connective flow between each parse rule implies a
 * parse tree. In general, the parse rules put together looks like this:
 *
 * Expression   : Integer
 *                  | VarRef
 *                  | Expression '+' Expression
 *                  | Expression '-' Expression
 *                  | Expression '*' Expression
 *                  | Expression '/' Expression
 *                  | Let
 * Integer      : [0-9]+
 * Identifier   : [a-zA-Z][a-zA-Z0-9_]*
 * VarRef       : Identifier
 * VAR          : Identifier '=' Expression
 * Let          : 'let' VAR* Do
 * Do           : 'do' Expression
 *
 * The parse tree constructed from this is an implied tree of the exact rules traversed to produced the
 * Abstract Syntax Tree. The parse tree is never analyzed and is also never produced, nor debugged. The parse tree is purely
 * an effect of traversing from one parse rule to another, with every succession producing a node in the AST.
 *
 * The parser is considered an LL parser, Left-to-Right Leftmost Derivation. This means that every non-terminal is
 * considered for parsing during a parse rule, in order to construct a parse tree that then constructs an AST. The
 * difference between this an LR is that with an LR, you try to reconstruct the input directly, and by using parse rules
 * to reconstruct the input string, you're able to determinate which parse rules to use. In our case, we are skipping
 * over the parse table in order to just parse each token directly. Tokenizing the source input allows us to break the
 * parse table construction step and skip ahead to the AST production by implying a parse tree.
 */
interface ParseRule{
    fun parse(errorHandler: ErrorHandling, tokenStream: TokenStream, canFail: Boolean = false): ASTNode<*>?
}
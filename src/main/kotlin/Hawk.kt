import bc.BCTree
import bc.CodegenPass
import interp.Interpreter
import kotlinx.io.core.*
import parser.ASTNode
import parser.LetParser
import symres.SymbolResolution
import symres.SymbolTable
import typeck.TypeMap
import typeck.TypeMapCreator
import java.io.File

/**
 * Read in the text of the file to be compiled
 * Pass it into the tokenizer and attempt to yield a [TokenStream]
 */
fun tokenize(input: String, path: String): TokenStream{

    val tokenizer = Tokenizer(path, input)
    return tokenizer.start()
}

/**
 * Take the tokenstream and pass it into the [LetParser]
 * This is the first step as the start of any hawk program is a let-do block, so we must parse for a 'let' block first
 * Following that is parsing for variables, which will parse for expressions. [parser.ExprParser] may parse for
 * a let-do block as well, since they can also be expressions. See [parser.ParseRule] for more info on how parsing
 * works in Hawk.
 */
fun parse(errorHandler: ErrorHandling, tokens: TokenStream): ASTNode<*>?{

    val letParser = LetParser()
    val ast = letParser.parse(errorHandler, tokens)
    if(ast == null || errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    return ast
}

/**
 * If nothing went wrong, we should then have an [AST][parser.ASTNode] or Abstract Syntax Tree. This will be traversed
 * to find all symbolic nodes. Symbolic nodes are nodes that contain an identifier as a child node. Variables and
 * variable references are currently the only symbolic nodes but this can be expanded upon to include functions,
 * data structures, modules, etc. This pass will produce a [SymbolTable] which contains all the symbols, their locations
 * in source code, and also keeps track of what scopes were found and what variables were defined in those scopes.
 * This is useful later on when doing type checking so that we don't have to do symbol resolution again to find a symbol
 * in the AST. Traversing the AST once again during type checking is expensive, so the symbol table makes it easier
 * and faster to load a symbol's information before doing type resolution and inference.
 */
fun resolveSymbols(errorHandler: ErrorHandling, astNode: ASTNode<*>): SymbolTable?{

    val symbolTable = SymbolTable()
    val symres = SymbolResolution()
    val ast = symres.visitFile(errorHandler, astNode, symbolTable)
    if(ast == null || errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    println("Successfully completed symbol resolution")
    return symbolTable
}

/**
 * This pass constructs a type map to perform a Hindley-Milner Type Inference algorithm. This does a depth first search
 * of all symbols, how they're used, how they're declared, and construct a map of all symbols starting with the
 * pretyped symbols. These symbols are essentially variables initialized to constants. In Hawk, that would be integer
 * constants. These variables become typed to int, and all references to them become inferred to the same type.
 *
 * ```
 *  let
 *      a = 5
 *  do
 *      let
 *          b = a
 *      do
 *          b
 * ```
 *
 * This will construct a type map of a of type int, due to its assignment being a compiletime known constant,
 * and that then allows us to infer the type of b since b is referencing a, and a is type int.
 * This means we have two nodes in the map:
 *  a : int
 *  b => a : int
 * This then means that when type checking b and all references to be, we must also check a. Type of a implies type of b
 * since b references a.
 *
 */
fun checkAndInferTypes(errorHandler: ErrorHandling, astNode: ASTNode<*>, symbolTable: SymbolTable): TypeMap?{
    val typemapCreator = TypeMapCreator()
    val typemap = TypeMap()
    val ast = typemapCreator.visitFile(errorHandler, astNode, symbolTable, typemap)
    if(errorHandler.hasError || ast == null){
        println(errorHandler.toString())
        return null
    }
    println("Successfully completed type checking!")
    return typemap
}

/**
 * This pass emulates the stack ahead of time. This is good so that the VM can be more light weight and just rely on
 * locations on the stack instead of using name objects. This is more realistic compared to native compilers, since
 * they have to emulate the stack/heap when generating a jump table and converting variable names to addresses.
 * This does not generate a jump table but instead keeps track of where variables are put on the stack and when, followed
 * by the replacement of names with locations on the stack. This makes the VM's job easier as it won't have to preload
 * and know about names at runtime, thus cutting down on memory and cpu usage.
 */
fun generateBytecode(errorHandler: ErrorHandling, astNode: ASTNode<*>): ByteReadPacket?{
    val bcTree = BCTree()
    val bcGen = CodegenPass()
    bcGen.visitFile(errorHandler, astNode, bcTree)
    if(errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    return bcTree.buildBytePacket()
}

/**
 * Attempts to compile with every pass until there is a byte packet returned.
 *
 * Each step is outlined in comments, but in general looks like this:
 *
 *  [Tokenizer]
 *
 *  [Parser][parser.ParseRule]
 *
 *  [Symbol Resolution][symres.SymbolResolution]
 *
 *  [Type Checking/Inference][typeck.TypeMapCreator]
 *
 *  [Stack Emulation][bc.BCTree] and [Code Generation][bc.CodegenPass]
 *
 * Technically the tokenizer and parser are together but they are used separately to guarantee a robust pipeline.
 * Separating the tokenizer and parser makes it easier to add preliminary phases such as
 *  - a preprocessor for imports/exports
 *  - A macro expansion and processing interpreter
 *  - A compiletime execution layer
 *
 * The error handling is very simple and if there's any hint that something went wrong, it will be reported immediately
 * and discontinue the operation before returning null bytes.
 *
 */
fun compile(file: File): ByteReadPacket?{
    val input = file.readText()
    val errorHandler = ErrorHandling(input, file.path)
    val tokens = tokenize(input, file.path)
    val ast = parse(errorHandler, tokens) ?: return null
    val symtab = resolveSymbols(errorHandler, ast) ?: return null
    checkAndInferTypes(errorHandler, ast, symtab) ?: return null
    val bytes = generateBytecode(errorHandler, ast) ?: return null

    /**
     * Finally, we write the compiled bytes to a file with a matching name to the input file but with extension `bc`.
     * Followed by returning the bytes to caller of this function.
     */
    val out = File("${file.nameWithoutExtension}.bc")
    out.writeBytes(bytes.copy().readBytes())
    return bytes
}

@ExperimentalStdlibApi
fun debug(args: Array<String>){
    val file = File(args[2])
    if(!file.exists()){
        println("File ${file.path} does not exist!")
        return
    }
    val input = file.readText()
    val tokens = tokenize(input, file.name)
    val errorHandler = ErrorHandling(input, file.path)
    val ast = parse(errorHandler, tokens) ?: return
    when(args[1]){
        "ast" -> {
            val astDebugger = ASTDebug()
            println(astDebugger.debugAST(ast))
            return
        }
        "symtab" -> {
            val symtab = resolveSymbols(errorHandler, ast) ?: return
            val astDebugger = SymTabDebug()
            println(astDebugger.debugSymtab(symtab))
            return
        }
        "tymap" -> {
            val symtab = resolveSymbols(errorHandler, ast) ?: return
            val tymap = checkAndInferTypes(errorHandler, ast, symtab) ?: return
            val tymapDebugger = TypemapDebug()
            println(tymapDebugger.debugTypemap(tymap))
            return
        }
    }
}

@ExperimentalStdlibApi
fun main(args: Array<String>){
    //Check if the cli args are empty. If so, yield an error and terminate the program
    if(args.isEmpty()){
        println("Expected an input file path but found nothing!")
        return
    }
    //Check if the 0th argument starts with '-', if so, then check what kind of option it is, otherwise, continue to compile
    val arg0 = args[0]
    if(arg0[0] == '-'){
        when(arg0.substring(1)){
            "debug" -> debug(args)
        }
    }
    //arg0 should be a file path, either relative or absolute
    //Check its extension, if it's 'hawk', then compile the source code and emit a bc file for reuse
    //If its 'bc', then just read in the bytes as a byte packet and give it to the interpreter
    val inputPath = arg0
    val file = File(inputPath)
    val bytes = when(file.extension){
        "hawk" -> compile(file) ?: return
        "bc" -> buildPacket { writeFully(file.readBytes()) }
        else -> {
            println("Unrecognized extension: ${file.extension}")
            return
        }
    }
    val interp = Interpreter(bytes)
    interp.start()
}
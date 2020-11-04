import bc.BCTree
import bc.CodegenPass
import interp.Interpreter
import kotlinx.io.core.*
import kotlinx.io.streams.inputStream
import parser.LetParser
import symres.SymbolResolution
import symres.SymbolTable
import typeck.TypeMap
import typeck.TypeMapCreator
import java.io.File

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
    /**
     * Read in the text of the file to be compiled
     * Pass it into the tokenizer and attempt to yield a [TokenStream]
     */
    val input = file.readText()
    val tokenizer = Tokenizer(file.path, input)
    val tokens = tokenizer.start()

    /**
     * Take the tokenstream and pass it into the [LetParser]
     * This is the first step as the start of any hawk program is a let-do block, so we must parse for a 'let' block first
     * Following that is parsing for variables, which will parse for expressions. [parser.ExprParser] may parse for
     * a let-do block as well, since they can also be expressions. See [parser.ParseRule] for more info on how parsing
     * works in Hawk.
     */
    val letParser = LetParser()
    val errorHandler = ErrorHandling(input, file.path)
    var ast = letParser.parse(errorHandler, tokens)
    if(ast == null || errorHandler.hasError){
        println(errorHandler.toString())
        return null
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
    val symbolTable = SymbolTable()
    val symres = SymbolResolution()
    ast = symres.visitFile(errorHandler, ast, symbolTable)
    if(ast == null || errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    println("Successfully completed symbol resolution")
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
    val typemapCreator = TypeMapCreator()
    val typemap = TypeMap()
    ast = typemapCreator.visitFile(errorHandler, ast, symbolTable, typemap)
    if(errorHandler.hasError || ast == null){
        println(errorHandler.toString())
        return null
    }
    println("Successfully completed type checking!")
    /**
     * This pass emulates the stack ahead of time. This is good so that the VM can be more light weight and just rely on
     * locations on the stack instead of using name objects. This is more realistic compared to native compilers, since
     * they have to emulate the stack/heap when generating a jump table and converting variable names to addresses.
     * This does not generate a jump table but instead keeps track of where variables are put on the stack and when, followed
     * by the replacement of names with locations on the stack. This makes the VM's job easier as it won't have to preload
     * and know about names at runtime, thus cutting down on memory and cpu usage.
     */
    val bcTree = BCTree()
    val bcGen = CodegenPass()
    bcGen.visitFile(errorHandler, ast, bcTree)
    if(errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    val bytes = bcTree.buildBytePacket()

    /**
     * Finally, we write the compiled bytes to a file with a matching name to the input file but with extension `bc`.
     * Followed by returning the bytes to caller of this function.
     */
    val out = File("${file.nameWithoutExtension}.bc")
    out.writeBytes(bytes.copy().readBytes())
    return bytes
}

fun main(args: Array<String>){
    //Check if the cli args are empty. If so, yield an error and terminate the program
    if(args.isEmpty()){
        println("Expected an input file path but found nothing!")
        return
    }
    //Get the 0th argument which should be a file path, either relative or absolute
    //Check its extension, if it's 'hawk', then compile the source code and emit a bc file for reuse
    //If its 'bc', then just read in the bytes as a byte packet and give it to the interpreter
    val inputPath = args[0]
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
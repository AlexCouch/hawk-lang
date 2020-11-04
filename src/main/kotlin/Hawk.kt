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

fun compile(file: File): ByteReadPacket?{
    val input = file.readText()
    val tokenizer = Tokenizer(file.path, input)
    val tokens = tokenizer.start()
    val letParser = LetParser()
    val errorHandler = ErrorHandling(input, file.path)
    var ast = letParser.parse(errorHandler, tokens)
    if(ast == null || errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    val symbolTable = SymbolTable()
    val symres = SymbolResolution()
    ast = symres.visitFile(errorHandler, ast, symbolTable)
    if(ast == null || errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    println("Successfully completed symbol resolution")
    val typemapCreator = TypeMapCreator()
    val typemap = TypeMap()
    ast = typemapCreator.visitFile(errorHandler, ast, symbolTable, typemap)
    if(errorHandler.hasError || ast == null){
        println(errorHandler.toString())
        return null
    }
    println("Successfully completed type checking!")
    val bcTree = BCTree()
    val bcGen = CodegenPass()
    bcGen.visitFile(errorHandler, ast, bcTree)
    if(errorHandler.hasError){
        println(errorHandler.toString())
        return null
    }
    val bytes = bcTree.buildBytePacket()
    val out = File("${file.nameWithoutExtension}.bc")
    out.writeBytes(bytes.copy().readBytes())
    return bytes
}

fun main(args: Array<String>){
    if(args.isEmpty()){
        println("Expected an input file path but found nothing!")
        return
    }
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
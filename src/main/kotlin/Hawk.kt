import bc.BCTree
import bc.CodegenPass
import interp.Interpreter
import kotlinx.io.core.readBytes
import kotlinx.io.streams.inputStream
import parser.LetParser
import symres.SymbolResolution
import symres.SymbolTable
import typeck.TypeMap
import typeck.TypeMapCreator
import java.io.File

fun main(){
    val file = File("test.hawk")
    val input = file.readText()
    val tokenizer = Tokenizer(file.path, input)
    val tokens = tokenizer.start()
    val letParser = LetParser()
    val errorHandler = ErrorHandling(input, file.path)
    var ast = letParser.parse(errorHandler, tokens)
    if(ast == null || errorHandler.hasError){
        println(errorHandler.toString())
        return
    }
    val symbolTable = SymbolTable()
    val symres = SymbolResolution()
    ast = symres.visitFile(errorHandler, ast, symbolTable) ?: return
    if(errorHandler.hasError){
        println(errorHandler.toString())
        return
    }
    println("Successfully completed symbol resolution")
    val typemapCreator = TypeMapCreator()
    val typemap = TypeMap()
    ast = typemapCreator.visitFile(errorHandler, ast, symbolTable, typemap)
    if(errorHandler.hasError || ast == null){
        println(errorHandler.toString())
        return
    }
    println("Successfully completed type checking!")
    val bcTree = BCTree()
    val bcGen = CodegenPass()
    bcGen.visitFile(errorHandler, ast, bcTree)
    if(errorHandler.hasError){
        println(errorHandler.toString())
        return
    }
    val bytes = bcTree.buildBytePacket()
    println(bytes.copy().readBytes().joinToString { "$it" })
    val interp = Interpreter(bytes)
    interp.start()
}
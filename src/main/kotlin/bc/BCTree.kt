package bc

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import parser.ASTKind
import parser.ASTNode
import java.util.*
import kotlin.collections.ArrayList

sealed class BCTreeNode{
    data class BCTreeBlock(val parent: BCTreeNode.BCTreeBlock?, val children: ArrayList<BCTreeNode>, val depth: Int): BCTreeNode()
    data class BCTreeStatement(val bytes: ByteReadPacket): BCTreeNode()
}

sealed class StackEntry{
    class StackFrame: StackEntry()
    data class StackEntryVar(val name: String): StackEntry()
    data class StackEntryVal(val int: Int): StackEntry()
}

typealias BCCallback = () -> ByteReadPacket

class BCTree{
    private val stack = LinkedList<StackEntry>()
    private val root = BCTreeNode.BCTreeBlock(null, arrayListOf(), 0)
    private var currentBlock: BCTreeNode.BCTreeBlock = root

    fun writeInteger(int: Int, push: Boolean = false){
        val bytes = buildPacket {
            writeByte(Bytecode.PUSH.ordinal.toByte())
            writeInt(int)
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
    }

    fun writeBinaryExpr(astKind: ASTKind){
        val bytes = buildPacket {
            when(astKind){
                ASTKind.BinaryPlus -> writeByte(Bytecode.ADD.ordinal.toByte())
                ASTKind.BinaryMinus -> writeByte(Bytecode.SUB.ordinal.toByte())
                ASTKind.BinaryMul -> writeByte(Bytecode.MUL.ordinal.toByte())
                ASTKind.BinaryDiv -> writeByte(Bytecode.DIV.ordinal.toByte())
            }
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
        stack.pop()
        stack.pop()
        pushVariable("${Random().nextInt()}")
    }

    fun enterBlock(){
        val block = BCTreeNode.BCTreeBlock(currentBlock, arrayListOf(), currentBlock.depth)
        currentBlock.children.add(block)
        currentBlock = block
    }

    fun leaveBlock(){
        currentBlock = currentBlock.parent ?: return
    }

    fun pushVariable(name: String){
        val entry = StackEntry.StackEntryVar(name)
        stack.push(entry)
    }

    fun popStack(){
        val bytes = buildPacket {
            writeByte(Bytecode.POP.ordinal.toByte())
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
        stack.pop()
    }

    fun writeSave(){
        val bytes = buildPacket {
            writeByte(Bytecode.SAVE.ordinal.toByte())
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
        stack.pop()
    }

    fun writeLoad(){
        val bytes = buildPacket {
            writeByte(Bytecode.LOAD.ordinal.toByte())
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
    }

    fun pushBlock(){
        stack.push(StackEntry.StackFrame())
        writeInteger(0xff)
    }

    fun popBlock(){
        val frameIdx = stack.indexOf(stack.first { it is StackEntry.StackFrame })
        for(i in 0 until frameIdx){
            popStack()
        }
    }

    fun nameTop(name: String){
        stack.remove()
        pushVariable(name)
    }

    fun refVariable(name: String){
        val (idx, _) =
                stack.withIndex().first {
                    it.value is StackEntry.StackEntryVar &&
                    (it.value as StackEntry.StackEntryVar).name == name
                }
        val bytes = buildPacket {
            writeByte(Bytecode.READ.ordinal.toByte())
            writeInt(idx)
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
        stack.push(stack[idx])

    }

    private fun BCTreeNode.walkTree(block: (BCTreeNode) -> Unit){
        block(this)
        if(this is BCTreeNode.BCTreeBlock){
            children.forEach{
                it.walkTree(block)
            }
        }
    }

    fun buildBytePacket() =
            buildPacket {
                root.walkTree {
                    if(it is BCTreeNode.BCTreeStatement){
                        writePacket(it.bytes)
                    }
                }
            }
}
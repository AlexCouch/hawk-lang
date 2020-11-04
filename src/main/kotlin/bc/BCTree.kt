package bc

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import parser.ASTKind
import parser.ASTNode
import java.util.*
import kotlin.collections.ArrayList

/**
 * A node of a bytecode tree. It can either be a block or a statement.
 */
sealed class BCTreeNode{
    /**
     * A block is a block of code such as a let-do block. This contains a chunk of bytecode to be strung together
     * with other blocks. This does that by keeping an [ArrayList] of other [BCTreeNode]'s, maintaining a [depth].
     */
    data class BCTreeBlock(
            /**
             * The parent block, which is used for leaving a block and returning back to the parent node.
             * This helps simulate the stack by tracking what code should exist and where.
             */
            val parent: BCTreeBlock?,
            /**
             * The child nodes which can either be statements or other blocks. Each child contains a [ByteReadPacket],
             * and is used to string together all the packets into one final bytecode packet.
             */
            val children: ArrayList<BCTreeNode>): BCTreeNode()

    /**
     * This represents a single instruction and its operands. This is correlated to a single step in the program.
     */
    data class BCTreeStatement(val bytes: ByteReadPacket): BCTreeNode()
}

/**
 * An entry in the stack. This helps emulate stack frames and variables pushed onto the stack.
 * The end goal here is to replace variable names with stack locations. This helps simplify the virtual machine
 * significantly and is generally a good practice to use as little memory as possible to have the most efficient vm
 * possible. The less memory your VM uses, the smaller it is, and, sometimes, the faster it performs (generally more
 * efficient with memory more than cpu).
 */
sealed class StackEntry{
    class StackFrame: StackEntry()
    data class StackEntryVar(val name: String): StackEntry()
}

/**
 * A tree of bytecode that is produced by emulating the runtime stack in the VM.
 * Without actually executing code, we emulate the stack by simulating variables on the stack.
 * Sometimes, when variables are consumed by an operation and replaced with a new temporary variable,
 * such as binary operations, this will result in a dummy variable with a randomly generated name.
 *
 * This stack can then be used for seeing where on the stack we would have to read from, in order to read from a certain
 * variable of a given name. If we read from variable 'a', where would it be on the stack by the time it is read?
 */
class BCTree{
    private val stack = LinkedList<StackEntry>()
    private val root = BCTreeNode.BCTreeBlock(null, arrayListOf())
    private var currentBlock: BCTreeNode.BCTreeBlock = root

    /**
     * Write an integer to the current block. This will generate a [Bytecode.PUSH] instruction.
     * This can be used in conjunction with [pushVariable]
     */
    fun writeInteger(int: Int){
        val bytes = buildPacket {
            writeByte(Bytecode.PUSH.ordinal.toByte())
            writeInt(int)
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
    }

    /**
     * Writes a binary operation instruction based on the given [ASTKind].
     * This will also pop twice off the stack. Reason being is that the VM will pop two items off the stack
     * when it crosses a binary operation instruction, and use those two popped items to get the result of the operation,
     * then push it on the stack. So a dummy variable is created and pushed on the stack.
     */
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

    /**
     * Create a new block based on the current block, and make it a child of the current block
     * Then set the current block to it
     */
    fun enterBlock(){
        val block = BCTreeNode.BCTreeBlock(currentBlock, arrayListOf())
        currentBlock.children.add(block)
        currentBlock = block
    }

    /**
     * Set the current block to the parent of the current block
     */
    fun leaveBlock(){
        currentBlock = currentBlock.parent ?: return
    }

    /**
     * Push a stack entry to the stack, representing a variable
     */
    fun pushVariable(name: String){
        val entry = StackEntry.StackEntryVar(name)
        stack.push(entry)
    }

    /**
     * Pop off the stack and generate a pop instruction
     */
    fun popStack(){
        val bytes = buildPacket {
            writeByte(Bytecode.POP.ordinal.toByte())
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
        stack.pop()
    }

    /**
     * Generate a [SAVE][Bytecode.SAVE] instruction and pop off the stack.
     */
    fun writeSave(){
        val bytes = buildPacket {
            writeByte(Bytecode.SAVE.ordinal.toByte())
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
        stack.pop()
    }

    /**
     * Write a [LOAD][Bytecode.LOAD] instruction and push a dummy variable
     */
    fun writeLoad(){
        val bytes = buildPacket {
            writeByte(Bytecode.LOAD.ordinal.toByte())
        }
        currentBlock.children.add(BCTreeNode.BCTreeStatement(bytes))
        pushVariable("${Random().nextInt()}")
    }

    /**
     * Push a new satck frame and generate a stack frame 0xff push instruction aka PUSH 0xff
     */
    fun pushBlock(){
        stack.push(StackEntry.StackFrame())
        writeInteger(0xff)
    }

    /**
     * Pop the current block by going through all variables upto the most recent stack frame and pop everything
     * up to the stack frame. This simulates the end of a stack frame
     */
    fun popBlock(){
        val frameIdx = stack.indexOf(stack.first { it is StackEntry.StackFrame })
        for(i in 0 until frameIdx){
            popStack()
        }
    }

    /**
     * Rename the top of the stack. This is useful in compound expressions where a variable's expression has multiple
     * child expressions and then final thing on the stack must be renamed as a simulation of variable assignment to
     * a compound expression.
     */
    fun nameTop(name: String){
        stack.remove()
        pushVariable(name)
    }

    /**
     * Reference a variable by first find the index on the stack this variable resides, if it does
     * Next, generate a [READ][Bytecode.READ] instruction with the given index.
     * Then, push a copy of the variable on the stack to the top of the stack.
     *
     * @see Bytecode.READ
     */
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

    /**
     * Walk through the bytecode tree and apply a callback
     */
    private fun BCTreeNode.walkTree(block: (BCTreeNode) -> Unit){
        block(this)
        if(this is BCTreeNode.BCTreeBlock){
            children.forEach{
                it.walkTree(block)
            }
        }
    }

    /**
     * Build the tree into a single [ByteReadPacket]
     */
    fun buildBytePacket() =
            buildPacket {
                root.walkTree {
                    if(it is BCTreeNode.BCTreeStatement){
                        writePacket(it.bytes)
                    }
                }
            }}
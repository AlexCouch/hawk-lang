package interp

import bc.Bytecode
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import java.util.*

/**
 * This is a very simple and watered down implementation of a virtual machine.
 * This virtual machine has a [save] register which is used for saving values on the top of the stack for any reason,
 * typically for popping the current stack frame before returning the previous frame with the saved value.
 * This will also execute through the instructions in [bytes] and manipulate the stack, and never uses stores for
 * any purpose. This is so that it is as simple and lightwieght as possible. Stores are often unnecessary for VM's if
 * you're able to change the names of variables to stack locations. Some languages are a "run asap" kind of language
 * (e.g.: python, ruby, javascript, etc) so waiting for the bc tree to be done analysing may not be in the best interest
 * of the user. This language, however, can be saved to disk before being ran again later with the same compiled binary.
 */
class Interpreter(private val bytes: ByteReadPacket){
    private val stack = LinkedList<Int>()
    private var save = 0

    private fun push(){
        val data = bytes.readInt()
        stack.add(data)
    }

    private fun pop(){
        stack.pop()
    }

    private fun read(){
        val idx = stack.lastIndex - bytes.readInt()
        val elem = stack[idx]
        if(elem == 0){
            return
        }
        stack.add(elem)
    }

    private fun save(){
        val elem = stack.removeLast()
        save = elem
    }

    private fun load(){
        stack.add(save)
    }

    private fun add(){
        val right = stack.removeLast()
        val left = stack.removeLast()
        stack.add(right + left)
    }

    private fun sub(){
        val right = stack.removeLast()
        val left = stack.removeLast()
        stack.add(left - right)
    }

    private fun mul(){
        val right = stack.removeLast()
        val left = stack.removeLast()
        stack.add(left * right)
    }

    private fun div(){
        val right = stack.removeLast()
        val left = stack.removeLast()
        stack.add(left / right)
    }

    fun start(){
        while(bytes.canRead()){
            when(bytes.readByte()){
                Bytecode.PUSH.ordinal.toByte() -> push()
                Bytecode.POP.ordinal.toByte() -> pop()
                Bytecode.READ.ordinal.toByte() -> read()
                Bytecode.SAVE.ordinal.toByte() -> save()
                Bytecode.LOAD.ordinal.toByte() -> load()
                Bytecode.ADD.ordinal.toByte() -> add()
                Bytecode.SUB.ordinal.toByte() -> sub()
                Bytecode.MUL.ordinal.toByte() -> mul()
                Bytecode.DIV.ordinal.toByte() -> div()
            }
        }
        println(stack.last)
    }
}
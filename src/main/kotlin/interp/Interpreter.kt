package interp

import bc.Bytecode
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import java.util.*

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
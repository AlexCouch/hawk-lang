package bc

enum class Bytecode {
    /**
     * Pushes a value onto the stack. This value can only be of an integer (but may be abstracted in the future).
     *
     * Example:
     *
     * PUSH 5   ;Pushes 5 onto the stack
     */
    PUSH,

    /**
     * Pops the top value off the stack.
     *
     * Example:
     *
     * POP
     */
    POP,

    /**
     * Add the two topmost values on the stack, and replace them with their sum
     */
    ADD,
    /**
     * Subtract the two topmost values on the stack, and replace them with their diff
     */
    SUB,
    /**
     * Multiply the two topmost values on the stack, and replace them with their product
     */
    MUL,
    /**
     * Divide the two topmost values on the stack, and replace them with their quotient
     */
    DIV,

    /**
     * Reads from a given location on the stack. This essentially brings a given address on the stack to focus by
     * pushing a copy of it onto the stack.
     *
     * Example:
     *
     *  READ    5   ;Read whatever is 5 from the top. If there is 7 values on the stack, then read whatever is at index 7 - 5 (index 2)
     */
    READ,

    /**
     * Saves the top value on the stack in a 'save' register, which can then be loaded using [LOAD]
     */
    SAVE,

    /**
     * Loads whatever is in the 'save' register back onto the stack. If there is nothing, nothing will be loaded.
     */
    LOAD
}
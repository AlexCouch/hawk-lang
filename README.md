# Introduction
Hawk is an experimental programming language compiler and virtual machine, made with the intent to provide a practice project for anyone hoping to get into programming language engineering and compiler/interpreter/vm development.

## Syntax
The syntax includes variable binding and do blocks. The final block in the code
is the block whose last expression computed on the stack to be printed out to the console.
```
let
    a = 5
    b = 3
    c = 8
do
    a + b * c
```
This code will compute b * c first followed by that + a, giving us 29.
```
let
    a = 5
    b =
        let
            c = 10
        do
            c + a
do
    b * 2
```
Computes b * 2, by frist computing b as c (10) + a (5) = 15, which then yeilds 30.

## Pipeline
The entire pipeline is as follows:

- Tokenizer
- Parser
    - This is an LL combinator like parser which implies a parse tree by immediate traversal of grammar structure. This will yields an abstract syntax tree if the grammar is correct.
- Symbol Resolution
    - This will resolve all symbols by emulating scopes and scope resolution and ensuring that all variable references exist at the time of the reference.
- Type Checking/Inference
    - This pass first constructs a type map of all the variables by inferring their types using a dept-first search algorithm of all variables.
    - This pass secondly infers variables by reference tracing. After all variables initialized to integer constants have been inferred, all variables referencing those variables will be inferred to the same type.
    - All variables that reference variables that reference variables will be typed to their inferred types as well.
    - This is called a Hindley-Milner inference system. This is a bottom-up, dept-first algorithm for inferring the deepest variables that are initialized to constants before inferring higher-level variables that reference other variables.
- Stack Emulation and Code Generation
    - This pass emulates the stack by emulating what will be on the stack when it reaches an instruction.
    - By having its own stack, we can generate code that is name-less and uses only positions on the stack.
    - This is useful for prebuilding an executable before passing it into the VM

## VM
The virtual machine is very simple. It contains a handful of instructions that manipulates the stack. There is no heap, only the stack.
The VM has a single register for saving values on the top of the stack. This is useful for at the end of a `do` block so that we don't accidentally pop off the value being set to its parent.

```
let
    a =
        let
            b = 5
        do
            b * 2
do
    a * 2
```
This will take the result of b * 2 and save it in the register using the `SAVE` instruction.
Then, `a` will be initialized to that saved value by using the `LOAD` instruction.
The final `do` block will take that initialized `a` variable (init'd to b * 2, which is 5 * 2 = 10)
...and multiply it by 2.
The final result of a * 2 will be saved after leaving the do block, and because there's no more code to execute,
...the VM will print that out to the console. So this code will print out 20.

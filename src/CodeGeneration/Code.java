package CodeGeneration;

import Parser.Parser;
import SymbolTable.SymbolTable;

import java.io.IOException;
import java.io.OutputStream;

public class Code { // Filled in skeleton file provided by Prof. Mössenböck
    public static final int  // instruction codes
            load        =  1,
            load0       =  2,
            load1       =  3,
            load2       =  4,
            load3       =  5,
            store       =  6,
            store0      =  7,
            store1      =  8,
            store2      =  9,
            store3      = 10,
            getstatic   = 11,
            putstatic   = 12,
            getfield    = 13,
            putfield    = 14,
            const0      = 15,
            const1      = 16,
            const2      = 17,
            const3      = 18,
            const4      = 19,
            const5      = 20,
            const_m1    = 21,
            const_      = 22,
            add         = 23,
            sub         = 24,
            mul         = 25,
            div         = 26,
            rem         = 27,
            neg         = 28,
            shl         = 29,
            shr         = 30,
            new_        = 31,
            newarray    = 32,
            aload       = 33,
            astore      = 34,
            baload      = 35,
            bastore     = 36,
            arraylength = 37,
            pop         = 38,
            jmp         = 39,
            jeq         = 40,
            jne         = 41,
            jlt         = 42,
            jle         = 43,
            jgt         = 44,
            jge         = 45,
            call        = 46,
            return_     = 47,
            enter       = 48,
            exit        = 49,
            read        = 50,
            print       = 51,
            bread       = 52,
            bprint      = 53,
            trap		= 54;

    public static final int  // compare operators
            eq = 0,
            ne = 1,
            lt = 2,
            le = 3,
            gt = 4,
            ge = 5;

    private static int[] inverse = {ne, eq, ge, gt, le, lt};
    private static final int bufSize = 8192;

    private static byte[] buf;	// code buffer
    public static int pc;		// next free byte in code buffer
    public static int mainPc;	// pc of main function (set by parser)
    public static int dataSize;	// length of static data in words (set by parser)

    //--------------- code buffer access ----------------------

    public static void put(int byt) {
        if (byt == call){
            int x;
        }
        if (pc >= bufSize) {
            if (pc == bufSize)
                Parser.error("program too large");
            pc++;
        } else
            buf[pc++] = (byte)byt;
    }

    public static void put2(int byt) {
        put(byt>>8); put(byt);
    }

    public static void put2(int pos, int byt) {
        int oldpc = pc; pc = pos; put2(byt); pc = oldpc;
    }

    public static void put4(int byt) {
        put2(byt>>16); put2(byt);
    }

    public static int get(int pos) {
        return buf[pos];
    }

    //----------------- instruction generation --------------

    // Load the operand x to the expression stack
    public static void load(Operand operand) {
        switch(operand.kind){ // const0..5 || const_m1 || const_ operand.val
            case Operand.Con:
                if (0 <= operand.val && operand.val <= 5){
                    put(const0 + operand.val);
                } else if (operand.val == -1){
                    put(const_m1);
                } else {
                    put(const_);
                    put4(operand.val);
                }
                break;
            case Operand.Static: // getstatic operand.address
                put(getstatic);
                put2(operand.adr);
                break;
            case Operand.Local: // load0..3 || load operand.address
                if (0 <= operand.adr && 3 >= operand.adr){
                    put(load0 + operand.adr);
                } else {
                    put(load);
                    put(operand.adr);
                }
                break;
            case Operand.Fld: // getfield operand.address
                put(getfield);
                put2(operand.adr);
                break;
            case Operand.Elem: // baload || aload
                if (operand.type == SymbolTable.charType){
                    put(baload); // Byte Array Load
                } else {
                    put(aload); // Array Load
                }
                break;
            case Operand.Stack: // Already loaded
                break;
            default:
                Parser.error("Cannot load this value");
        }
        operand.kind = Operand.Stack; // Object has been loaded
    }

    // Generate an assignment x = y
    public static void assign(Operand operand1/*x*/, Operand operand2/*y*/) {
        load(operand2);
        switch(operand1.kind){
            case Operand.Local:
                if (operand1.adr >= 0 && operand1.adr <= 3){
                    put(store0 + operand1.adr);
                } else {
                    put(store);
                    put(operand1.adr);
                }
                break;
            case Operand.Static:
                put(putstatic);
                put2(operand1.adr);
                break;
            case Operand.Fld:
                put(putfield);
                put2(operand1.adr);
                break;
            case Operand.Elem:
                if (operand1.type == SymbolTable.charType){ // Char
                    put(bastore);
                } else { // Int
                    put(astore);
                }
                break;
            default:
                Parser.error("Cannot assign values");
        }
    }

    //------------- jumps ---------------

    // Unconditional jump
    public static void putJump(int adr) {
        put(jmp);
        put2(adr);
    }

    // Conditional jump if op is false
    public static void putFalseJump(int op, int adr) {
        put(jeq + inverse[op]);
        put2(adr);
    }

    // patch jump target at adr so that it jumps to the current pc
    public static void fixup(int adr) {
        put2(adr, pc);
    }

    //------------------------------------

    // initialize code buffer
    static {
        buf = new byte[bufSize];
        pc = 0; mainPc = -1;
    }

    // Write the code buffer to the output stream
    public static void write(OutputStream s) {
        int codeSize;
        try {
            codeSize = pc;
            Decoder.decode(buf, 0, codeSize);
            put('M'); put('J');
            put4(codeSize);
            put4(dataSize);
            put4(mainPc);
            s.write(buf, codeSize, pc - codeSize);	// header
            s.write(buf, 0, codeSize);				// code
            s.close();
        } catch(IOException e) {
            Parser.error("cannot write code file");
        }
    }
}

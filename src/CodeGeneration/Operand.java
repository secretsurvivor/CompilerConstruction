package CodeGeneration;

import SymbolTable.*;
import Parser.*;

public class Operand { // Provided by Prof. Mössenböck
    public static final int // item kinds
            Con    = 0,
            Local  = 1,
            Static = 2,
            Stack  = 3,
            Fld    = 4,
            Elem   = 5,
            Meth   = 6;

    public int      kind;	// Con, Local, Static, Stack, Fld, Elem, Meth
    public Struct   type;	// item type
    public Obj      obj;    // Meth
    public int      val;    // Con: value
    public int      adr;    // Local, Static, Fld, Meth: address

    public Operand(Obj obj) {
        type = obj.type; val = obj.val; adr = obj.adr; kind = Stack; // default
        switch (obj.kind) {
            case Obj.Con:
                kind = Con; break;
            case Obj.Var:
                if (obj.level == 0)
                    kind = Static;
                else
                    kind = Local;
                break;
            case Obj.Meth:
                kind = Meth; this.obj = obj; break;
            case Obj.Type:
                Parser.error("type identifier not allowed here");
                break;
            default:
                Parser.error("wrong kind of identifier");
                break;
        }
    }

    public Operand(int val) {
        kind = Con;
        this.val = val;
        type = SymbolTable.intType;
    }

    public Operand(int kind, int val, Struct type) {
        this.kind = kind;
        this.val = val;
        this.type = type;
    }
}

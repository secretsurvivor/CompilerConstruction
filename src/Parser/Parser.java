package Parser;

import CodeGeneration.Code;
import CodeGeneration.Operand;
import Scanner.Scanner;
import Scanner.Token;
import SymbolTable.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;

public class Parser {
    private static boolean debug = true;
    private static final String[] name = { // token names for error messages; provided by Prof. Mössenböck.
            "none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
            "==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
            "[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
            "program", "read", "return", "void", "while", "eof"
    };

    private static Scanner scanner;

    static { // I did this so I didn't have to call an init function, it just did it automatically on initialisation
        try { 
            scanner = new Scanner(new FileReader(new File("H://Java//CompilerConstruction//ExtensiveTest.mj")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    } // Initialise Scanner

    // These were changed to improve readability
    private static Token lastToken; // Most recent recognised token
    private static Token currentToken; // Token to be recognised
    private static int symbol; // Type code of Token to be recognised

    private static void scan() throws IOException {
        lastToken = currentToken;
        currentToken = scanner.next();
        symbol = currentToken.kind;
        errorDist++;
    }

    private static int errorCounter = 0; // Number of errors
    private static int errorDist = 4; // Distance from last error

    public static void error(String msg){ // Mostly provided by Prof. Mössenböck, modified to print more information
        if (errorDist >= 3) {
            System.err.printf("line: %s, col: %s | %s\n", currentToken.line, currentToken.col, msg); // Changed to err PrintStream from out
            //if (debug) System.out.printf("Debug:: %s ~ %s | %s ~ %s ::\n", lastToken.string, lastToken.kind, currentToken.string, currentToken.kind);
            errorCounter++;
        }
        errorDist = 0;
    }
    public static void error(String msg, Object... args){ // Personally prefer using printf
        error(String.format(msg, args));
    }

    private static void check(int expected) throws IOException { // Provided by Prof. Mössenböck
        if (symbol == expected){
            scan();
        } else {
            error("%s is expected.", name[expected]);
        }
    }

    private static void Program() throws IOException {
        check(Scanner.program_);
        check(Scanner.ident);
        Obj progObj = SymbolTable.insert(Obj.Prog, lastToken.string, SymbolTable.nullType);
        SymbolTable.openScope();
        for(;;){
            if (symbol == Scanner.final_){
				ConstDecl();
			} else if (symbol == Scanner.ident) {
				VarDecl();
			} else if (symbol == Scanner.class_){
				ClassDecl();
			} else {
				break;
			}
        }
		check(Scanner.lbrace);
        for(;;) {
            if (symbol == Scanner.void_ || symbol == Scanner.ident) {
                MethodDecl();
            } else {
                break;
            }
        }
        check(Scanner.rbrace);
        progObj.locals = SymbolTable.curScope.locals;
        if (debug)SymbolTable.dumpScope(progObj);
        SymbolTable.closeScope();
    } // Program

    private static void ConstDecl() throws IOException {
        check(Scanner.final_);
        Struct type = Type();
        check(Scanner.ident);
        Obj constObj = SymbolTable.insert(Obj.Con, lastToken.string, type);
        check(Scanner.assign);
        if (symbol == Scanner.charCon){
            scan();
            if (constObj.type != SymbolTable.charType)
                error("%s must match the type char", lastToken.string);
        } else {
            check(Scanner.number);
            if (constObj.type != SymbolTable.intType)
                error("%s must match the type int");
        }
        constObj.val = lastToken.val;
        check(Scanner.semicolon);
        //Code.load();
    } // Constant Declaration

    private static int VarDecl() throws IOException {
        Struct type = Type();
        int count = 0;
        for(;;){
            check(Scanner.ident);
            SymbolTable.insert(Obj.Var, lastToken.string, type);
            count++;
            if (symbol == Scanner.comma){
                scan();
            } else {
                break;
            }
        }
        check(Scanner.semicolon);
        return count;
    } // Variable Declaration

    private static void ClassDecl() throws IOException {
        check(Scanner.class_);
        check(Scanner.ident);
        Obj classObj = SymbolTable.insert(Obj.Type, lastToken.string, SymbolTable.nullType);
	    Struct type = new Struct(Struct.Class);
        SymbolTable.openScope();
        check(Scanner.lbrace);
        for(;;){
            if (symbol == Scanner.ident){
	            type.nFields += VarDecl();;
            } else {
                break;
            }
        }
        check(Scanner.rbrace);
	    type.fields = SymbolTable.curScope.locals;
	    classObj.type = type;
	    classObj.nPars = type.nFields;
        if (debug)SymbolTable.dumpScope(classObj);
        SymbolTable.closeScope();
    } // Class Declaration

    static Obj curMethod;
    private static void MethodDecl() throws IOException {
        Struct type; String name; int n = 0;
        if (symbol == Scanner.ident){
            type = Type();
        } else {
            check(Scanner.void_);
            type = SymbolTable.noType;
        }
        check(Scanner.ident);
        name = lastToken.string;
        curMethod = SymbolTable.insert(Obj.Meth, name, type);
        SymbolTable.openScope();
        check(Scanner.lpar);
        if (symbol == Scanner.ident) { // Option Form Parameters
            n = FormPars();
        }
        check(Scanner.rpar);
        curMethod.nPars = n;
        if (name.equals("main")){
            Code.mainPc = Code.pc;
            if (curMethod.type != SymbolTable.noType)
                error("Method main must be void");
            if (curMethod.nPars != 0)
                error("Main must not have parameters");
        }
        for(;;){
            if (symbol == Scanner.ident){
                VarDecl();
            } else {
                break;
            }
        }
        curMethod.locals = SymbolTable.curScope.locals;
        curMethod.adr = Code.pc;
        Code.put(Code.enter);
        Code.put(curMethod.nPars);
        Code.put(SymbolTable.curScope.nVars);
        Block();
        if (curMethod.type == SymbolTable.noType){
            Code.put(Code.exit);
            Code.put(Code.return_);
        } else {
            Code.put(Code.trap);
            Code.put(1);
        }
        if (debug)SymbolTable.dumpScope(curMethod);
        SymbolTable.closeScope();
    } // Method Declaration

    private static int FormPars() throws IOException {
        Struct type = Type(); int n = 0;
        for(;;){
            check(Scanner.ident);
            SymbolTable.insert(Obj.Var, lastToken.string, type);
            n++;
            if (symbol == Scanner.comma){
                scan();
                type = Type();
            } else {
                break;
            }
        }
        return n;
    } // Parameters

    private static Struct Type() throws IOException {
        check(Scanner.ident);
        Struct type;
        if (lastToken.string.equals("int")){
            type = SymbolTable.intType;
        } else if (lastToken.string.equals("char")){
            type = SymbolTable.charType;
        } else {
            Obj possClass = SymbolTable.find(lastToken.string);
            if (possClass.kind == Obj.Type){
                type = possClass.type;
            } else {
                type = SymbolTable.noType;
            }
        }
        if (symbol == Scanner.lbrack){
            scan();
            check(Scanner.rbrack);
            type = new Struct(Struct.Arr, type);
        }
        return type;
    } // Type

    private static void Block() throws IOException {
        check(Scanner.lbrace);
        for(;;){
            if (firstStatement.get(symbol)){
                Statement();
            } else {
                break;
            }
        }
        check(Scanner.rbrace);
    } // Block

    private static BitSet firstStatement = new BitSet(); static {
        firstStatement.set(Scanner.ident);
        firstStatement.set(Scanner.if_);
        firstStatement.set(Scanner.while_);
        firstStatement.set(Scanner.return_);
        firstStatement.set(Scanner.read_);
        firstStatement.set(Scanner.print_);
        firstStatement.set(Scanner.lbrack);
        firstStatement.set(Scanner.semicolon);
    }
    private static BitSet syncStatement = new BitSet(); static {
        syncStatement.set(Scanner.if_);
        syncStatement.set(Scanner.while_);
        syncStatement.set(Scanner.return_);
        syncStatement.set(Scanner.read_);
        syncStatement.set(Scanner.print_);
        syncStatement.set(Scanner.lbrack);
        syncStatement.set(Scanner.semicolon);
    }
    private static void Statement() throws IOException {
        Operand x, y;
        switch(symbol){
            case Scanner.ident:
                x = Designator();
                if (symbol == Scanner.assign){ // Assignment
                    scan();
                    y = Expr();
                    if (y.type.assignableTo(x.type))
                        Code.assign(x, y);
                    else
                        error("Incompatible types in assignment");
                } else {
                    ActPars(x);
                    Code.put(Code.call);
                    Code.put2(x.adr);
                    if (x.type != SymbolTable.noType)
                        Code.put(Code.pop);
                }
                check(Scanner.semicolon);
                break;
            case Scanner.if_:
                scan();
                check(Scanner.lpar);
                int op = Condition();
                Code.putFalseJump(op, 0);
                int adr = Code.pc - 2;
                check(Scanner.rpar);
                Statement();
                if (symbol == Scanner.else_){
                    scan();
                    Code.putJump(0);
                    int adr2 = Code.pc - 2;
                    Code.fixup(adr);
                    Statement();
                    Code.fixup(adr2);
                } else {
                    Code.fixup(adr);
                }
                break;
            case Scanner.while_:
                scan();
                int top = Code.pc;
                check(Scanner.lpar);
                op = Condition();
                Code.putFalseJump(op, 0);
                adr = Code.pc - 1;
                check(Scanner.rpar);
                Statement();
                Code.putJump(top);
                Code.fixup(adr);
                break;
            case Scanner.return_:
                scan();
                if (firstExpr.get(symbol)){
                    x = Expr();
                    Code.load(x);
                    if (curMethod.type == SymbolTable.noType)
                        error("Void method must not return a value");
                    else if (!x.type.assignableTo(curMethod.type))
                        error("Type of return value must match method type");
                } else {
                    if (curMethod.type != SymbolTable.noType)
                        error("Return value expected");
                }
                check(Scanner.semicolon);
                Code.put(Code.exit);
                Code.put(Code.return_);
                break;
            case Scanner.read_:
                scan();
                check(Scanner.lpar);
                Designator();
                check(Scanner.rpar);
                check(Scanner.semicolon);
                break;
            case Scanner.print_:
                scan();
                check(Scanner.lpar);
                Expr();
                if (symbol == Scanner.comma){
                    scan();
                    Expr();
                }
                check(Scanner.rpar);
                check(Scanner.semicolon);
                break;
            case Scanner.lbrace:
                Block();
                break;
            case Scanner.semicolon:
                scan();
                break;
            default:
                error("Invalid Statement");
                while(!syncStatement.get(symbol))
                    scan();
                errorDist = 0;
            }
    } // Statement

    private static void ActPars(Operand m) throws IOException {
        Operand ap;
        check(Scanner.lpar);
        if (m.kind != Operand.Meth){
            error("Not a method");
            m.obj = SymbolTable.noObj;
        }
        int aPars = 0;
        int fPars = m.obj.nPars;
        Obj fp = m.obj.locals;
        if (firstExpr.get(symbol)){
            for(;;){
                ap = Expr();
                Code.load(ap);
                aPars++;
                if (fp != null){
                    if (!ap.type.assignableTo(fp.type))
                        error("Parameter type mismatch");
                    fp = fp.next;
                }
                if (symbol != Scanner.comma)
                    break;
            }
        }
        if (aPars > fPars)
            error("Too many actual parameters");
        else if (aPars < fPars)
            error("Too few actual parameters");
        check(Scanner.rpar);
    } // Action Parameters

    private static int Condition() throws IOException {
        int op; Operand x, y;
        x = Expr();
        Code.load(x);
        op = Relop();
        y = Expr();
        Code.load(y);
        if (!x.type.compatibleWith(y.type)) {
            System.out.printf("%s == %s\n", x.type.kind, y.type.kind);
            error("Type mismatch");
        }
        if (x.type.isRefType() && op != Code.eq && op != Code.ne)
            error("Invalid compare");
        return op;
    } // Condition

    private static int Relop() throws IOException {
        switch (symbol){
            case Scanner.eql:
                scan();
                break;
            case Scanner.neq:
                scan();
                return Code.ne;
            case Scanner.lss:
                scan();
                return Code.lt;
            case Scanner.leq:
                scan();
                return Code.le;
            case Scanner.gtr:
                scan();
                return Code.gt;
            case Scanner.geq:
                scan();
                return Code.ge;
            default:
                scan();
                error("Invalid Relop");
                break;
        }
        return Code.eq;
    } // Compare

    private static BitSet firstExpr = new BitSet(); static {
        firstExpr.set(Scanner.ident);
        firstExpr.set(Scanner.number);
        firstExpr.set(Scanner.charCon);
        firstExpr.set(Scanner.new_);
        firstExpr.set(Scanner.lpar);
        firstExpr.set(Scanner.minus);
    }
    private static Operand Expr() throws IOException {
        Operand first, second; int op;
        if (symbol == Scanner.minus){
            scan();
            first = Term();
            if (first.type != SymbolTable.intType)
                error("operand must be of type int");
            if (first.kind == Operand.Con)
                first.val = -first.val;
            else {
                Code.load(first);
                Code.put(Code.neg);
            }
        } else {
            first = Term();
        }
        for(;;) {
            if (symbol == Scanner.plus || symbol == Scanner.minus) {
                op = Addop();
                Code.load(first);
                second = Term();
                Code.load(second);
                if (first.type != SymbolTable.intType && second.type != SymbolTable.intType)
                    error("operands must be of type int");
                Code.put(op);
            } else {
                break;
            }
        }
        return first;
    } // Expression

    private static Operand Term() throws IOException {
        Operand first, second; int op;
        first = Factor();
        for(;;){
            if (symbol == Scanner.times || symbol == Scanner.slash || symbol == Scanner.rem){
                op = Mulop();
                Code.load(first);
                second = Factor();
                Code.load(second);
                if (first.type != SymbolTable.intType && second.type != SymbolTable.intType)
                    error("operands must be of type int");
                Code.put(op);
            } else {
                break;
            }
        }
        return first;
    } // Term

    private static Operand Factor() throws IOException {
        Operand x, m; int value; String name;
        switch(symbol) {
            case Scanner.ident:
                x = Designator();
                if (symbol == Scanner.lpar){
                    ActPars(x);
                    if (x.type == SymbolTable.noType)
                        error("Procedure called as a function");
                    if (x.obj == SymbolTable.ordObj || x.obj == SymbolTable.chrObj);
                    else if (x.obj == SymbolTable.lenObj)
                        Code.put(Code.arraylength);
                    else {
                        Code.put(Code.call);
                        Code.put(x.adr);
                    }
                    x.kind = Operand.Stack;
                }
                break;
            case Scanner.number:
                scan();
                x = new Operand(lastToken.val);
                x.type = SymbolTable.intType;
                break;
            case Scanner.charCon:
                scan();
                x = new Operand(lastToken.val);
                x.type = SymbolTable.charType;
                break;
            case Scanner.new_:
                scan();
                check(Scanner.ident);
                Obj obj = SymbolTable.find(lastToken.string);
                Struct type = obj.type;
                if (symbol == Scanner.lbrack) { // Array
                    scan();
                    if (obj.kind != Obj.Type)
                        error("type expected");
                    x = Expr();
                    if (x.type != SymbolTable.intType)
                        error("array size must be of type int");
                    Code.load(x);
                    Code.put(Code.newarray);
                    if (type == SymbolTable.charType)
                        Code.put(0);
                    else
                        Code.put(1);
                    type = new Struct(Struct.Arr, type);
                    check(Scanner.rbrack);
                } else { //
                    if (obj.kind != Obj.Type || type.kind != Struct.Class)
                        error("class type expected");
                    Code.put(Code.new_);
                    Code.put2(type.nFields);
                }
                x = new Operand(Operand.Stack);
                x.type = type;
                break;
            case Scanner.lpar:
                scan();
                x = Expr();
                check(Scanner.rpar);
                break;
            default:
                error("Invalid Factor");
                x = new Operand(Operand.Stack);
        }
        return x;
    } // Factor

    private static Operand Designator() throws IOException {
        String name, fname;    Operand x, y;

        check(Scanner.ident);
        name = lastToken.string;
        Obj obj = SymbolTable.find(name);
        x = new Operand(obj);

        for(;;){
            if (symbol == Scanner.period){
                scan();
                check(Scanner.ident);
                fname = lastToken.string;
                if (x.type.kind == Struct.Class){
                    Code.load(x);
                    Obj field = SymbolTable.findField(fname, x.type);
                    x.kind = Operand.Fld;
                    x.adr = field.adr;
                    x.type = field.type;
                } else {
                    error("%s is not an Object.", name);
                }
            } else if (symbol == Scanner.lbrack){
                scan();
                Code.load(x);
                y = Expr();
                if (x.type.kind == Struct.Arr) {
                    if (y.type.kind != Struct.Int)
                        error("index must be of type Int");
                    Code.load(y);
                    x.kind = Operand.Elem;
                    x.type = x.type.elemType;
                } else {
                    error("%s is not an array", name);
                }
                check(Scanner.rbrack);
            } else {
                return x;
            }
        }
    } // Designator

    private static int Addop() throws IOException {
        switch(symbol){
            case Scanner.plus:
                scan();
                break;
            case Scanner.minus:
                scan();
                return Code.sub;
            default:
                error("Invalid Operator");
        }
        return Code.add;
    } // Add Operator

    private static int Mulop() throws IOException {
        switch(symbol){
            case Scanner.times:
                scan();
                break;
            case Scanner.slash:
                scan();
                return Code.div;
            case Scanner.rem:
                scan();
                return Code.rem;
            default:
                error("Invalid Operator");
        }
        return Code.mul;
    } // Multiply Operator

    public static void Parse() throws IOException { // Parse start, branches into different conditions
        scan();
        Program();
        if (symbol != Scanner.eof) error("end of file found before end of program");
    }
}

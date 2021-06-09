package SymbolTable;

import Parser.Parser;

public class SymbolTable {
    public static Scope curScope;	// current scope
    public static int   curLevel;	// nesting level of current scope

    public static Struct intType;	// predefined types
    public static Struct charType;
    public static Struct nullType;
    public static Struct noType;
    public static Obj chrObj;		// predefined objects
    public static Obj ordObj;
    public static Obj lenObj;
    public static Obj noObj;

    static { // I did this so I didn't have to call an init function, it just did it automatically on initialisation
        curScope = new Scope();
        curScope.outer = null;
        curLevel = -1;

        // create predeclared types
        intType = new Struct(Struct.Int);
        charType = new Struct(Struct.Char);
        nullType = new Struct(Struct.Class);
        noType = new Struct(Struct.None);
        noObj = new Obj(Obj.Var, "???", noType);

        // create predeclared objects
        insert(Obj.Type, "int", intType);
        insert(Obj.Type, "char", charType);
        insert(Obj.Con, "null", nullType);
        chrObj = insert(Obj.Meth, "chr", charType);
        chrObj.locals = new Obj(Obj.Var, "i", intType);
        chrObj.nPars = 1;
        ordObj = insert(Obj.Meth, "ord", intType);
        ordObj.locals = new Obj(Obj.Var, "ch", charType);
        ordObj.nPars = 1;
        lenObj = insert(Obj.Meth, "len", intType);
        lenObj.locals = new Obj(Obj.Var, "a", new Struct(Struct.Arr, noType));
        lenObj.nPars = 1;
    } // Initialises Symbol Table

    private static void error(String msg) {
        Parser.error(msg);
    }
    private static void error(String msg, Object... args){
        Parser.error(msg, args);
    }


    public static void openScope() {
        Scope scope = new Scope();
        scope.outer = curScope;
        curScope = scope;
        curLevel++;
    }

    public static void closeScope() {
        curScope = curScope.outer;
        curLevel--;
    }


    // Create a new object with the given kind, name and type
    // and insert it into the top scope.
    public static Obj insert(int kind, String name, Struct type) {
        Obj obj = new Obj(kind, name, type);
        if (kind == Obj.Var){
            obj.adr = curScope.nVars;
            curScope.nVars++;
            obj.level = curLevel;
        }

        Obj lastObj = null; // Objects in local scope, last object in linked list
        for(Obj localObj = curScope.locals; localObj != null; localObj = localObj.next){
            if (localObj.name.equals(name)){
                error("%s declared twice", name);
            }
            lastObj = localObj;
        }
        if (lastObj == null){
            curScope.locals = obj;
        } else {
            lastObj.next = obj;
        }

        return obj;
    }

    // Retrieve the object with the given name from the top scope
    public static Obj find(String name) {
        for (Scope scope = curScope; scope != null; scope = scope.outer)
            for (Obj obj = scope.locals; obj != null; obj = obj.next)
                if (obj.name.equals(name))
                    return obj;
        error("%s is undeclared", name);
        return noObj;
    }

    // Retrieve a class field with the given name from the fields of "type"
    public static Obj findField(String name, Struct type) {
        for (Obj field = type.fields; field != null; field = field.next)
            if (field.name.equals(name))
                return field;
        error("%s is an undeclared type", name);
        return noObj;
    }


    public static void dumpStruct(Struct type) {
        String kind;
        switch (type.kind) {
            case Struct.Int:  kind = "Int  "; break;
            case Struct.Char: kind = "Char "; break;
            case Struct.Arr:  kind = "Arr  "; break;
            case Struct.Class:kind = "Class"; break;
            default: kind = "None";
        }
        System.out.printf("%s ", kind);
        if (type.kind == Struct.Arr) {
            System.out.printf("%s (", type.nFields);
            dumpStruct(type.elemType);
            System.out.print(")");
        }
        if (type.kind == Struct.Class) {
            System.out.printf("%s<<\n", type.nFields);
            for (Obj o = type.fields; o != null; o = o.next)
                dumpObj(o);
            System.out.print(">>");
        }
    }

    /*
    * Modified dump functions to improve readability heavily and increase how much information you see
    * */
    public static void dumpObj(Obj obj) {
        String kind;
        switch (obj.kind) {
            case Obj.Con:  kind = "Constant"; break;
            case Obj.Var:  kind = "Variable"; break;
            case Obj.Type: kind = "Class"; break;
            case Obj.Meth: kind = "Method"; break;
            default: kind = "None";
        }
        System.out.printf("%s - %s :: %s %s %s %s\n", kind, obj.name, obj.val, obj.adr, obj.level, obj.nPars);
        //dumpStruct(obj.type);
        //System.out.println(")");
    }

    public static void dumpScope(Obj head) {
        System.out.print("--------------\n\t");
        dumpObj(head);
        for (Obj obj = head.locals; obj != null; obj = obj.next)
            dumpObj(obj);
    }
}

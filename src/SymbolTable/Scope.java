package SymbolTable;

public class Scope { // Provided by Prof. Mössenböck
    public Scope outer;		// to outer scope
    public Obj   locals;	// to local variables of this scope
    public int   nVars;     // number of variables in this scope
}

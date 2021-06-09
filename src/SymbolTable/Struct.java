package SymbolTable;

public class Struct { // Class holding type information, entirely provided by Prof. Mössenböck
    public static final int // Structure kinds
            None  = 0,
            Int   = 1,
            Char  = 2,
            Arr   = 3,
            Class = 4;
    public int    kind;		  // None, Int, Char, Arr, Class
    public Struct elemType; // Arr: element type
    public int    nFields;  // Class: number of fields
    public Obj    fields;   // Class: fields

    public Struct(int kind) {
        this.kind = kind;
    }

    public Struct(int kind, Struct elemType) {
        this.kind = kind; this.elemType = elemType;
    }

    // Checks if this is a reference type
    public boolean isRefType() {
        return kind == Class || kind == Arr;
    }

    // Checks if two types are equal
    public boolean equals(Struct other) {
        if (kind == Arr)
            return other.kind == Arr && other.elemType == elemType;
        else
            return (other == this);
    }

    // Checks if two types are compatible (e.g. in a comparison)
    public boolean compatibleWith(Struct other) {
        return this.equals(other)
                ||	this == SymbolTable.nullType && other.isRefType()
                ||	other == SymbolTable.nullType && this.isRefType();
    }

    // Checks if an object with type "this" can be assigned to an object with type "dest"
    public boolean assignableTo(Struct dest) {
        return this.equals(dest)
                ||	this == SymbolTable.nullType && dest.isRefType()
                ||  this.kind == Arr && dest.kind == Arr && dest.elemType == SymbolTable.noType;
    }
}

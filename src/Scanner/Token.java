package Scanner;

public class Token { // Provided by Prof. Mössenböck
    public int kind;		// token kind
    public int line = -1;		// token line
    public int col  = -1;		// token column
    public int val  = -1;		// token value (for number and charConst)
    public String string = "";	// token string

    public Token(){} // Added constructors
    public Token(int line, int col){
        this.line = line;
        this.col = col;
    }
}

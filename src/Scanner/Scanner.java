package Scanner;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

public class Scanner {
    public static final int  // token codes; These were taken from the Source File provided by Prof. Mössenböck with
                             // added comments to organise and explain each other with examples.

            // Token Classes //
            ident     = 1, // Identifier
            number    = 2,
            charCon   = 3, // Character Constants

            // Operators and Special Characters //
            plus      = 4, // +
            minus     = 5, // -
            times     = 6, // *
            slash     = 7, // /
            rem       = 8, // Remainder %
            eql       = 9, // Equal ==
            neq       = 10, // Not Equal !=
            lss       = 11, // Less than <
            leq       = 12, // Less and Equal than <=
            gtr       = 13, // Greater than >
            geq       = 14, // Greater and Equal than >=
            assign    = 15, // ==
            semicolon = 16, // ;
            comma     = 17, // ,
            period    = 18, // .
            lpar      = 19, // Left Parenthesis (
            rpar      = 20, // Right Parenthesis )
            lbrack    = 21, // Left Bracket [
            rbrack    = 22, // Right Bracket ]
            lbrace    = 23, // Left Brace {
            rbrace    = 24, // Right Brace }

            // Keywords //
            class_    = 25,
            else_     = 26,
            final_    = 27,
            if_       = 28,
            new_      = 29,
            print_    = 30,
            program_  = 31,
            read_     = 32,
            return_   = 33,
            void_     = 34,
            while_    = 35,

            // Misc //
            none      = 0,
            eof       = 36,
            white     = 37; // White Space

    /*
    * These 'is' functions were made myself so it would be easier to just see what
    * the symbols are being checked against.
    * */
    static boolean isLetter(int cha){
        return cha > 0x40 && cha < 0x5B || cha > 0x60 && cha < 0x7B || cha == 0x5F;
    }
    static boolean isSymbol(int cha){
        switch(cha){
            case 0x2B: // Plus
            case 0x2D: // Minus
            case 0x2A: // Times
            case 0x2F: // Slash
            case 0x25: // Remainder
            case 0x21: // Not Equal
            case 0x3D: // Equals and Assign
            case 0x3C: // Less than and Less and Equal than
            case 0x3E: // Greater than and Greater and Equal than
            case 0x3B: // Semicolon
            case 0x2C: // Comma
            case 0x2E: // Period
            case 0x28: // Left Par
            case 0x29: // Right Par
            case 0x5B: // Left Bracket
            case 0x5D: // Right Bracket
            case 0x7B: // Left Brace
            case 0x7D: // Right Brace
            case 0x27: // Single speech mark
                return true;
        }
        return false;
    }
    static boolean isNumber(int cha){
        return cha >= 0x30 && cha <= 0x39;
    }
    static boolean isWhiteSpace(int cha){
        return cha == 0x20 || cha == 0x0D || cha == 0x09 || cha == 0x0A;
    }

    /*
    * I decided to make the class not static as it works better with the Reader
    * */
    private Reader r;
    public Scanner(Reader r) throws IOException {
        this.r = r;
        cha = r.read();
    }

    private void nextChar() throws IOException { // I decided to make the name of the function more obvious
                                                 // to improve readability
        cha = r.read();
        if (cha == '\n'){
            line += 1;
            col = 0;
        } else {
            col += 1;
        }
    }

    final HashMap<String, Integer> keywords = new HashMap(); {
        keywords.put("class", class_);
        keywords.put("else", else_);
        keywords.put("final", final_);
        keywords.put("if", if_);
        keywords.put("new", new_);
        keywords.put("print", print_);
        keywords.put("program", program_);
        keywords.put("read", read_);
        keywords.put("return", return_);
        keywords.put("void", void_);
        keywords.put("while", while_);
    } // I decided to do this so I didn't have to check the token against such a large if statement to improve readability

    /*
    http://ssw.jku.at/Misc/CC/
    http://ssw.jku.at/Misc/CC/slides/01.Overview.pdf
    http://ssw.jku.at/Misc/CC/slides/02.Scanning.pdf
	http://ssw.jku.at/Misc/CC/slides/03.Parsing.pdf
    http://ssw.jku.at/Misc/CC/Handouts.pdf
    * */

    private Token token;
    private boolean fin = false;
    private int line = 1, col = 1, cha;
    public Token next() throws IOException {
        if (Scanner.isLetter(cha)){
            token = new Token(line, col);
            while(Scanner.isLetter(cha) || Scanner.isNumber(cha)){
                token.string = token.string + (char)cha;
                this.nextChar();
            }
            token.kind = keywords.containsKey(token.string) ? keywords.get(token.string) : ident;
            return token;
        } else if (Scanner.isNumber(cha)){
            token = new Token(line, col);
            while(Scanner.isNumber(cha)){
                token.string = token.string + (char)cha;
                this.nextChar();
            }
            token.kind = number;
            token.val = Integer.parseInt(token.string);
            return token;
        } else if (Scanner.isSymbol(cha)) {
            token = new Token(line, col);
            switch(cha){
                case 0x2B: // Plus
                    token.kind = plus;
                    break;
                case 0x2D: // Minus
                    token.kind = minus;
                    break;
                case 0x2A: // Times
                    token.kind = times;
                    break;
                case 0x2F: // Slash
                    token.string += (char)cha;
                    nextChar();
                    if (cha == 0x2F){ // End of line comment
                        while(cha != '\n'){
                            nextChar();
                        }
                        return next();
                    } else if (cha == 0x2A) { // Block comment
                        boolean end = false;
                        while(!end){
                            nextChar();
                            if (cha == 0x2A){
                                nextChar();
                                if (cha == 0x2F){
                                    end = true;
                                }
                            } else if (cha == -1){
                                end = true;
                            }
                        }
                        return next();
                    } else {
                        token.kind = slash;
                    }
                    break;
                case 0x25: // Remainder
                    token.kind = rem;
                    break;
                case 0x21: // Not Equal
                    token.string += (char)cha;
                    nextChar();
                    if (cha == 0x3D){
                        token.kind = neq;
                    }
                    break;
                case 0x3B: // Semicolon
                    token.kind = semicolon;
                    break;
                case 0x2C: // Comma
                    token.kind = comma;
                    break;
                case 0x2E: // Period
                    token.kind = period;
                    break;
                case 0x28: // Left Par
                    token.kind = lpar;
                    break;
                case 0x29: // Right Par
                    token.kind = rpar;
                    break;
                case 0x5B: // Left Bracket
                    token.kind = lbrack;
                    break;
                case 0x5D: // Right Bracket
                    token.kind = rbrack;
                    break;
                case 0x7B: // Left Brace
                    token.kind = lbrace;
                    break;
                case 0x7D: // Right Brace
                    token.kind = rbrace;
                    break;
                case 0x3D: // Equals and Assign
                    token.string += (char)cha;
                    nextChar();
                    if (cha == 0x3D){
                        token.kind = eql;
                        token.string += (char)cha;
                    } else {
                        token.kind = assign;
                    }
                    break;
                case 0x3C: // Less than and Less and Equal than
                    token.string += (char)cha;
                    nextChar();
                    if (cha == 0x3D){
                        token.kind = leq;
                        token.string += (char)cha;
                    } else {
                        token.kind = lss;
                    }
                    break;
                case 0x3E: // Greater than and Greater and Equal than
                    token.string += (char)cha;
                    nextChar();
                    if (cha == 0x3D){
                        token.kind = geq;
                        token.string += (char)cha;
                    } else {
                        token.kind = gtr;
                    }
                    break;
                case 0x27: // Character Const
                    token.string += (char)cha;
                    nextChar();
                    if (Scanner.isLetter(cha)){
                        token.string += (char)cha;
                        nextChar();
                        if (cha == 0x27){
                            token.kind = charCon;
                            token.val = cha;
                        }
                    }
                    break;
            }
            token.string += (char)cha;
            nextChar();
            return token;
        } else if (Scanner.isWhiteSpace(cha)){
            nextChar();
            return next();
        } else if (cha == '\u0080' || cha == -1){
            token = new Token(line, col);
            fin = true;
            token.kind = eof;
            return token;
        }
        nextChar();
        return new Token(line, col);
    }

    public boolean isEOF(){
        return fin;
    }
}

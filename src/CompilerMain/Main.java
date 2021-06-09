package CompilerMain;

import java.io.*;

import CodeGeneration.Code;
import Parser.Parser;

public class Main {/*
    public static void main(String[] args) {
        ArrayList<Token> tokenList = new ArrayList<>();
        try {
            Reader reader = new FileReader(new File("H://Java//CompilerConstruction//TestScanner.mj"));
            Scanner tokenScanner = new Scanner(reader);
            while (!tokenScanner.isEOF()) {
                tokenList.add(tokenScanner.next());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        tokenList.forEach((Token token) -> {
            System.out.printf("Type: %s - %s | (%s, %s)\n", token.kind, token.string, token.line, token.col);
        });
    }*/

    public static void main(String[] args) {
        try {
            Parser.Parse();
            OutputStream output = new FileOutputStream(new File("H://Java//CompilerConstruction//MJCompiled"));
            Code.write(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

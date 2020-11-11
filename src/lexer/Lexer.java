package lexer;

import static control.Control.ConLexer.dump;

import java.util.*;

import java.io.InputStream;
import java.io.PushbackInputStream;

import lexer.Token.Kind;
import util.Todo;

public class Lexer
{
  String fname; // the input file name to be compiled

  PushbackInputStream fstream; // input stream for the above file


  private static final Map<String, Kind> keywords;

  private int current;

  private int linenum;
  
  
  
  static {
    keywords = new HashMap<>();
    keywords.put("boolean",  Kind.TOKEN_BOOLEAN);
    keywords.put("class",    Kind.TOKEN_CLASS);
    keywords.put("else",     Kind.TOKEN_ELSE);
    keywords.put("extends",  Kind.TOKEN_EXTENDS);
    keywords.put("false",    Kind.TOKEN_FALSE);
    keywords.put("if",       Kind.TOKEN_IF);
    keywords.put("int",      Kind.TOKEN_INT);
    keywords.put("length",   Kind.TOKEN_LENGTH);
    keywords.put("main",     Kind.TOKEN_MAIN);
    keywords.put("new",      Kind.TOKEN_NEW);
    keywords.put("out",      Kind.TOKEN_OUT);
    keywords.put("println",  Kind.TOKEN_PRINTLN);
    keywords.put("public",   Kind.TOKEN_PUBLIC);
    keywords.put("return",   Kind.TOKEN_RETURN);
    keywords.put("static",   Kind.TOKEN_STATIC);
    keywords.put("String",   Kind.TOKEN_STRING);
    keywords.put("System",   Kind.TOKEN_SYSTEM);
    keywords.put("this",     Kind.TOKEN_THIS);
    keywords.put("true",     Kind.TOKEN_TRUE);
    keywords.put("void",     Kind.TOKEN_VOID);
    keywords.put("while",    Kind.TOKEN_WHILE);
}

  public Lexer(String fname, InputStream fstream)
  {
    this.fname = fname;
    this.fstream =  new PushbackInputStream(fstream);
    this.current = ' ';
    this.linenum = 1;
  }

  // When called, return the next token (refer to the code "Token.java")
  // from the input stream.
  // Return TOKEN_EOF when reaching the end of the input stream.
  private Token nextTokenInternal() throws Exception
  {  
    int c = advance();
    
    //skip  all kinds of "blanks" and all comments 
    for(;;) {
    	if(' ' == c || '\t' == c || '\n' == c || '\r' == c) {
    		if('\n' == c)
    			this.linenum++;
    	    c = advance();
    	}
    	else if('/' == c) {
    		if(current == '/'){
    			advance();
    			while(current != '\n' && current != -1){
    				advance();
    	        }
    	        c = advance();
    	       }
    	       else if(current == '*'){
    	         advance();
    	         while(current != -1){
    	        	 if(current == '\n')  
    	        		  this.linenum++;
    	        	 int cr = advance();
    	        	 if(cr == '*' && current == '/'){
    	        		 advance();
    	        		 break;
    	        	 }
    	         }
    	         c = advance();
    	       }
    	       else {
    	    	  throw new Exception(" unexpected '\' ");
    	       }
    	}
    	else {
    		break;
    	}
    }
    
     if (-1 == c)
        return new Token(Kind.TOKEN_EOF, null);

      switch (c) {
        case '+':  return new Token(Kind.TOKEN_ADD, linenum);
        case '=':  return new Token(Kind.TOKEN_ASSIGN, linenum);
        case '.':  return new Token(Kind.TOKEN_DOT, linenum);
        case ',':  return new Token(Kind.TOKEN_COMMER, linenum);
        case '{':  return new Token(Kind.TOKEN_LBRACE, linenum);
        case '[':  return new Token(Kind.TOKEN_LBRACK, linenum);
        case '(':  return new Token(Kind.TOKEN_LPAREN, linenum);
        case '<':  return new Token(Kind.TOKEN_LT, linenum);
        case '!':  return new Token(Kind.TOKEN_NOT, linenum);
        case '}':  return new Token(Kind.TOKEN_RBRACE, linenum);
        case ']':  return new Token(Kind.TOKEN_RBRACK, linenum);
        case ')':  return new Token(Kind.TOKEN_RPAREN, linenum);
        case ';':  return new Token(Kind.TOKEN_SEMI, linenum);
        case '-':  return new Token(Kind.TOKEN_SUB, linenum);
        case '*':  return new Token(Kind.TOKEN_TIMES, linenum);
        case '&':
          if(current == '&') {
            advance();
            return new Token(Kind.TOKEN_AND, linenum);
          }
          else
            throw new Exception("expected &");
        default:
          // Lab 1, exercise 2: supply missing code to
          // lex other kinds of tokens.
          // Hint: think carefully about the basic
          // data structure and algorithms. The code
          // is not that much and may be less than 50 lines. If you
          // find you are writing a lot of code, you
          // are on the wrong way.
          if(Character.isDigit(c)) {
            StringBuffer str = new StringBuffer();
            str.append((char)c);
            while(Character.isDigit(current)) {
              str.append((char)current);  
              advance();
            }
            return new Token(Kind.TOKEN_NUM, linenum, str.toString());
          }
          
          else if(isAlnum(c)) {
            StringBuffer str = new StringBuffer();
            str.append((char)c);
            while(isAlnum(current)) {
              str.append((char)current);  
              advance();
            }
            String res = str.toString();
            if(keywords.containsKey(res)) {
              return new Token(keywords.get(res), linenum);
            }
            return new Token(Kind.TOKEN_ID, linenum, str.toString());
          }
          else
            throw new Exception("unexpected char");
        }
  }

  private int advance() throws Exception{
     int c = current;
     current = this.fstream.read();
     return c;
  }
  
  private boolean isAlnum(int c) {
	  return Character.isDigit(c) || Character.isLetter(c) || c == '_';
  }

  public Token nextToken()
  {
    Token t = null;

    try {
      t = this.nextTokenInternal();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    if (dump)
      System.out.println(t.toString());
    return t;
  }
}

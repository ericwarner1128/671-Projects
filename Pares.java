import java.io.*;
import java.util.*;

public class Parse
{
    private String line;
    private Scanner scan;
	private int[] vars;
	private boolean comt;

    // constructor
    public Parse()
    {
        line = "";
        scan = new Scanner(System.in);
        vars = new int[256];
        for(int i=0; i < 256; i++)
			vars[i] = 0;
    }


    // entry point into Parse
    public void run() throws IOException
    {
        String token;

        System.out.println("Welcome to SQRL...enjoy");
        token = getToken();
        parseCode(token);        // ::= <code>
    }

	// parse token for <code>
	private void parseCode(String token) throws IOException
	{
		do{
			parseStmt(token, true);      // ::= <stmt> <code> | <stmt>
			token = getToken();
		}while (!token.equals("."));
	}
	
	// checks for blanck space characters
	private void parseStmt(String token, boolean eval) throws IOException
	{
		int val;
		
		if (token.equals("load"))             // ::= load "<string>"
        {
            int i;
            String name = parseString(token);

            // interperter execution part
			if(eval)
				line = loadPgm(name) + line;
        }
		else if(token.equals("repeat"))
		{
			String var = getToken();
			int from = parseVar(var);
			
			token = getToken();
			if(!token.equals("to"))
				reportError(token);
			
			int to = parseVal(getToken());
			
			token = getToken();
			if(!token.equals("by"))
				reportError(token);
			
			int by = parseVal(getToken());
			
			token = getToken();
			
			String saveLine = line;
			// execution
			if(eval)
				for(int i=from; i<=to; i+=by)
				{
					vars[hashFun(var)%256] = i;
					line = saveLine;
					parseStmt(token, true);
				}
			else
				parseStmt(token, false);
			vars[hashFun(var)%256] = from;
			
		}
		else if(token.equals("print"))
		{
			token = getToken();
			val = parseExpr(token);            // ::= print <expr>
			
			// execution
			if(eval)
				System.out.println(val);
		}
		else if(token.equals("input"))
		{
			token = getToken();                // ::= input <var>
			if(!isVar(token))
				reportError(token);
			// execution
			if(eval)
			{
				System.out.print("? ");
			
				Scanner in = new Scanner(System.in);
				int input = in.nextInt();
				vars[hashFun(token)%256] = input;
			}
		}
		else if(token.equals("if"))
		{
			token = getToken();
			boolean cond = parseCond(token);
			if(cond)                      // ::= if <cond> <stmt>
			{
				token = getToken();
				
				// execution
				if(eval)
					parseStmt(token, true);
				else
					parseStmt(token, false);
			}
			else
			{
				token = getToken();
			    
				// execution
				parseStmt(token, false);
			}
			// else case                  // ::= else <stmt>
			token = getToken();
			
			if(token.equals("else"))
			{
				token = getToken();
				if(!cond)
					parseStmt(token, true);
				else
					parseStmt(token, false);
			}
			else if(token.equals("."))
				System.exit(1);
			else
				parseStmt(token, true);
		}
		else if(isVar(token))
		{
			String var = token;
			token = getToken();
			if(!token.equals("="))
			{
				reportError(token);
			}
			val = parseExpr(getToken());
			
			// execution
			if(eval)
				vars[hashFun(var)%256] = val;
		}
		else if(token.equals("#"))
		{
			
		}
		else if(token.equals("."))
			System.exit(1);
		else
			reportError(token);
	}
	
	// parse for <string>...this skips lexical analyzer getToken()
    private String parseString(String token)
    {

        int i;
        String str = "";

        // skip blanks                                                      
        line = skipLeadBlanks(line);

        // grab string between quotes                                      
        if (!nextChar('"'))
            reportError(token + " ");
        for (i=1; i<line.length(); i++)
            if (line.charAt(i) == '"')
                break;
            else
                str += line.charAt(i);

        // if no trailing quote error                                       
        if (i >= line.length())
            reportError(token + " ");

        // remove string from line and load program                         
        line = line.substring(i+1);

        return str;
    }
	
	// lookahead to next character 
    private boolean nextChar(char ch) 
    { 
        return line.charAt(0) == ch; 
    }   
	
    // loads program from file                                                  
    private String loadPgm(String name) throws IOException
    {
        String buffer = "";
        File file = new File(name);
        Scanner fileScan = new Scanner(file);

        while (fileScan.hasNextLine())
            buffer += fileScan.nextLine() + "\n";

        return buffer;
    }

	
	// parse token for cond
	private boolean parseCond(String token)
	{
		boolean eval;
		
		int val;
		val = parseVal(token);
		String opToken = getToken();
		
		switch (opToken.charAt(0))
		{
			case '<':
				eval = val < parseVal(getToken());  // ::= <val> < <val>
				break;
 			case '>':
				eval = val > parseVal(getToken());  // ::= <val> > <val>
				break;
			case '=':
				getToken();
				eval = val == parseVal(getToken()); // ::= <val> == <val>
				break;
			default :
				eval = false;
				reportError(token);
		}
		return eval;
	}
	
	// parse token for <expr>
	private int parseExpr(String token)
	{
		int val; 
		String opToken;
		val = parseVal(token);
		
		opToken = getToken();
		
		switch (opToken.charAt(0))
		{
			case '+':                   // ::= <val> + <val>
				token = getToken();
				val = val + parseVal(token);
				break;
			case '-':                   // ::= <val> - <val>
				token = getToken();
				val = val - parseVal(token);
				break;
			case '*':                   // ::= <val> * <val>
				token = getToken();
				val = val * parseVal(token);
				break;
			case '/':                   // ::= <val> / <val>
				token = getToken();
				val = val / parseVal(token);
				break;
			default :
				line = opToken + line;
		}
		
		return val;
	}
	
	// parse token for <val> ant return its value
	private int parseVal(String token)
	{
		if(isNumeric(token))
			return Integer.parseInt(token);
			
		if(isVar(token))
			return parseVar(token);
		
		reportError(token);
		
		return -1;  // won't compile without this
	}
	
	// parse token for <var> and return its value
	private int parseVar(String token)
	{
		if(!isVar(token))         // ::= a | b | c | ...
			reportError(token);
			
		// otherwise we have to access memory???
		return vars[hashFun(token)%256];
	}
	
	
	// parse token to see if it's a variable
	private boolean isVar(String token)
	{
		for(int i=0; i<token.length(); i++)
		{
			if( isAlpha(token.charAt(i)) || isNumeric(token.substring(i,i+1)) ||
				!isDelim(token.charAt(i)) || !isBlank(token.charAt(i)) )
			{}
			else
				return false;
		}
		return true;
	}
	
	// return true if token it <num>
	private boolean isNumeric(String token)
	{
		for(int i=0; i<token.length(); i++)
		{
			if (!Character.isDigit(token.charAt(i)))
				return false;
		} 
		return true;
	}
	
	// checks if alphabetic character
	private boolean isAlpha(char ch)
	{
		return ((int) ch) >= 97 && ((int) ch) <= 122;
	}
	
    // checks for blank space characters
    private boolean isBlank(char ch)
    {
        switch (ch)
        {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                return true;
        }

        return false;
    } 

    // checs for delimeters
    private boolean isDelim(char ch)
    {
        if (isBlank(ch))
            return true;

        switch (ch)
        {
            case '.':
            case '+': case '-': case '*': case '/':
            case '=': case '>': case '<':
                return true;
        }
        
        return false;
    }


    // skip over leading blank space
    private String skipLeadBlanks(String buffer)
    {
        int i;
        for (i=0; i<buffer.length(); i++)
            if (!isBlank(buffer.charAt(i)))
            break;
        return buffer.substring(i);
    }


    // gets next token
    private String getToken()
    {
        int i;
        String token;

        line = skipLeadBlanks(line);
        // grab a non-blank line
        while (line.length() == 0)
        {
            line = scan.nextLine();
            line = skipLeadBlanks(line);
        }
        // grab our actual token
        for (i=0; i<line.length(); i++)
		{
			if(line.charAt(i) == '#')
			{
				int s = i;
				while(i<line.length() && line.charAt(i) != '\n')
					i++;
				
				if(i != line.length())
					line = line.substring(i+1);
				else
					line = line.substring(i);
				return getToken();
			}
            if(isDelim(line.charAt(i)))
            {
                if (i == 0)
                    i++;

                token = line.substring(0, i);
				
				if(line.charAt(0) == '.')
					System.exit(1);
				
                line = line.substring(i);
				return token;
            }
		}
        // entire line is token
        token = line;
        line = "";
        return token;
    }
    
    // reports a syntax error printing the remainder of buffer
    private void reportError(String token)
    {
		line += "\n";
        line = line.substring(0, line.indexOf("\n"));

        System.out.println("ERROR: " + token + line);
        for (int i = 0; i < 7+token.length(); i++)
            System.out.print(" ");
        System.out.println("^");

        System.exit(-1);
	}
	
	// hash function used for var assignment
	private int hashFun(String str)
	{
		int sum = 0;
		for(int i=0; i<str.length(); i++)
		{
			int ch = (int)str.charAt(i);
			sum = sum + ch;
		}
		return sum;
	}
}

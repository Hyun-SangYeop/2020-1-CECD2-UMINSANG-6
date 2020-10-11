//import java.util.LinkedList;

import com.gliwka.hyperscan.wrapper.*;
//import com.gliwka.hyperscan.wrapper.Expression;
//import com.gliwka.hyperscan.wrapper.Scanner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;


public class Inspection_hyperscan {
	
	static final String SSN_PTTN = "([0-9][0-9][0-1][0-9][0-3][0-9])[-\\x20]?([1234]\\d{6})";
	static final String MPH_PTTN = "(82-1(0-[2-9][0-9]|1-([2-8]|17|9[0-9])|[69]-([2-8]|9[0-9])|[78]-[2-8])[0-9]{2}-[0-9]{4}|01(0[-\\x20]?[2-9][0-9]|1[-\\x20]?([2-8]|17|9[0-9])|[69][-\\x20]?([2-8]|9[0-9])|[78][-\\x20]?[2-8])[0-9]{2}[-\\x20]?[0-9]{4})";
	static final String PHN_PTTN = "(82-(2|31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64|70)-[0-9]{3,4}-[0-9]{4}|0(2|31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64|70)[-\\x20\\)]?[0-9]{3,4}[-\\x20]?[0-9]{4})";
	static final String HIN_PTTN = "([0-9]{11})";
	static final String CCARD_PTTN = "((5[1-5]\\d{14})|(4\\d{12}(\\d{3})?)|(3[47]\\d{13})|(6011\\d{12})|((30[0-5]|3[68]\\d)\\d{11}))";
	
	void checker(String path, String encoding) throws UnsupportedEncodingException, IOException {
		String txt = new String(Files.readAllBytes(Paths.get(path)), encoding);
		
		//we define a list containing all of our expressions
		LinkedList<Expression> expressions = new LinkedList<Expression>();

		//the first argument in the constructor is the regular pattern, the latter one is a expression flag
		//make sure you read the original hyperscan documentation to learn more about flags
		//or browse the ExpressionFlag.java in this repo.

		// SOM_LEFTMOST는 가장 왼쪽부터??
		//expressions.add(new Expression(expr, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
		expressions.add(new Expression(SSN_PTTN, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
		expressions.add(new Expression(MPH_PTTN, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
		expressions.add(new Expression(PHN_PTTN, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
		expressions.add(new Expression(HIN_PTTN, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
		expressions.add(new Expression(CCARD_PTTN, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
		//expressions.add(new Expression("Test", EnumSet.of(ExpressionFlag.SINGLEMATCH))); //.CASELESS)));
		
		//expressions.add(new Expression("([0-9][0-9][0-1][0-9][0-3][0-9])[-\\\\x20]?([1234]\\\\d{6})", ))

		//we precompile the expression into a database.
		//you can compile single expression instances or lists of expressions

		//since we're interacting with native handles always use try-with-resources or call the close method after use
		try(Database db = Database.compile(expressions)) {
		    //initialize scanner - one scanner per thread!
		    //same here, always use try-with-resources or call the close method after use
		    try(Scanner scanner = new Scanner())
		    {
		        //allocate scratch space matching the passed database
		        scanner.allocScratch(db);


		        //provide the database and the input string
		        //returns a list with matches
		        //synchronized method, only one execution at a time (use more scanner instances for multithreading)
		        List<Match> matches = scanner.scan(db, txt);
		        //List<Match> matches = scanner.scan(db, "12345 test string");
		        
		        //////////////////////////
		        ///// 1. 각 PPTN별 VALID 필요
		        ////  2. 코드 리팩토링 필요
		        //////////////////////////
		        
		        for(Match i : matches) {
		        	System.out.println(i.getMatchedExpression().getExpression());
		        	System.out.println(i.getMatchedString());
		        	System.out.println(i.getStartPosition() + " " + i.getEndPosition() + "\n");
		        }
		        
		        //matches always contain the expression causing the match and the end position of the match
		        //the start position and the matches string it self is only part of a matach if the
		        //SOM_LEFTMOST is set (for more details refer to the original hyperscan documentation)
		    }

		    // Save the database to the file system for later use
		    try(OutputStream out = new FileOutputStream("db")) {
		        db.save(out);
		    }

		    // Later, load the database back in. This is useful for large databases that take a long time to compile.
		    // You can compile them offline, save them to a file, and then quickly load them in at runtime.
		    // The load has to happen on the same type of platform as the save.
		    try (InputStream in = new FileInputStream("db");
		         Database loadedDb = Database.load(in)) {
		        // Use the loadedDb as before.
		    }
		}
		catch (CompileErrorException ce) {
		    //gets thrown during  compile in case something with the expression is wrong
		    //you can retrieve the expression causing the exception like this:
		    Expression failedExpression = ce.getFailedExpression();
		}
		catch(IOException ie) {
		  //IO during serializing / deserializing failed
		}
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		new Inspection_hyperscan().checker("./testfile/test.txt", "UTF-8");	
	}
	
}


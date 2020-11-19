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
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import java.net.InetSocketAddress;


public class Inspection_hyperscan {
	
	static final String SSN_PTTN = "([0-9][0-9][0-1][0-9][0-3][0-9])[-\\x20]?([1234]\\d{6})";
	static final String MPH_PTTN = "(82-1(0-[2-9][0-9]|1-([2-8]|17|9[0-9])|[69]-([2-8]|9[0-9])|[78]-[2-8])[0-9]{2}-[0-9]{4}|01(0[-\\x20]?[2-9][0-9]|1[-\\x20]?([2-8]|17|9[0-9])|[69][-\\x20]?([2-8]|9[0-9])|[78][-\\x20]?[2-8])[0-9]{2}[-\\x20]?[0-9]{4})";
	static final String PHN_PTTN = "(82-(2|31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64|70)-[0-9]{3,4}-[0-9]{4}|0(2|31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64|70)[-\\x20\\)]?[0-9]{3,4}[-\\x20]?[0-9]{4})";
	static final String HIN_PTTN = "([0-9]{11})";
	//static final String CCARD_PTTN = "((5[1-5]\\d{14})|(4\\d{12}(\\d{3})?)|(3[47]\\d{13})|(6011\\d{12})|((30[0-5]|3[68]\\d)\\d{11}))";
	
	// number of each detection (0:SSN, 1:MPH, 2:PHN, 3:HIN)
	public static long checkNum[] = {0, 0, 0, 0};
	
	public static final int THIS_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    public static final int NUM_DAYS[] = {-1, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public static boolean isValidDate(int y, int m, int d) {
        return 1 <= m && m <= 12 && 1 <= d && d <= NUM_DAYS[m] &&
                (m != 2 || d != 29 || (y % 4 == 0 && y % 100 != 0 || y % 400 == 0));
    }
	
	static final boolean SSN_VALID(String ssn_str) {
		// 111111-1111111
    	// 1111111111111
    	// 111111 1111111
		
		int ssn[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		int k=0;
		char tmp;
		for(int i=0; i<ssn_str.length(); i++) {
			tmp = ssn_str.charAt(i);
			if(tmp >= 48 && tmp <= 57)
				ssn[k++] = tmp - 48;
		}
        
        int year = ((ssn[6] <= 2) ? 1900 : 2000) + ssn[0] * 10 + ssn[1];        
        if (THIS_YEAR < year) {
        	return false;
        }

        if (!isValidDate(year, ssn[2] * 10 + ssn[3], ssn[4] * 10 + ssn[5])) {
            return false;
        }

        int wsum = (2 * ssn[0] + 3 * ssn[1] + 4 * ssn[2] + 5 * ssn[3] + 6 * ssn[4] + 7 * ssn[5] + 8 * ssn[6] + 9 * ssn[7] + 2 * ssn[8]
                + 3 * ssn[9] + 4 * ssn[10] + 5 * ssn[11]);

        // 맨 마지막자리가 체크섬이다.
        return (11 - wsum % 11) % 10 == ssn[12];
		
	}
	
	static final boolean MPH_VALID(String mph_str) {
	//  82-111-111-1111
    	//	82-111-1111-1111
    	//  1111111111   	// 10자리
    	//	111-111-1111 	// 구분자 다르면 안된다. (-이거나 띄어쓰기로 맞춰야함)
    	//	11111111111		// 11자리
    	//	111-1111-1111	// 구분자 다르면 안된다. (-이거나 띄어쓰기로 맞춰야함)
    	
    	int space = 0, dash = 0;
    	// 자리수를 예측하기 어려우므로 ArrayList를 사용
    	ArrayList<Integer> mph = new ArrayList<Integer>();
    	char tmp;
    		
    	for(int i = 0; i < mph_str.length(); i++) {
    		tmp = mph_str.charAt(i);
    		if(tmp >= 48 && tmp <= 57)
    			mph.add(tmp - 48);
    		else if (tmp == 32) // SP
                space++;
            else if (tmp == 45) // '-'
                dash++;
    	}
    	
    	// 국가코드 82 검출 및 82를 0으로 바꿔준다.
    	// RE에서 8로 시작하면 82일 수 밖에 없다.
    	if (mph.get(0) == 8) {
    		mph.set(0, 0);
    		mph.remove(1);
    		dash--; // 82-로 시작하는데 아래 dash/space check시에 반영되면 안된다.
    	}
    	
    	// (dash, space) 가능한 쌍은 (0,0) (0,2) (2,0)
    	// 즉, (1,0) (0,1) (1,1)은 불가능
    	if(dash == 1 || space == 1)
    		return false;
    	
    	// 맨 뒤 4자리가 0000이면 false
    	int len = mph.size();
    	if(mph.get(len-4) == 0 && mph.get(len-3) == 0 && mph.get(len-2) == 0 && mph.get(len-1) == 0)
    		return false;    	
    	
        return true;
	}
	
	static final boolean PHN_VALID(String phn_str) {
		// 82-2-111-1111
    	// 82-2-1111-1111
    	// 82-11-111-1111
    	// 82-11-1111-1111
    	
    	// 111111111 // 9자리
    	// 11-1111111
    	// 11111-1111
    	// 11-111-1111

        // 1111111111 // 10자리
        // 11-11111111
    	// 111-1111111
    	// 11-1111-1111
    	// 111-111-1111

        // 11111111111 // 11자리
        // 111-11111111
        // 1111111-1111
        // 111-1111-1111
    	
    	// 구분자 쌍 무시
    	// 자리수를 예측하기 어려우므로 ArrayList를 사용
    	ArrayList<Integer> phn = new ArrayList<Integer>();
    	char tmp;
    		
    	for(int i = 0; i < phn_str.length(); i++) {
    		tmp = phn_str.charAt(i);
    		if(tmp >= 48 && tmp <= 57)
    			phn.add(tmp - 48);
    	}

        // 한국 국가코드(82) -> 0으로 치환
        if(phn.get(0) == 8){
        	phn.set(0, 0);
    		phn.remove(1);
        }
        
        // 070은 11자리 이다.
        if(phn.get(1) == 7 && phn.size() != 11)
        	return false;

        // 02면 3번째 자리에 0 또는 1이 없다.
        if(phn.get(1) == 2 && (phn.get(2) == 0 || phn.get(2) == 1))
        	return false;
        
        // 032 등 지역번호 다음 자리에 0 또는 1이 없다.
        if(phn.get(1) != 2 && (phn.get(3) == 0 || phn.get(3) == 1))
        	return false;

        return true;
	}
	
	static final boolean HIN_VALID(String hin_str) {
		// 11111111111
    	int[] hin = IntStream.range(0, 11).map(i -> hin_str.charAt(i) - '0').toArray();
    	
    	int wsum = (hin[0]*3 + hin[1]*5 + hin[2]*7 + hin[3]*9 +
    			hin[4]*3 + hin[5]*5 + hin[6]*7 + hin[7]*9 +
    			hin[8]*3 + hin[9]*5) % 11;
    	
    	return (wsum <= 1 ? 1 : 11) - wsum == hin[10];
	}
	
	static final boolean CCARD_VALID(String card) {
		// The validity of the credit card number is attributable to each company, making it difficult to check as an open source.
		return true;
	}
	
	boolean matchValid(String expr, String str) {
    	switch(expr) {
    	case SSN_PTTN: 
    		if(SSN_VALID(str)) {
    			checkNum[0]++; //System.out.print("SSN: ");
    			return true;
    		}
    		else return false;
    	case MPH_PTTN: 
    		if(MPH_VALID(str)) {
    			checkNum[1]++; //System.out.print("MPH: ");
    			return true;
    		}
    		else return false;
    	case PHN_PTTN: 
    		if(PHN_VALID(str)) {
    			checkNum[2]++; //System.out.print("PHN: ");
    			return true;
    		}
    		else return false;
    	case HIN_PTTN: 
    		if(HIN_VALID(str)) {
    			checkNum[3]++; //System.out.print("HIN: ");
    			return true;
    		}
    		else return false;
    	//case CCARD_PTTN: return CCARD_VALID(str);
    	default: return false;
    	}
    }
	
	int checker(String path, String encoding) throws UnsupportedEncodingException, IOException {
		//String txt = new String(Files.readAllBytes(Paths.get(path)), encoding);
		 String txt = Extractor.extract(path);
		
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
		//expressions.add(new Expression(CCARD_PTTN, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
		
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
		        	
		        	if(matchValid(i.getMatchedExpression().getExpression(),
		        			i.getMatchedString()))
		        			{
		        				//System.out.println(i.getMatchedString());
		        			}
		        	
		        	//System.out.println(i.getMatchedExpression().getExpression());
		        	//System.out.println(i.getMatchedString());
		        	//System.out.println(i.getStartPosition() + " " + i.getEndPosition() + "\n");
		        	
		        	
		        }
		        
		        System.out.println("\n\nInspect personal information version Hyperscan");
				System.out.println("SSN " + checkNum[0]);
				System.out.println("MPH " + checkNum[1]);
				System.out.println("PHN " + checkNum[2]);
				System.out.println("HIN " + checkNum[3]);
		        		        
		        //matches always contain the expression causing the match and the end position of the match
		        //the start position and the matches string it self is only part of a matach if the
		        //SOM_LEFTMOST is set (for more details refer to the original hyperscan documentation)
		    }

//		    // Save the database to the file system for later use
//		    try(OutputStream out = new FileOutputStream("db")) {
//		        db.save(out);
//		    }
//
//		    // Later, load the database back in. This is useful for large databases that take a long time to compile.
//		    // You can compile them offline, save them to a file, and then quickly load them in at runtime.
//		    // The load has to happen on the same type of platform as the save.
//		    try (InputStream in = new FileInputStream("db");
//		         Database loadedDb = Database.load(in)) {
//		        // Use the loadedDb as before.
//		    }
		}
		catch (CompileErrorException ce) {
		    //gets thrown during  compile in case something with the expression is wrong
		    //you can retrieve the expression causing the exception like this:
		    Expression failedExpression = ce.getFailedExpression();
		    System.out.println(failedExpression.getExpression());
		}
		catch(IOException ie) {
		  //IO during serializing / deserializing failed
		}
		finally
		{
			// 통제 정책
			if(checkNum[0] > 5 || checkNum[1] >5
					|| checkNum[2] > 5 || checkNum[3] > 5
					|| (checkNum[0] + checkNum[1]
							+ checkNum[2] + checkNum[3] > 5))
				return 1;
			else
				return 0;
		}
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		
//		long beforeTime = System.currentTimeMillis();
//		
//		new Inspection_hyperscan().checker("./testfile/test100000.txt", "UTF-8");
//		long afterTime = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
//		double secDiffTime = (afterTime - beforeTime)/1000.0; //두 시간에 차 계산
//		System.out.println("시간차이(s) : "+secDiffTime);
		
		
		
		int port=65500;
		ServerSocket s_socket = new ServerSocket();
		s_socket.bind(new InetSocketAddress("10.0.2.15", port));

		while (true) {
			try

			{
				checkNum[0] = 0; // ssn
				checkNum[1] = 0; // mph
				checkNum[2] = 0; // phn
				checkNum[3] = 0; // hin

				

				s_socket.setReuseAddress(true);

				Socket client_socket = s_socket.accept();

				byte[] byteArr = new byte[1024];

				InputStream client_data = client_socket.getInputStream();

				int readByteCount = client_data.read(byteArr);
				//System.out.println(readByteCount);
				String fromClient = new String(byteArr, 0, readByteCount, "UTF-8");

				//System.out.println("from c-client: " + fromClient);

				//long beforeTime1 = System.currentTimeMillis();
				int inspection = new Inspection_hyperscan().checker(fromClient, "UTF-8");
				//long afterTime1 = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
				//double secDiffTime1 = (afterTime1 - beforeTime1) / 1000.0; // 두 시간에 차 계산
				//System.out.println("시간차이(s) : " + secDiffTime1);

				//String sendDataString = String.valueOf(secDiffTime1);
				//String sendDataString = String.valueOf(inspection);
				String sendDataString = inspection + " " + checkNum[0] + " "
						+ checkNum[1] + " " + checkNum[2] + " " + checkNum[3] + "\0";
				//System.out.println("\n" + sendDataString + "\n");

				OutputStream server_data = client_socket.getOutputStream();

				server_data.write(sendDataString.getBytes());

				client_socket.close();

				//s_socket.close(); while문 바깥에 추가요망

			}

			catch (Exception e) {

				e.printStackTrace();

			}

		}
	}
	
}


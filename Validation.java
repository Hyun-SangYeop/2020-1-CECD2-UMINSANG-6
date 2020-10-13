
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.stream.IntStream;
import java.io.File;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

// 예전 이름 FileScanner
public class Validation {
	
	final static int SSNCONDITION=1;
	final static int MPHCONDITION=1;
	final static int PHNCONDITION=1;
	final static int HINCONDITION=0;
	
	static int SSNCOUNT;
	static int MPHCOUNT;
	static int PHNCOUNT;
	static int HINCOUNT;
	

	// Discover에서 compile하는데 이를 평가할 떄마다 할 수 없는 노릇
	// 1번만 해도 충분한 것들은 다 뺴놓자
    void validate(String path, String encoding) throws IOException, SAXException, TikaException {
        Discover d = new Discover(Arrays.asList(SSN, MPH, PHN, HIN));
        SSNCOUNT=0;MPHCOUNT=0;PHNCOUNT=0;HINCOUNT=0;

		//String txt = new String(Files.readAllBytes(Paths.get(path)), encoding);
        String txt = new Extractor().extract(path);

        d.scan(txt, (idf, start, end) -> {
            System.out.println(idf + ": " + txt.substring(start, end));
        });
        
        File file=new File(path);
        if(SSNCOUNT>SSNCONDITION||MPHCOUNT>MPHCONDITION||PHNCOUNT>PHNCONDITION||HINCOUNT>HINCONDITION) {
        	if(file.delete()) {
        		System.out.println("파일 삭제 성공");
        	}else {
        		System.out.println("파일 삭제 실패");
        	}
        }
        
        
        
    }
    
    /*
     * Helpers
     */
    
    public static final int THIS_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    public static final int NUM_DAYS[] = {-1, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public static boolean isValidDate(int y, int m, int d) {
        return 1 <= m && m <= 12 && 1 <= d && d <= NUM_DAYS[m] &&
                (m != 2 || d != 29 || (y % 4 == 0 && y % 100 != 0 || y % 400 == 0));
    }

    /*
     * Concrete identifiers
     */

    // 괄호는 capturing group
    static final String SSN_PTTN = "([0-9][0-9][0-1][0-9][0-3][0-9])[-\\x20]?([1234]\\d{6})";
    // Identifier class의 interface 내부 함수를 lamda함수로 구현
    static final Identifier.Validator SSN_VALID = g -> {
    	// 111111-1111111
    	// 1111111111111
    	// 111111 1111111
    	
        // 앞 6자리 (g[0]은 전체, g[1]은 첫번째 그룹, ...)
        int[] ssn6 = IntStream.range(0, 6).map(i -> g[1].charAt(i) - '0').toArray();
        // 뒤 7자리
        int[] ssn7 = IntStream.range(0, 7).map(i -> g[2].charAt(i) - '0').toArray();
        
        int year = ((ssn7[0] <= 2) ? 1900 : 2000) + ssn6[0] * 10 + ssn6[1];        
        if (THIS_YEAR < year) {
        	return false;
        }

        if (!isValidDate(year, ssn6[2] * 10 + ssn6[3], ssn6[4] * 10 + ssn6[5])) {
            return false;
        }

        int wsum = (2 * ssn6[0] + 3 * ssn6[1] + 4 * ssn6[2] + 5 * ssn6[3] + 6 * ssn6[4] + 7 * ssn6[5] + 8 * ssn7[0] + 9 * ssn7[1] + 2 * ssn7[2]
                + 3 * ssn7[3] + 4 * ssn7[4] + 5 * ssn7[5]);

        boolean isSSNValidationOK=(11 - wsum % 11) % 10 == ssn7[6];
        
        if(isSSNValidationOK) {
        	SSNCOUNT++;
        	return true;
        }
        else {
        	return false;
        }
        // 맨 마지막자리가 체크섬이다.
        //return (11 - wsum % 11) % 10 == ssn7[6];
    };

    static final Identifier SSN = new Identifier("주민등록번호", SSN_PTTN, SSN_VALID);
    

    //휴대전화번호
    static final String MPH_PTTN = "82-1(0-[2-9][0-9]|1-([2-8]|17|9[0-9])|[69]-([2-8]|9[0-9])|[78]-[2-8])[0-9]{2}-[0-9]{4}|01(0[-\\x20]?[2-9][0-9]|1[-\\x20]?([2-8]|17|9[0-9])|[69][-\\x20]?([2-8]|9[0-9])|[78][-\\x20]?[2-8])[0-9]{2}[-\\x20]?[0-9]{4}";
    // 010인 경우 중간번호는 4자리이며 0,1로 시작 불가능
    // 011-200~499,500~899 존재
    // 011-1700~1799,9000~9499,9500~9999 존재
    // 017-200~499,500~899 존재
    // 016-200~499,500~899 존재
    // 016-9000~9499,9500~9999 존재
    // 018-200~499,500~899 존재
    // 019-200~499,500~899 존재
    // 019-9000~9499,9500~9999 존재
    
    static final Identifier.Validator MPH_VALID = g -> {
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
    		
    	for(int i = 0; i < g[0].length(); i++) {
    		tmp = g[0].charAt(i);
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
    	
    	
    	MPHCOUNT++;
        return true;
    };

    static final Identifier MPH = new Identifier("휴대전화번호", MPH_PTTN, MPH_VALID);

    
    //전화번호
    static final String PHN_PTTN = "82-(2|31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64|70)-[0-9]{3,4}-[0-9]{4}|0(2|31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64|70)[-\\x20\\)]?[0-9]{3,4}[-\\x20]?[0-9]{4}";
    // 서울 02 경기 031 인천 032 강원 033 충남 041 대전 042 충북 043 세종 044
    // 부산 051 울산 052 대구 053 경북 054 경남 055 전남 061 광주 062 전북 063 제주 064
    // 인터넷 전화 070
    
    static final Identifier.Validator PHN_VALID = g -> {
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
    		
    	for(int i = 0; i < g[0].length(); i++) {
    		tmp = g[0].charAt(i);
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

        
        PHNCOUNT++;
        return true;
    };

    static final Identifier PHN = new Identifier("집전화번호", PHN_PTTN, PHN_VALID);

    
    // 건강보험증번호
    static final String HIN_PTTN = "[0-9]{11}";
    static final Identifier.Validator HIN_VALID = g -> {
    	// 11111111111
    	int[] hin = IntStream.range(0, 11).map(i -> g[0].charAt(i) - '0').toArray();
    	
    	int wsum = (hin[0]*3 + hin[1]*5 + hin[2]*7 + hin[3]*9 +
    			hin[4]*3 + hin[5]*5 + hin[6]*7 + hin[7]*9 +
    			hin[8]*3 + hin[9]*5) % 11;
    	
    	boolean isHINValidationOK=(wsum <= 1 ? 1 : 11) - wsum == hin[10];
    	
    	if(isHINValidationOK) {
    		HINCOUNT++;
    		return true;
    	}
    	else
    		return false;
    	
    	//return (wsum <= 1 ? 1 : 11) - wsum == hin[10];
    };
    static final Identifier HIN = new Identifier("건강보험증번호", HIN_PTTN, HIN_VALID);
    
    /////////////////////////////////////////////////
    //////////////// 아래는 아직 수정 안함 //////////////////
    ////////////////////////////////////////////////////
    
    
    
    
    //credit card number

    static final String CCARD_PTTN = "(5[1-5]\\d{14})|(4\\d{12}(\\d{3})?)|(3[47]\\d{13})|(6011\\d{12})|((30[0-5]|3[68]\\d)\\d{11})";

    static final Identifier.Validator CCARD_VALID = g -> {
        return true;
    };

    static final Identifier CCARD = new Identifier("신용카드번호", CCARD_PTTN, CCARD_VALID);


    /*
     * main
     */
//    public static void main(String[] args) throws IOException, SAXException, TikaException {
//        //new Validation().validate("./testfile/test.txt", "UTF-8");
//    	new Validation().validate("/media/user/C2B8023BB8022E8B/test", "UTF-8");
//    }
}

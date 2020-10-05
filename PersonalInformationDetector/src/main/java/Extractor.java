import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Extractor {
    public static String extract(String url)throws IOException, SAXException, TikaException {
        //String urlString = "/Users/KYUMIN/pjt/test3.hwp";
        //metadata 추출
//        InputStream inputStream = new FileInputStream(urlString);
//
//        AutoDetectParser parser = new AutoDetectParser(new HwpV5Parser());
//        Metadata metadata = new Metadata();
//        DefaultHandler contentHandler = new DefaultHandler();
//        parser.parse(inputStream,contentHandler,metadata);
//        System.out.println(metadata.toString());

        //내용물 추출
        //한글파일, word, ppt, pdf 추출 가능
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try(InputStream inputStream = new FileInputStream(url)){
            parser.parse(inputStream,handler,metadata);
            return handler.toString();
        }
    }
}

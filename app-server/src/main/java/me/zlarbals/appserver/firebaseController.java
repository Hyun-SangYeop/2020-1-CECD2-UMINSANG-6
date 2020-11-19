package me.zlarbals.appserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class firebaseController {

    //public String fcmtest(HttpServletRequest request, HttpServletResponse response, Model model){

    @GetMapping("/test")
    public String fcmtest(@RequestParam String filepath){

        FcmUtil fcmUtil=new FcmUtil();


        //String tokenId="cEnZCfB2vEQ:APA91bGroHMtH3oWEZKuZftjanwmZ5RR_TU4dOpLADLv47dAr5AI262QUAhw45vPjq6sak4t1v0AulIxLzltEX6pxJxYFzbyB929Cvu6MD7tPYeG7ScStMxdubo_OAKpFosskVocBmA1";
        String title="DLP";
        String content=filepath+"\n 복사/이동이 감지되었습니다.";
        // \n ssn="+ssn;" mph="+mph+" phn="+phn+" hin="+hin;

        fcmUtil.send_FCM(title,content);
        System.out.println(filepath);
        return filepath;
    }

}

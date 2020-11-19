package me.zlarbals.firebasetest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class firebaseController {

    //public String fcmtest(HttpServletRequest request, HttpServletResponse response, Model model){

    @GetMapping("/test")
    public String fcmtest(@RequestParam String filepath){

        FcmUtil fcmUtil=new FcmUtil();


        //String tokenId="cEnZCfB2vEQ:APA91bGroHMtH3oWEZKuZftjanwmZ5RR_TU4dOpLADLv47dAr5AI262QUAhw45vPjq6sak4t1v0AulIxLzltEX6pxJxYFzbyB929Cvu6MD7tPYeG7ScStMxdubo_OAKpFosskVocBmA1";
        String title="DLP";
        String content=filepath+"\n 복사/이동이 감지되었습니다.";

        fcmUtil.send_FCM(title,content);
        System.out.println(filepath);
        return filepath;
    }

}

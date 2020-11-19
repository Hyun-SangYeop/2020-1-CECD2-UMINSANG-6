package me.zlarbals.appserver;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Component
public class FcmUtil {
    //    public void send_FCM(String toeknId,String title,String content){
//        try{
//            FileInputStream serviceAccount =
//                    new FileInputStream("C:\\Project\\firebasetest\\src\\main\\resources\\fcmexam-2f5c0-firebase-adminsdk-b5x5u-c5c66a4fed.json");
//
//            FirebaseOptions options = new FirebaseOptions.Builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .setDatabaseUrl("https://fcmexam-2f5c0.firebaseio.com")
//                    .build();
//
//            if(FirebaseApp.getApps().isEmpty()){
//                FirebaseApp.initializeApp(options);
//            }
//
//            String registrationToken = toeknId;
//
//            String topic="dongguk";
//            Message message = Message.builder()
//                    .setAndroidConfig(AndroidConfig.builder()
//                            .setTtl(3600*1000)
//                            .setPriority(AndroidConfig.Priority.NORMAL)
//                            .setNotification(AndroidNotification.builder()
//                                    .setTitle(title)
//                                    .setBody(content)
//                                    .setIcon("stock_ticker_update")
//                                    .setColor("#f45342")
//                                    .build())
//                            .build())
//                    //.setToken(registrationToken)
//                    .setTopic(topic)
//                    .build();
//
//            String response = FirebaseMessaging.getInstance().send(message);
//            System.out.println("Successfully sent message: "+response);
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (FirebaseMessagingException e) {
//            e.printStackTrace();
//        }
//    }
    public void send_FCM(String title,String content){
        try{
            FileInputStream serviceAccount =
                    new FileInputStream("./src/main/resources/fcmexam-2f5c0-firebase-adminsdk-b5x5u-c5c66a4fed.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://fcmexam-2f5c0.firebaseio.com")
                    .build();

            if(FirebaseApp.getApps().isEmpty()){
                FirebaseApp.initializeApp(options);
            }

            //String registrationToken = toeknId;

            String topic="dongguk";
            Message message = Message.builder()
                    .setAndroidConfig(AndroidConfig.builder()
                            .setTtl(3600*1000)
                            .setPriority(AndroidConfig.Priority.NORMAL)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle(title)
                                    .setBody(content)
                                    .setIcon("stock_ticker_update")
                                    .setColor("#f45342")
                                    .build())
                            .build())
                    //.setToken(registrationToken)
                    .setTopic(topic)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent message: "+response);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
}


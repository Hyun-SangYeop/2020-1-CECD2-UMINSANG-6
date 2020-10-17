import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
 
/**
 * Created by Jisu on 2015-11-26.
 */
public class javatoc {
    public static void main(String[] args){
    	try
    	{
    		ServerSocket s_socket = new ServerSocket(55000);
    		
    		Socket client_socket = s_socket.accept();
    		
    		//BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
    		//System.out.println("from c-client: " + in.readLine());
    		while(true) {
    		byte[] byteArr = new byte[100];
    		InputStream client_data = client_socket.getInputStream();
    		int readByteCount = client_data.read(byteArr);
    		String fromClient = new String(byteArr, 0, readByteCount, "UTF-8");
    		if(fromClient == "Bye")
                break;
    		System.out.println("from c-client: " + fromClient);
    		
    		String sendDataString = "javatoc";
    		//Scanner sc = new Scanner(System.in);
    		//String sendDataString = sc.nextLine(); 
    		OutputStream server_data = client_socket.getOutputStream();
    		server_data.write(sendDataString.getBytes());
    		}
    		client_socket.close();
    		s_socket.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    }
}
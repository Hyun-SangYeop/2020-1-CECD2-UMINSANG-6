import java.net.*;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.*;

public class Server extends Thread{
	private ServerSocket serverSocket;
	
	public Server(int port) throws IOException{
		serverSocket=new ServerSocket(port);
	}
	
	public void run()
	{
		Socket server;
		String url;
		while(true) {
			try {
				server=serverSocket.accept();
				
				System.out.println("Just connected to " + server.getRemoteSocketAddress());
				
				BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
				
				url=in.readLine();
				System.out.println(url);
				
				
				
				try {
					new Validation().validate(url, "UTF-8");
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TikaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				server.close();
			}
			
			catch(IOException e)
			{
				e.printStackTrace();
				break;
			}
		}
	}
	
//	public static void main(String[] args) {
//		int port=1218;
//		try {
//			Thread t=new Server(port);
//			t.start();
//		}
//		catch(IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	
}
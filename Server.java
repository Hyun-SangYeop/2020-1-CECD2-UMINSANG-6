import java.net.*;
import java.io.*;

public class Server extends Thread{
	private ServerSocket serverSocket;
	
	public Server(int port) throws IOException{
		serverSocket=new ServerSocket(port);
	}
	
	public void run()
	{
		Socket server;
		while(true) {
			try {
				server=serverSocket.accept();
				
				System.out.println("Just connected to " + server.getRemoteSocketAddress());
				
				BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
				
				System.out.println(in.readLine());
				server.close();
			}
			
			catch(IOException e)
			{
				e.printStackTrace();
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		int port=1218;
		try {
			Thread t=new Server(port);
			t.start();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	
}

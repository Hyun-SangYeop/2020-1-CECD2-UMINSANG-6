import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

	public static void main(String[] args) throws UnknownHostException {
		System.out.println("thread start~");
		int port=8733;
		
		InetAddress local = InetAddress.getLocalHost();
		String ip = local.getHostAddress();
		System.out.println(ip);
		
		try {
			Thread t=new Server(port);
			t.start();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

}

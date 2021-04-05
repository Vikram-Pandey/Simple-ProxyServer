import jdk.swing.interop.SwingInterOpUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;


public class ProxyServer {

	//cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
	Map<String, String> cache;

	ServerSocket proxySocket;
	 static String webpage;

	String logFileName = "Proxy.log";

	public static void main(String[] args) {
		new ProxyServer().startServer(8000);
	}

	void startServer(int proxyPort) {

		cache = new ConcurrentHashMap<>();

		// create the directory to store cached files.
		File cacheDir = new File("cached");
		if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
			cacheDir.mkdirs();
		}
		File ProxyLog = new File("ProxyLog");
		if (!ProxyLog.exists() || (ProxyLog.exists() && !ProxyLog.isDirectory())) {
			ProxyLog.mkdirs();
		}


		try{
			proxySocket = new ServerSocket(proxyPort);
			System.out.println("Server is running at port" + 8000);
			while( true){
				Socket clientSocket = proxySocket.accept();
				//clientSocket.setSoTimeout(2000);
				RequestHandler requestHandler=new RequestHandler(clientSocket,this);
				Thread t1 = new Thread(requestHandler);
				t1.run();
			}
		}catch (Exception e){
			System.out.println(e);
		}

	}




	public String getCache(String hashcode) {
		return cache.get(hashcode);
	}

	public void putCache(String hashcode, String fileName) {
		cache.put(hashcode, fileName);
	}

	public synchronized void writeLog(String info)  {

		String ProxyfileName = "ProxyLog/" +logFileName ;


		Logger logger=Logger.getLogger(ProxyfileName);
			try {

				InetAddress localhost = InetAddress.getLocalHost();
				String Ip=localhost.getHostAddress().trim();
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
				File file=new File(ProxyfileName);
				if(!file.exists()){
					file.createNewFile();
				}
				FileWriter fw=new FileWriter(file,true);
				logger.info(timeStamp+" "+Ip+" "+info);
				fw.write(timeStamp+" "+Ip+" "+info+"\n");
				fw.close();
			}
			catch(IOException e){
				e.printStackTrace();
			}

	}

}

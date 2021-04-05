import com.sun.source.tree.CompoundAssignmentTree;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;


// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {


	Socket clientSocket;

	InputStream inFromClient;

	OutputStream outToClient;

	byte[] request = new byte[1024];

	BufferedReader proxyToClientBufferedReader;

	BufferedWriter proxyToClientBufferedWriter;


	private ProxyServer server;


	public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {


		this.clientSocket = clientSocket;


		this.server = proxyServer;

		try {
			clientSocket.setSoTimeout(2000);
			inFromClient = (clientSocket.getInputStream());
			outToClient = clientSocket.getOutputStream();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	@Override

	public void run() {


			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				 String webURL = in.readLine();

				if (webURL.startsWith("GET") && webURL.contains("http://")) {
					System.out.println("working");
					 webURL = webURL.split(" ")[1];
					 System.out.println("sending request to internet " + webURL);
					URL uri=new URL(webURL);
					System.out.println("Host"+uri.getHost());

					String hostname= uri.getHost();
					String path=uri.getPath();
					int port = 80;
					System.out.printf("host name %s, port %d %n", hostname, port);
					String filename;
					if(((filename = server.getCache(uri.toString())) != null)){
						System.out.println("Cached Copy found for : " + uri + "\n");
						System.out.println(filename);
						sendCachedInfoToClient(filename);
					} else {
						proxyServertoClient(webURL.getBytes());
					}
				}
			} catch (Exception e) {
				System.out.println(e);
			}

		}



	public boolean proxyServertoClient(byte[] clientRequest)  {


		FileOutputStream fileWriter = null;
		Socket serverSocket = null;
		InputStream inFromServer;
		OutputStream outToServer;

		// Create Buffered output stream to write to cached copy of file
		String fileName = "cached/" + generateRandomFileName() + ".dat";

		// to handle binary content, byte is used
		byte[] serverReply = new byte[4096];
		int bytesRead=0;
		String requestURL = new String(clientRequest, StandardCharsets.UTF_8);
		System.out.println("requestURL"+requestURL);
		//BufferedWriter FileToCache=null;
		//FileWriter FileToCache=null;
		FileOutputStream fos=null;
		DataOutputStream dos=null;

		//caching
		try{
			// FileToCache=(new FileWriter(fileName));
			 fos = new FileOutputStream (fileName, true);
			 dos = new DataOutputStream (fos);
		}
		catch(Exception e){
			e.printStackTrace();
		}

        try{
			URL uri=new URL(requestURL);
			System.out.println("Host"+uri.getHost());

		String hostname= uri.getHost();
		String path=uri.getPath();
		int port = 80;
		System.out.printf("host name %s, port %d %n", hostname, port);

			//sending the request to the internet
			Socket internetSocket = new Socket(hostname, port);
			String requestStr = "GET " + path + "?" + " HTTP/1.1\r\n"
					+ "Accept: */*\r\n" + "Host: " + hostname + "\r\n"
					+ "Connection: Close\r\n\r\n";
			internetSocket.getOutputStream().write(requestStr.getBytes());
			internetSocket.getOutputStream().flush();
			System.out.println("Request sent to internet " + requestStr);

			while ((bytesRead = internetSocket.getInputStream().read(serverReply)) != -1) {
				System.out.println("Reading response from internet " + bytesRead);
				clientSocket.getOutputStream().write(serverReply, 0, bytesRead);
				 dos.write(serverReply);
			}


			//Add to log
			server.writeLog(hostname);

			//Add to the HashMap

			server.cache.put(uri.toString(),fileName);


			clientSocket.getOutputStream().flush();
			dos.flush();
			dos.close();
			fos.close();
			internetSocket.close();
			clientSocket.close();
			System.out.println("Finished with request");
		}
		catch(IOException e){
			e.printStackTrace();
		}
  return true;
	}



	// Sends the cached content stored in the cache file to the client
	private void sendCachedInfoToClient(String fileName) {

		try {

			byte[] bytes = Files.readAllBytes(Paths.get(fileName));
			String content = new String(bytes);

			// print contents
			System.out.println("Content"+content);
			outToClient.write(bytes);
			outToClient.flush();
			System.out.println("CACHED FILE");

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {

			if (clientSocket != null) {
				clientSocket.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
	}


	// Generates a random file name
	public String generateRandomFileName() {

		String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
		SecureRandom RANDOM = new SecureRandom();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 10; ++i) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

}
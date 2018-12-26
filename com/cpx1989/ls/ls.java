package com.cpx1989.LS;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.cpx1989.LS.client.HandleCommand;
import com.cpx1989.LS.mysql.DBConnection;
import com.cpx1989.LS.wrapper.Metrics;
import com.cpx1989.LS.wrapper.WrapperThread;
import com.cpx1989.LS.wrapper.types.Logs;

public class LS {
	public static boolean home = false;
	public static List<String> logs = new ArrayList<String>();
	public static boolean on = true;
	public static Cipher rc4;
	private static ServerSocket server;
	public static SecretKeySpec rc4Key;
	public static WrapperThread ioserver;
	public static Metrics mets;
	public static Map<String, String> titleids = new HashMap<String, String>();
	private static byte[] key = {0x24, 0x19, 0x14, (byte) 0xba, (byte) 0xca, (byte) 0xd9, (byte) 0xfb, (byte) 0xa2, 0x7a, (byte) 0x81, (byte) 0xc8, 0x34, (byte) 0xce, 0x79, (byte) 0x9e, (byte) 0xfb};
	
	public static void main(String argv[]){
		try {
			Handler handler = new FileHandler("logs/output.log", 15728640, 5);
			Logger.getLogger("").addHandler(handler);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			saveToLogger("Could not attach to output.log! Not logging to file!");
		}
		ioserver = new WrapperThread((home == true) ? "127.0.0.1" : "74.91.112.151", 9897); //74.91.112.86
		mets = new Metrics();
		startServer();
	}
	
	public static void startServer(){
		try {
			rc4 = Cipher.getInstance("RC4");
			rc4Key = new SecretKeySpec(key, "RC4");
			server = new ServerSocket(9689);
			server.setReuseAddress(true);
			saveToLogger("Socket Server listening on port 9689");
			DBConnection.initialize();
			saveToLogger("Connected to database");
			poolTitles();
			listenServer();
		} catch (NoSuchAlgorithmException | IOException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}
	
	public static void listenServer(){
		new Thread(new ListenServer(server)).start();
	}
	
	public static void poolTitles() {
		try {
			if (!titleids.isEmpty()) titleids.clear();
			DBConnection.isDBCon();
			Statement statement;
			statement = DBConnection.dbcon.createStatement();
			statement.setEscapeProcessing(true);
			ResultSet res = statement.executeQuery("SELECT `hexid`, `title` FROM `titleids` WHERE 1");
			while (res.next()) {
				titleids.put(res.getString("hexid"), res.getString("title"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			saveToLogger("Failed to grab Title IDs!");
		}
		
	}
	
	public static void saveToLogger(String string){
		System.out.println(string);
		string = string.replaceAll("\n", "&#10;");
		logs.add(string);
		if(logs.size() > 151){
			List<String> newlog = new ArrayList<String>();
			for (int i = 1; i < 152; i++){
				newlog.add(logs.get(i).toString());
			}
			logs = newlog;
		}
		ioserver.server.getBroadcastOperations().sendEvent("logs", new Logs(0,LS.logs));
	}
}

class ListenServer implements Runnable {
	
	ServerSocket serv;

	public ListenServer(ServerSocket serv) {
		this.serv = serv;
	}

	@Override
	public void run() {
		try {
			LS.saveToLogger("*** Server Started ***");
			while (LS.on) {
				Socket socket = serv.accept();
				new Thread(new HandleCommand(socket)).start();
			}
			LS.saveToLogger("*** Server Stopped ***");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
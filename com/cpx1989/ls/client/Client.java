package com.cpx1989.LS.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cpx1989.LS.LS;
import com.cpx1989.LS.Utils;
import com.cpx1989.LS.enums.TitleIdJson;
import com.cpx1989.LS.mysql.DBConnection;
import com.cpx1989.LS.wrapper.Metrics;
import com.google.gson.Gson;

public class Client {
	
	private Socket sock;
	private double dbver, version;
	private int dbfails, dbdays, dblifetime, titleid, free;
	private String cpukey, hash, dbhash, dbname, status, token, dbtoken, reason, gamertag;
	private DateTime dblast;
	private boolean blacklist = false, indb = false;
	//private byte[] kv = new byte[0x4000];
	DataOutputStream out;
	
	
	public Client(Socket sock, double version, String cpukey, String hash, String token, int titleid, String gamertag/*, byte[] kv*/){
		this.sock = sock;
		this.version = version;
		this.cpukey = cpukey;
		this.hash = hash;
		this.token = token;
		this.titleid = titleid;
		this.gamertag = gamertag;
		//this.kv = kv;
		//if (kv.length == 0x4000) logKV();
		try {
			out = new DataOutputStream(sock.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public Client(Socket sock){
		this.sock = sock;
	}
	
	public boolean isVerCorrect(){
		return this.version == dbver || this.version > dbver;
	}
	
	public boolean isHashCorrect(){
		return (this.hash.equalsIgnoreCase(dbhash) || dbhash.equalsIgnoreCase("0") || hash.equalsIgnoreCase("5AD1E3422472C22C076227D8A97A2DD4"));
	}
	
	public boolean isTokenCorrect(){
		if (dbtoken == null){ return false;}
		return this.dbtoken.equalsIgnoreCase(token);
	}
	
	public boolean isBlacklisted(){
		return blacklist;
	}
	
	public boolean challenge(){
		try {
			Cipher rc4 = LS.rc4;
			SecretKeySpec rc4Key = LS.rc4Key;
			rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			boolean chal = true;
			byte[] padding = new byte[20];
			new Random().nextBytes(padding);
			if (chal && isBlacklisted()){
				chal = false;
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0x80000008);
				buff.position(52);
				buff.putInt(0);
				buff.position(56);
				buff.put(padding);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Client Blacklisted! Reason: "+reason;
				logClient();
			}
			if (chal && !isVerCorrect()){
				chal = false;
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0x20000002);
				buff.position(52);
				buff.putInt(0);
				buff.position(56);
				buff.put(padding);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Client Version Outdated!";
				logClient();
				
			}
			if (chal && !isHashCorrect()){
				chal = false;
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0x50000005);
				buff.position(52);
				buff.putInt(0);
				buff.position(56);
				buff.put(padding);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Client using tampered plugin!";
				logClient();
				addFail(cpukey);
			}
			if (version > 8 && chal && !isTokenCorrect()){
				chal = false;
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0x70000007);
				buff.position(52);
				buff.putInt(0);
				buff.position(56);
				buff.put(padding);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Client connected with different token!";
				logClient();
			}
			return chal;
		} catch (InvalidKeyException | IOException e) {
			e.printStackTrace();
			LS.saveToLogger("Error in console: Challenge!");
		}
		return false;
	}
	
	public void handleResponse(){
		try {
			Cipher rc4 = LS.rc4;
			SecretKeySpec rc4Key = LS.rc4Key;
			rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			DataOutputStream out = new DataOutputStream(sock.getOutputStream()); 
			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
			DateTime now = new DateTime(System.currentTimeMillis());
			boolean newDay = false;
			byte[] padding = new byte[20];
			new Random().nextBytes(padding);
			if (dblast == null && dbname == null && free != 1){
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0x10000001);
				buff.position(52);
				buff.putInt(0);
				buff.position(56);
				buff.put(padding); 
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Client not registered with LS!";
				logClient();
			} else if (dblifetime == 1){
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0xA5F00000); //Status
				buff.position(4);
				buff.putInt(0); // Days
				buff.position(8);
				buff.put(dbname.getBytes());
				buff.position(28);
				buff.put(getDifference(new DateTime(), dblast).getBytes()); // Date
				buff.position(52);
				buff.putInt(0);
				buff.position(56);
				buff.put(padding);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Authenticated. Lifetime <3";
				logClient();
			} else if ((now.isBefore(dblast) || dbdays > 0) && free != 1){
				if (now.isAfter(dblast)){
					newDay = true;
					dblast = new DateTime().plusDays(1);
					String dbdate = fmt.print(dblast);
					dbdays--;
					Statement statement = DBConnection.dbcon.createStatement();
					statement.executeUpdate("UPDATE `clients` SET `lastDay` = '"+dbdate+"',`days` = '"+(dbdays)+"' WHERE `cpukey` = '"+cpukey+"'");
				}
				if (now.isAfter(dblast.minusMinutes(5))){
					ByteBuffer buff = ByteBuffer.allocate(76);
					buff.putInt(0xA5E00000);// Status
					buff.position(4);
					buff.putInt(dbdays); // Days
					buff.position(8);
					buff.put(dbname.getBytes());
					buff.position(28);
					buff.put(getDifference(new DateTime(), dblast).getBytes()); // Date
					buff.position(52);
					buff.putInt(0);
					buff.position(56);
					buff.put(padding);
					byte[] done = rc4.update(buff.array());
					out.write(done);
					status = "Authenticated. Expires Soon! Days Remaining: "+dbdays;
					logClient();
				} else {
					ByteBuffer buff = ByteBuffer.allocate(76);
					buff.putInt((newDay == true) ? 0xA5D00000 : 0xA5000000);
					buff.position(4);
					buff.putInt(dbdays); // Days
					buff.position(8);
					buff.put(dbname.getBytes());
					buff.position(28);
					buff.put(getDifference(new DateTime(), dblast).getBytes()); // Date
					buff.position(52);
					buff.putInt(0);
					buff.position(56);
					buff.put(padding); //Padding
					byte[] done = rc4.update(buff.array());
					out.write(done);
					status = "Authenticated. Days Remaining: "+dbdays;
					logClient();
				}
		    } else if (free == 1){
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0xA500000F); //Status
				buff.position(8);
				buff.position(56);
				buff.put(padding); //Padding
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Authenticated. Free Time!";
				if (dbname == null) dbname = "Free Client";
				logClient();
			} else {
				ByteBuffer buff = ByteBuffer.allocate(76);
				buff.putInt(0x30000003);
				buff.position(52);
				buff.putInt(0);
				buff.position(56);
				buff.put(padding); //Padding
				byte[] done = rc4.update(buff.array());
				out.write(done);
				status = "Expired :(";
				logClient();
		    }
		} catch (IOException | SQLException | InvalidKeyException e) {
			e.printStackTrace();
			LS.saveToLogger("Error in console: HandleResponse!");
		}
	}
	
	public void logClient(){
		String titid = parseTitle(Integer.toHexString(titleid));
		String print = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n" + 
					   "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n" +
				       "      - CPUKey: "+ cpukey + "\n" + 
				       "      - Name: "+ dbname + "\n" + 
					   "      - Hash: "+hash+"\n" + 
					   "      - Token: "+token+"\n" + 
				       "      - Version: "+ version + "\n" + 
					   "      - Title ID: " +titid + "\n" + 
					   "      - Gamertag: " + gamertag + "\n" + 
				       "      - Status: " + status + "\n" + 
					   "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
		LS.saveToLogger(print);
		if (indb) logToDB(cpukey, hash, version, sock, titid, gamertag);
		Metrics.logClient(cpukey);
	}
	
	private String parseTitle(String hexString) {
		if (LS.titleids.containsKey(hexString)){
			return hexString.toUpperCase() + " ("+LS.titleids.get(hexString)+")";
		} else {
			try {
				String json = Utils.readURLFirefox("http://xblLS.com/includes/titleid.php?titleid="+hexString);
				Gson gson = new Gson();        
			    TitleIdJson titleid = gson.fromJson(json, TitleIdJson.class);
			    String print = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n" + 
						   "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n" +
						   "   	  - New Game: " + titleid.Name + "\n" +
					       "      - CPUKey: "+ cpukey + "\n" + 
					       "      - Name: "+ dbname + "\n" + 
						   "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
			    
			    System.out.println(print);
			    DBConnection.isDBCon();
				Statement statement;
				statement = DBConnection.dbcon.createStatement();
				statement.setEscapeProcessing(true);
				statement.execute("INSERT INTO `titleids`(`hexid`, `title`) VALUES ('"+hexString+"','"+titleid.Name+"')");
			    LS.poolTitles();
			    return hexString.toUpperCase() + " ("+titleid.Name+")";
			} catch (Exception e) {
				e.printStackTrace();
				return hexString.toUpperCase();
			}
		}
	}
	
	public String getDifference(DateTime start, DateTime end){
		String time = "";
		Hours hours = Hours.hoursBetween(start, end);
		if (hours.getHours() > 1) time += hours.getHours() + " hours ";
		else if (hours.getHours() == 1) time += hours.getHours() + " hour ";
		Minutes mins = Minutes.minutesBetween(start, end);
		int mins1 = mins.getMinutes() % 60;
		if (mins1 > 1) time += mins1 + " minutes";
		else if (mins1 == 1) time += mins1 + " minute";
		return time;
	}

	
	public void finish(){
		try {
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			LS.saveToLogger("Error in console: Finalize!");
		}
	}
	
	/*public void logKV(){
		try {
			PreparedStatement statement = DBConnection.dbcon.prepareStatement("INSERT INTO `clients`(`KV`) VALUES (?)");
			Blob blob = new SerialBlob(kv);
			statement.setBlob(1, blob);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}*/
	
	public void logToDB(String cpukey, String xexhash, double version, Socket socket, String titid, String gamertag){
		try {
			DBConnection.isDBCon();
			Statement statement;
			statement = DBConnection.dbcon.createStatement();
			statement.executeUpdate("UPDATE `clients` SET `titleid`='"+titid+"',`gamertag`='"+gamertag+"',`xexhash`='"+xexhash+"',`version`='"+version+"',`ip`='"+socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]+/*',`kv`='+con.escape(kv)+*/"' WHERE `cpukey` = '"+cpukey+"'");
		} catch (SQLException e) {
			e.printStackTrace();
			LS.saveToLogger("Error in console: LogToDB!");
		}
	}
	
	public void poolDB(){
		try {
			DBConnection.isDBCon();
			Statement statement;
			statement = DBConnection.dbcon.createStatement();
			statement.setEscapeProcessing(true);
			ResultSet res = statement.executeQuery("SELECT `name`, `days`, `lastDay`, `fails`, `lifetime` FROM `clients` WHERE `cpukey` = '"+cpukey+"' LIMIT 1");
			while (res.next()) {
				dbname = res.getString("name");
				dbdays = res.getInt("days");
				dblast = new DateTime(res.getTimestamp("lastDay"));
				dbfails = res.getInt("fails");
				dblifetime = res.getInt("lifetime");
				indb = true;
			}
			res = statement.executeQuery("SELECT `session` FROM `sessions` WHERE `cpukey` = '"+cpukey+"' LIMIT 1");
			while (res.next()) {
				dbtoken = res.getString("session");
			}
			res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'version' LIMIT 1");
			while (res.next()) {
				dbver = res.getDouble("value");
			}
			res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'xexhash' LIMIT 1");
			while (res.next()) {
				dbhash = res.getString("value");
			}
			res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'free' LIMIT 1");
			while (res.next()) {
				free = res.getInt("value");
			}
			res = statement.executeQuery("SELECT `reason` FROM `blacklist` WHERE `cpukey` = '"+cpukey+"' LIMIT 1");
			if(res.next()) {
				blacklist = true;
				reason = res.getString("reason");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			LS.saveToLogger("Error in console: PoolDB!");
		}
		
	}
	
	public void addFail(String cpukey){
		try {
			Statement statement = DBConnection.dbcon.createStatement();
			dbfails += 1;
			statement.executeUpdate("UPDATE `clients` SET `fails` = '"+dbfails+"' WHERE `cpukey` = '"+cpukey+"'");
		} catch (SQLException e) {
			e.printStackTrace();
			LS.saveToLogger("Error in console: AddFail!");
		}
	}

}

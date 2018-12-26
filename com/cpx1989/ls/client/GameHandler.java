package com.cpx1989.LS.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.joda.time.DateTime;

import com.cpx1989.LS.LS;
import com.cpx1989.LS.Utils;
import com.cpx1989.LS.enums.MODULE_STATUS;
import com.cpx1989.LS.mysql.DBConnection;

public class GameHandler {
	
	private Socket sock;
	private int dbdays, dblifetime, dbfails, game, free;
	Map<String, Integer> games = new HashMap<String, Integer>();
	private String cpukey, hash, dbhash, status, token, dbtoken, dbname;
	private DateTime dblast;
	private boolean blacklist = false, indb = false;
	DataOutputStream out;

	public GameHandler(Socket sock, int game, String cpukey, String hash, String token) {
		this.sock = sock;
		this.cpukey = cpukey;
		this.hash = hash;
		this.token = token;
		this.game = game;
		for (String s : Arrays.asList("bo3", "bo3z", "bo3_pub", "bo3z_pub", "aw", "aw_pub", "ghost", "ghost_pub", "bo2", "bo2_pub", "mw3", "bo1", "mw2", "waw", "cod4", "destiny", "msp")){
			games.put(s, 0);
		}
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
	
	public boolean passedChallenge(){
		return (isHashCorrect() && isTokenCorrect() && isAuthed() && !isBlacklisted());
	}
	
	public boolean isAuthed(){
		DateTime now = new DateTime(System.currentTimeMillis());
		if (dblast == null && dbname == null && free != 1){
			return false;
		} else if (dblifetime == 1){
			return true;
		} else if ((now.isBefore(dblast) || dbdays > 0) && free != 1){
			return true;
		} else if (free == 1){
			if (dbname == null) dbname = "Free Client";
			return true;
		} else {
			return false;
		}
	}
	
	public void logClient(){
		String print = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n" + 
					   "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n" +
				       "      - CPUKey: "+ cpukey + "\n" + 
				       "      - Name: "+ dbname + "\n" + 
					   "      - Hash: "+hash+"\n" + 
					   "      - Token: "+token+"\n" + 
				       "      - Status: " + status + "\n" + 
					   "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
		LS.saveToLogger(print);
		if (indb) logToDB(cpukey, hash, sock);
	}
	
	public void logToDB(String cpukey, String xexhash, Socket socket){
		try {
			DBConnection.isDBCon();
			Statement statement;
			statement = DBConnection.dbcon.createStatement();
			statement.executeUpdate("UPDATE `clients` SET `xexhash`='"+xexhash+"',`ip`='"+socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]+/*',`kv`='+con.escape(kv)+*/"' WHERE `cpukey` = '"+cpukey+"'");
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
			ResultSet res = statement.executeQuery("SELECT `name`, `days`, `lastDay`, `fails`, `lifetime`, `bo3`, `bo3z`, `bo3_pub`, `bo3z_pub`, `aw`, `aw_pub`, `ghost`, `ghost_pub`, `bo2`, `bo2_pub`, `mw3`, `bo1`, `mw2`, `waw`, `cod4`, `destiny`, `msp` FROM `clients` WHERE `cpukey` = '"+cpukey+"' LIMIT 1");
			while (res.next()) {
				dbname = res.getString("name");
				dbdays = res.getInt("days");
				dblast = new DateTime(res.getTimestamp("lastDay"));
				dbfails = res.getInt("fails");
				dblifetime = res.getInt("lifetime");
				indb = true;
				for (String s : Arrays.asList("bo3", "bo3z", "bo3_pub", "bo3z_pub", "aw", "aw_pub", "ghost", "ghost_pub", "bo2", "bo2_pub", "mw3", "bo1", "mw2", "waw", "cod4", "destiny", "msp")){
					games.put(s, res.getInt(s));
				}
			}
			res = statement.executeQuery("SELECT `session` FROM `sessions` WHERE `cpukey` = '"+cpukey+"' LIMIT 1");
			while (res.next()) {
				dbtoken = res.getString("session");
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
			}
			if (free == 1){
				for (String s : Arrays.asList("bo3", "bo3z", "bo3_pub", "bo3z_pub", "aw", "aw_pub", "ghost", "ghost_pub", "bo2", "bo2_pub", "mw3", "bo1", "mw2", "waw", "cod4", "destiny", "msp")){
					games.put(s, 1);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void addFail(String cpukey){
		try {
			Statement statement = DBConnection.dbcon.createStatement();
			dbfails += 1;
			statement.executeUpdate("UPDATE `clients` SET `fails` = '"+dbfails+"' WHERE `cpukey` = '"+cpukey+"'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void start() throws InvalidKeyException, IOException{
		poolDB();
		Cipher rc4 = LS.rc4;
		SecretKeySpec rc4Key = LS.rc4Key;
		rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
		DataOutputStream out = new DataOutputStream(sock.getOutputStream()); 
		if (passedChallenge()){
			switch (game){
			case 1:
				status = "Sent Black Ops 2 Bypass!";
				sendFile("patches/bo2.bin", games.get("bo2"));
				break;
			case 2:
				status = "Sent Black Ops 2 Public Cheater!";
				sendFile("patches/bo2pub.bin", games.get("bo2_pub"));
				break;
			case 3:
				status = "Sent Ghost Bypass!";
				sendFile("patches/ghost.bin", games.get("ghost"));
				break;
			case 4:
				status = "Sent Ghost Public Cheater!";
				sendFile("patches/ghostpub.bin", games.get("ghost_pub"));
				break;
			case 5:
				status = "Sent Advanced Warfare Bypass!";
				sendFile("patches/aw.bin", games.get("aw"));
				break;
			case 6:
				status = "Sent Advanced Warfare Public Cheater!";
				sendFile("patches/awpub.bin", games.get("aw_pub"));
				break;
			case 7:
				status = "Sent Destiny Bypass!";
				sendFile("patches/dest.bin", games.get("destiny"));
				break;
			case 8:
				status = "Sent Modern Warfare 3 Public Cheater!";
				sendFile("patches/mw3.bin", games.get("mw3"));
				break;
			case 9:
				status = "Sent Black Ops 1 Public Cheater!";
				sendFile("patches/bo1.bin", games.get("bo1"));
				break;
			case 10:
				status = "Sent Modern Warfare 2 Public Cheater!";
				sendFile("patches/mw2.bin", games.get("mw2"));
				break;
			case 11:
				status = "Sent World At War Public Cheater!";
				sendFile("patches/waw.bin", games.get("waw"));
				break;
			case 12:
				status = "Sent Modern Warfare Public Cheater!";
				sendFile("patches/cod4.bin", games.get("cod4"));
				break;
			case 13:
				status = "Sent Microsoft Point Spoof (UNDO)!";
				sendFile("patches/msphud1.bin", games.get("msp"));
				break;
			case 14:
				status = "Sent Microsoft Point Spoof (HUD)!";
				sendFile("patches/msphud.bin", games.get("msp"));
				break;
			case 15:
				status = "Sent Black Ops 3 Bypass!";
				sendFile("patches/bo3.bin", games.get("bo3"));
				break;
			case 16:
				status = "Sent Black Ops 3 Public Cheater!";
				sendFile("patches/bo3pub.bin", games.get("bo3_pub"));
				break;
			case 17:
				status = "Sent Black Ops 3 Zombies Bypass!";
				sendFile("patches/bo3z.bin", games.get("bo3z"));
				break;
			case 18:
				status = "Sent Black Ops 3 Zombies Public Cheater!";
				sendFile("patches/bo3zpub.bin", games.get("bo3z_pub"));
				break;
			}
		} else {
			ByteBuffer buff = ByteBuffer.allocate(4);
			buff.putInt(0);
			byte[] done = rc4.update(buff.array());
			out.write(done);
			out.flush();
			out.close();
			status = "User failed to pass challenge!";
			logClient();
		}
	}
	
	public void sendFile(String path, int stat) throws IOException{
		Cipher rc4 = LS.rc4;
		SecretKeySpec rc4Key = LS.rc4Key;
		File file = new File(path);
		DataOutputStream out = new DataOutputStream(sock.getOutputStream());
		if (stat != 1){
			if (stat == 2){
				status = status.replaceAll("Sent", "");
				status = status.replaceAll("!", "");
				status += " turned off by Server";
				out.writeInt(MODULE_STATUS.SRV_OFF.getValue());
				out.flush();
				out.close();
			} else if (stat == 0){
				status = status.replaceAll("Sent", "");
				status = status.replaceAll("!", "");
				status += " turned off by Client";
				out.writeInt(MODULE_STATUS.USER_OFF.getValue());
				out.flush();
				out.close();
			}
		}
		logClient();
		try (FileInputStream fis = new FileInputStream(file)) {
			int size = fis.available();
			byte[] data = new byte[size];
			fis.read(data);
			rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			ByteBuffer buff = ByteBuffer.allocate(4);
			buff.putInt(size);
			byte[] doneSize = rc4.update(buff.array());
			rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			byte[] donePatch = rc4.update(data);
			out.write(doneSize);
			out.write(donePatch);
			out.flush();
			out.close();
		} catch (IOException | InvalidKeyException e) {
			status = "An error occurred sending patch data!";
			logClient();
		}
	}

}

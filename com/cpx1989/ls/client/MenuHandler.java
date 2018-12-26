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

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.joda.time.DateTime;

import com.cpx1989.LS.LS;
import com.cpx1989.LS.Utils;
import com.cpx1989.LS.mysql.DBConnection;

public class MenuHandler {
	
	Socket sock;
	String cpukey, hash, token, status, dbhash, dbtoken, dbname;
	DateTime dblast;
	int free = 0, dblifetime = 0, dbdays = 0, dbfails = 0;
	boolean blacklist = false, indb = false;

	public MenuHandler(Socket sock, String cpukey, String hash, String token){
		this.sock = sock;
		this.cpukey = cpukey;
		this.hash = hash;
		this.token = token;
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
			sendMenu();
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
	
	public void sendMenu() throws IOException{
		Cipher rc4 = LS.rc4;
		SecretKeySpec rc4Key = LS.rc4Key;
		File file = new File("xex/Ponies.xex");
		DataOutputStream out = new DataOutputStream(sock.getOutputStream());
		try (FileInputStream fis = new FileInputStream(file)) {
			int size = fis.available();
			byte[] data = new byte[size];
			fis.read(data);
			rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			ByteBuffer buff = ByteBuffer.allocate(4);
			buff.putInt(size);
			byte[] doneSize = rc4.update(buff.array());
			rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			byte[] doneXex = rc4.update(data);
			out.write(doneSize);
			for (int i = 0; i * 2048 < size; i++) {
				byte[] tmp1 = Arrays.copyOfRange(doneXex, i * 2048, (i * 2048) + 2048);
				out.write(tmp1);
				Thread.sleep(100);
			}
			out.flush();
			out.close();
			status = "Sent Ponies.xex to client!";
			logClient();
		} catch (IOException | InvalidKeyException | InterruptedException e) {
			status = "An error occurred sending Ponies.xex!";
			logClient();
			e.printStackTrace();
		}
	}
	
}

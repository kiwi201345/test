package com.cpx1989.LS.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.joda.time.DateTime;

import com.cpx1989.LS.LS;
import com.cpx1989.LS.Utils;
import com.cpx1989.LS.mysql.DBConnection;

public class XamChallenge {
	
	private Socket sock;
	private byte[] salt, ecc, hv_rand_bytes;
	private int dbdays, dblifetime, dbfails, clr, fcrt, type1kv, free;
	private String cpukey, hash, dbhash, status, token, dbtoken, dbname, chalhash, ecchash, hv_rand;
	private DateTime dblast;
	private boolean blacklist = false, random = false, indb = false;
	DataOutputStream out;
	
	public XamChallenge(Socket sock, int clr, int fcrt, int type1kv, byte[] salt, byte[] ecc, String cpukey, String hash, String token){
		this.sock = sock;
		this.cpukey = cpukey;
		this.hash = hash;
		this.token = token;
		this.clr = clr;
		this.fcrt = fcrt;
		this.type1kv = type1kv;
		this.salt = salt;
		this.ecc = ecc;
	}
	
	public XamChallenge(Socket sock, int clr, int fcrt, int type1kv, byte[] salt, String cpukey, String hash, String token){
		this.sock = sock;
		this.cpukey = cpukey;
		this.hash = hash;
		this.token = token;
		this.clr = clr;
		this.fcrt = fcrt;
		this.type1kv = type1kv;
		this.salt = salt;
		random = true;
	}
	
	public boolean isHashCorrect(){
		if (dbhash != null) return true;
		return true; //(this.hash.equalsIgnoreCase(dbhash) || dbhash.equalsIgnoreCase("0") || hash.equalsIgnoreCase("5AD1E3422472C22C076227D8A97A2DD4"));
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
				       "      - Chal: "+chalhash+"\n" +
				       "      - Ecc: "+ecchash+"\n" +
				       "      - HV_D: "+hv_rand.substring(0, 32)+"\n" +
					   "      - Token: "+token+"\n" + 
				       "      - Status: " + status + "\n" + 
					   "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
		LS.saveToLogger(print);
		if (indb) logToDB(cpukey, hash, hv_rand, sock);
	}
	
	public void logToDB(String cpukey, String xexhash, String hvrand, Socket socket){
		try {
			DBConnection.isDBCon();
			Statement statement;
			statement = DBConnection.dbcon.createStatement();
			statement.executeUpdate("UPDATE `clients` SET `xexhash`='"+xexhash+"',`ip`='"+socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]+/*',`kv`='+con.escape(kv)+*/"' WHERE `cpukey` = '"+cpukey+"'");
			statement.executeUpdate("UPDATE `sessions` SET `rand_hv`='"+hvrand+"' WHERE `cpukey` = '"+cpukey+"'");
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
			res = statement.executeQuery("SELECT `session`,`rand_hv` FROM `sessions` WHERE `cpukey` = '"+cpukey+"' LIMIT 1");
			while (res.next()) {
				dbtoken = res.getString("session");
				hv_rand = res.getNString("rand_hv");
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
			File file = new File("patches/HV.bin");
			File file1 = new File("patches/cache.bin");
			byte[] hv = new byte[0], cache = new byte[0];
			try (FileInputStream fis = new FileInputStream(file)) {
				int size = fis.available();
				hv = new byte[size];
				fis.read(hv);
				fis.close();
			} catch (IOException e){
				e.printStackTrace();
				ByteBuffer buff = ByteBuffer.allocate(256);
				buff.putInt(0x40000004);
				rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				out.flush();
				out.close();
				status = "Failed to read HV.bin!";
				logClient();
			}
			try (FileInputStream fis1 = new FileInputStream(file1)) {
				int size = fis1.available();
				cache = new byte[size];
				fis1.read(cache);
				fis1.close();
			} catch (IOException e){
				e.printStackTrace();
				ByteBuffer buff = ByteBuffer.allocate(256);
				buff.putInt(0x40000004);
				rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				out.flush();
				out.close();
				status = "Failed to read cache.bin!";
				logClient();
			}
			try {
				//LS.saveToLogger("Before call: " + hv_rand);
				if (clr == 0){ //first time boot
					hv_rand_bytes = new byte[0x80]; //randomize first boot
					Random rand = new Random();
					rand.nextBytes(hv_rand_bytes);
					hv_rand = Utils.bytesToHex(hv_rand_bytes);
					//LS.saveToLogger("clr == 0 | " + hv_rand);
				} else { //not first boot
					if (hv_rand != null && hv_rand.length() == 256){ // key is valid
						hv_rand_bytes = Utils.hexToBytes(hv_rand);
						//LS.saveToLogger("clr != 0 | hv_rand != null | hv_rand.length() == 256 | " + hv_rand);
					} else { //key is not valid and make new
						hv_rand_bytes = new byte[0x80];
						Random rand = new Random();
						rand.nextBytes(hv_rand_bytes);
						hv_rand = Utils.bytesToHex(hv_rand_bytes);
						//LS.saveToLogger("clr != 0 | hv_rand == '' | " + hv_rand);
					}
				}
				
				int patch1 = 0x23289d3, patch2 = 0xd83e, patch3 = 0x304000D;
				if (type1kv == 1){
					patch3 = 0x10b0400;
					patch2 = patch2 & -33;
				}
				patch1 = (clr == 1) ? patch1 | 0x10000 : patch1;
				patch1 = (fcrt == 1) ? patch1 | 0x1000000 : patch1;
				ByteBuffer challenge = ByteBuffer.allocate(256);
				//Fill with HV Data first!
				challenge.position(0x28);
				challenge.put(hv, 0, 8);
				challenge.position(0x30);
				challenge.put(hv, 0x10, 8);
				challenge.position(0x38);
				challenge.put(hv, 0x30, 4);
				challenge.position(0x3C);
				challenge.put(hv, 0x74, 4);
				challenge.position(0x40);
				challenge.put(new byte[]{0, 0, 0, 2, 0, 0, 0, 0});
				challenge.position(0x48);
				challenge.put(new byte[]{0, 0, 1, 0, 0, 0, 0, 0});
				//This is for the ecc hash
				challenge.position(0x50);
				if (random){
					byte[] r = new byte[128];
					Random rand = new Random();
					rand.nextBytes(r);
					ecchash = Utils.bytesToHex(r).substring(0, 40) + " (r)";
					challenge.put(r, 0, 0x14);
				} else {
					challenge.put(SetupECCHash(ecc, hv, cache, hv_rand_bytes), 0, 0x14);
				}
				//Now fill challenge with the patch data!
				challenge.position(0x2C);
				challenge.putInt(patch2);
				challenge.position(0x38);
				challenge.putInt(patch1);
				challenge.position(0x3C);
				challenge.putInt(patch3);
				challenge.position(0xEC);
				challenge.put(SetupHvHash(salt, hv), 0, 0x14);
				//Add in the hv_random data
				challenge.position(0x78);
				challenge.put(hv_rand_bytes);
				//Now fill in the HVEX as hvhash would overwrite
				challenge.position(0xF8);
				challenge.put(new byte[]{1, (byte) 0xB7});
				//Now fill with important data for client!
				challenge.position(0);
				challenge.putInt(0x10000001);
				try {
					File file2= new File("xamresp.bin");
					FileOutputStream fis2 = new FileOutputStream(file2);
					fis2.write(challenge.array());
					fis2.flush();
					fis2.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			    rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
				byte[] done = rc4.update(challenge.array());
				out.write(done);
				out.flush();
				out.close();
				status = "Sent Challenge buffer to client!";
				logClient();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException e){
				ByteBuffer buff = ByteBuffer.allocate(256);
				buff.putInt(0x40000004);
				rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
				byte[] done = rc4.update(buff.array());
				out.write(done);
				out.flush();
				out.close();
				status = "Failed to read challenge dump!";
				logClient();
			}
		} else {
			ByteBuffer buff = ByteBuffer.allocate(256);
			buff.putInt(0x40000004);
		    rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			byte[] done = rc4.update(buff.array());
			out.write(done);
			out.flush();
			out.close();
			status = "User failed to pass challenge!";
			logClient();
		}
	}
	
	public byte[] SetupHvHash(byte[] salt, byte[] hv) throws NoSuchAlgorithmException, IOException {
	    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
	    sha1.update(salt, 0x00, 0x10);
	    sha1.update(hv, 0x34, 0x40);
	    sha1.update(hv, 0x78, 0xf88);
	    sha1.update(hv, 0x100c0, 0x40);
	    sha1.update(hv, 0x10350, 0xdf0);
	    sha1.update(hv, 0x16d20, 0x2e0);
	    sha1.update(hv, 0x20000, 0xffc);
	    sha1.update(hv, 0x30000, 0xffc);
	    byte[] ret = sha1.digest();
		chalhash = Utils.bytesToHex(ret).substring(0, 40);
	    return ret;
    }
	
	public byte[] SetupECCHash(byte[] salt, byte[] hv, byte[] cache, byte[] hvrand) throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException {
		Cipher hv_rc4 = Cipher.getInstance("RC4");
		SecretKeySpec hv_rc4Key = new SecretKeySpec(hvrand, "RC4");
		hv_rc4.init(Cipher.ENCRYPT_MODE, hv_rc4Key);
		cache = hv_rc4.update(cache);
	    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
	    sha1.update(salt, 0x00, 0x02);
	    sha1.update(hv, 0x34, 0x0C);
	    sha1.update(hv, 0x40, 0x30);
	    sha1.update(hv, 0x70, 0x04);
	    sha1.update(hv, 0x78, 0x08);
	    sha1.update(cache, 0x02, 0x3FE);
	    sha1.update(hv, 0x100c0, 0x40);
	    sha1.update(hv, 0x10350, 0x30);
	    sha1.update(cache, 0x40E, 0x176);
	    sha1.update(hv, 0x16100, 0x40);
	    sha1.update(hv, 0x16d20, 0x60);
	    sha1.update(cache, 0x5b6, 0x24A);
	    sha1.update(cache, 0x800, 0x400);
	    sha1.update(cache, 0xC00, 0x400);
	    byte[] ret = sha1.digest();
		ecchash = Utils.bytesToHex(ret).substring(0, 40) + " (s)";
	    return ret;
	}

}

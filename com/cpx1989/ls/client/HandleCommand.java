package com.cpx1989.LS.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.cpx1989.LS.LS;
import com.cpx1989.LS.Utils;
import com.cpx1989.LS.mysql.DBConnection;

public class HandleCommand implements Runnable {

	Socket sock;

	public HandleCommand(Socket socket) {
		this.sock = socket;
	}

	@Override
	public void run() {
		try {
			Cipher rc4 = LS.rc4;
			SecretKeySpec rc4Key = LS.rc4Key;
		    rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
			DataInputStream in;
			in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
			int cmd, len, titleid, major, minor;
			String cpukey, hash, token, gamertag = null;
			//byte[] kv;
			cmd = in.readInt();
			len = in.readInt();
			switch (cmd) {
			case 1:
				byte[] b = new byte[len];
				in.readFully(b);
				byte[] dec = rc4.update(b);
				DataInputStream fin = new DataInputStream(new ByteArrayInputStream(dec));
				/*try {
					File file = new File("request.bin");
					FileOutputStream fis = new FileOutputStream(file);
					fis.write(new byte[]{0x00, 0x00, 0x00});
					fis.write(cmd);
					fis.write(new byte[]{0x00, 0x00, 0x00});
					fis.write(len);
					fis.write(dec);
					fis.flush();
					fis.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/
				byte[] tmp = new byte[16];
				
				major = fin.readUnsignedShort();
				minor = fin.readUnsignedShort();
				double version = Double.parseDouble(major + "." + minor);
				//int min = fin.readShort();
				titleid = fin.readInt();
				fin.read(tmp);
				gamertag = new String(Utils.trimZeros(tmp), StandardCharsets.UTF_8);
				if (!gamertag.matches("[a-zA-Z0-9 ]+")) gamertag = "n/a";
				fin.read(tmp);
				cpukey = Utils.bytesToHex(tmp);
				fin.read(tmp);
				hash = Utils.bytesToHex(tmp);
				//kv = new byte[16384];
				//fin.read(kv);
				byte[] tmp3 = new byte[8];
				fin.read(tmp3);
				token = Utils.bytesToHex(tmp3);
				fin.close();
				Client cli = new Client(sock, version, cpukey, hash, token, titleid, gamertag);
				cli.poolDB();
				if (cli.challenge()) { cli.handleResponse(); }
				cli.finish();
				break;
			case 2:
				byte[] b1 = new byte[len];
				in.readFully(b1);
				byte[] dec1 = rc4.update(b1);
				DataInputStream fin1 = new DataInputStream(new ByteArrayInputStream(dec1));
				byte[] tmp2 = new byte[16];
				fin1.read(tmp2);
				cpukey = Utils.bytesToHex(tmp2);
				ByteBuffer buff1 = ByteBuffer.allocate(28);
				byte[] padding = new byte[8];
				new Random().nextBytes(padding);
				buff1.position(0);
				buff1.put(padding);
				try {
					DBConnection.isDBCon();
					Statement statement = DBConnection.dbcon.createStatement();
					statement.setEscapeProcessing(true);
					ResultSet res = statement.executeQuery("SELECT `session` FROM `sessions` WHERE `cpukey` = '"+cpukey+"' LIMIT 1");
					if(!res.isBeforeFirst()){
						statement.execute("INSERT INTO `sessions`(`session`,`ip`,`cpukey`, `rand_hv`) VALUES ('"+Utils.bytesToHex(padding)+"','"+sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]+"','"+cpukey+"','')");
						
					} else {
						statement.executeUpdate("UPDATE `sessions` SET `session`='"+Utils.bytesToHex(padding)+"',`ip`='"+sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]+"' WHERE `cpukey` = '"+cpukey+"'");
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				byte[] padding1 = new byte[20];
				new Random().nextBytes(padding1);
				buff1.position(8);
				buff1.put(padding1);
				rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
				byte[] done = rc4.update(buff1.array());
				String print1 = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n"
						 + "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n"
						 + "      - CPUKey: "+ cpukey + "\n"
						 + "      - New Session: "+Utils.bytesToHex(padding)+"!\n"
						 + "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
				LS.saveToLogger(print1);
				DataOutputStream out1 = new DataOutputStream(sock.getOutputStream());
				out1.write(done);
				out1.flush();
				out1.close();
				break;
			case 3:
				byte[] b3 = new byte[len];
				in.readFully(b3);
				byte[] dec4 = rc4.update(b3);
				DataInputStream fin4 = new DataInputStream(new ByteArrayInputStream(dec4));
				byte[] tmp4 = new byte[16];
				fin4.read(tmp4);
				cpukey = Utils.bytesToHex(tmp4);
				File file = new File("xex/LS.xex");
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
					DataOutputStream out = new DataOutputStream(sock.getOutputStream());
					String print = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n"
								 + "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n"
								 + "      - CPUKey: "+ cpukey + "\n"
								 + "      - Status: Sending update to client!\n"
								 + "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
					LS.saveToLogger(print);
					out.write(doneSize);
					for (int i = 0; i * 2048 < size; i++) {
						byte[] tmp1 = Arrays.copyOfRange(doneXex, i * 2048, (i * 2048) + 2048);
						out.write(tmp1);
						//if (i == 0) Edge.saveToLogger(Utils.bytesToHex(tmp1));
						Thread.sleep(100);
					}
					out.flush();
					out.close();
				} catch (IOException | InvalidKeyException | InterruptedException e) {
					e.printStackTrace();
				}
				break;
			case 4:
				//READ ALL DATA TO BUFFER
				byte[] b2 = new byte[len];
				in.readFully(b2);
				byte[] dec2 = rc4.update(b2);
				DataInputStream fin2 = new DataInputStream(new ByteArrayInputStream(dec2));
				//READ CPUKEY
				byte[] tmp5 = new byte[16];
				fin2.read(tmp5);
				cpukey = Utils.bytesToHex(tmp5);
				//READ TOKEN TO BE REDEEMED
				byte[] tmp14 = new byte[14];
				fin2.read(tmp14);
				String tokenr = new String(tmp14, StandardCharsets.UTF_8);
				//READ CONFIRM
				int confirm = fin2.readInt();
				//PROCESS OUTPUT
				ByteBuffer buff6 = ByteBuffer.allocate(28);
				buff6.position(0);
				if (Utils.checkBrute(cpukey)){
					buff6.putInt(223829);
					String print3 = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n"
							 + "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n"
							 + "      - CPUKey: "+ cpukey + "\n"
							 + "      - Token: "+ tokenr + "\n"
							 + "      - Status: Redeem token while brute!\n"
							 + "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
					LS.saveToLogger(print3);
				} else if (confirm == 0){
					buff6.putInt(Utils.getDays(tokenr));
					String print3 = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n"
							 + "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n"
							 + "      - CPUKey: "+ cpukey + "\n"
							 + "      - Token: "+ tokenr + "\n"
							 + "      - Status: Check token!\n"
							 + "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
					LS.saveToLogger(print3);
				} else if (confirm == 1){
					buff6.putInt(Utils.redeemToken(tokenr, cpukey));
					String print3 = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n"
							 + "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n"
							 + "      - CPUKey: "+ cpukey + "\n"
							 + "      - Token: "+ tokenr + "\n"
							 + "      - Status: Redeem token!\n"
							 + "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
					LS.saveToLogger(print3);
				} else {
					buff6.putInt(462938);
					String print3 = "\n\n-----------------"+Utils.getTimestamp() + "-----------------\n\n"
							 + "   Socket Address: " + sock.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\n"
							 + "      - CPUKey: "+ cpukey + "\n"
							 + "      - Token: "+ tokenr + "\n"
							 + "      - Status: Token fail!\n"
							 + "\n-----------------"+Utils.getTimestamp() + "-----------------\n";
					LS.saveToLogger(print3);
				}
				byte[] padding5 = new byte[20];
				new Random().nextBytes(padding5);
				buff6.position(4);
				buff6.put(padding5);
				rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
				byte[] done7 = rc4.update(buff6.array());
				DataOutputStream out7 = new DataOutputStream(sock.getOutputStream());
				out7.write(done7);
				out7.flush();
				out7.close();
				break;
			case 5:
				byte[] b9 = new byte[len];
				in.readFully(b9);
				byte[] dec9 = rc4.update(b9);
				DataInputStream fin9 = new DataInputStream(new ByteArrayInputStream(dec9));
				int game = fin9.readInt();
				byte[] tmp9 = new byte[16];
				fin9.read(tmp9);
				cpukey = Utils.bytesToHex(tmp9);
				fin9.read(tmp9);
				hash = Utils.bytesToHex(tmp9);
				byte[] tmp0 = new byte[8];
				fin9.read(tmp0);
				token = Utils.bytesToHex(tmp0);
				fin9.close();
				GameHandler cli7 = new GameHandler(sock, game, cpukey, hash, token);
				cli7.start();
				break;
			case 6:
				byte[] b10 = new byte[len];
				byte[] ecc = new byte[0];
				XamChallenge chal;
				in.readFully(b10);
				rc4Key = LS.rc4Key;
			    rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
				byte[] dec10 = rc4.update(b10);
				try {
					File file1 = new File("request.bin");
					FileOutputStream fis = new FileOutputStream(file1);
					fis.write(new byte[]{0x00, 0x00, 0x00});
					fis.write(cmd);
					fis.write(new byte[]{0x00, 0x00, 0x00});
					fis.write(len);
					fis.write(dec10);
					fis.flush();
					fis.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				DataInputStream fin10 = new DataInputStream(new ByteArrayInputStream(dec10));
				byte[] tmp10 = new byte[16];
				fin10.read(tmp10);
				cpukey = Utils.bytesToHex(tmp10);
				fin10.read(tmp10);
				hash = Utils.bytesToHex(tmp10);
				byte[] tmp11 = new byte[8];
				fin10.read(tmp11);
				token = Utils.bytesToHex(tmp11);
				byte[] salt = new byte[16];
				fin10.read(salt);
				if (len == 0x48){
					byte[] shit = new byte[2];
					ecc = new byte[2];
					fin10.read(shit);
					fin10.read(ecc);
				}
				int clr = fin10.readInt();
				int fcrt = fin10.readInt();
				int type1kv = fin10.readInt();
				fin10.close();
				if (len == 0x48){
					chal = new XamChallenge(sock, clr, fcrt, type1kv, salt, ecc, cpukey, hash, token);
				} else {
					chal = new XamChallenge(sock, clr, fcrt, type1kv, salt, cpukey, hash, token);
				}
				chal.start();
				break;
			case 7:
				break;
			case 8:
				byte[] b14 = new byte[len];
				in.readFully(b14);
				byte[] dec14 = rc4.update(b14);
				DataInputStream fin14 = new DataInputStream(new ByteArrayInputStream(dec14));
				byte[] tmp19 = new byte[16];
				fin14.read(tmp19);
				cpukey = Utils.bytesToHex(tmp19);
				fin14.read(tmp19);
				hash = Utils.bytesToHex(tmp19);
				byte[] tmp15 = new byte[8];
				fin14.read(tmp15);
				token = Utils.bytesToHex(tmp15);
				MenuHandler mh = new MenuHandler(sock, cpukey, hash, token);
				mh.start();
				break;
			default:
				break;
			}
			in.close();
			sock.close();
		} catch (IOException | InvalidKeyException e1) {
			LS.saveToLogger("Error in console: HandleCommand!");
			e1.printStackTrace();
		}
	}

}

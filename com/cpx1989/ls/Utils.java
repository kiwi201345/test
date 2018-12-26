package com.cpx1989.LS;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cpx1989.LS.mysql.DBConnection;

public class Utils {

	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
	
	public static byte[] hexToBytes(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	public static int parseInt(int b1, int b2, int b3, int b4) {
		try {
			if ((b1 | b2 | b3 | b4) < 0)
				throw new EOFException();
			return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
		} catch (EOFException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static boolean isDateValid(String date) {
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			df.setLenient(false);
			df.parse(date);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}
	
	public static String getTimestamp(){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d = new Date();
		df.format(d);
		return "[" + df.format(d) + "]";
	}
	
	public static int getDays(String token){
		try{
			int days = 462938;
			String status;
			DBConnection.isDBCon();
			Statement statement;
			statement = DBConnection.dbcon.createStatement();
			statement.setEscapeProcessing(true);
			ResultSet res = statement.executeQuery("SELECT `status`, `days` FROM `tokens` WHERE `token` = '"+token+"' LIMIT 1");
			while (res.next()) {
				status = res.getString("status");
				days = res.getInt("days");
				if (status.equalsIgnoreCase("unused")){
					return days;
				} else {
					return 372839;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 462938;
	}
	
	public static int redeemToken(String token, String cpukey){
		try{
			int days = getDays(token);
			if (days == 462938 || days == 372839) return 462938;
			if (checkBrute(cpukey)) return 223829;
			DBConnection.isDBCon();
			Statement statement = DBConnection.dbcon.createStatement();
			if (days == 99999){
				statement.executeUpdate("UPDATE `clients` SET `days` = 0, `lifetime` = 1 WHERE `cpukey` = '"+cpukey+"'");
			} else {
				statement.executeUpdate("UPDATE `clients` SET `days` = "+days+" WHERE `cpukey` = '"+cpukey+"'");
			}
			DateTime now = new DateTime();
			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
			statement.executeUpdate("UPDATE `tokens` SET `status`= '"+cpukey+"', `redate`= '"+fmt.print(now)+"' WHERE `token` = '"+token+"'");
			return 2849230;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 462938;
	}
	
	public static boolean checkBrute(String cpukey){
		try{
			DBConnection.isDBCon();
			Statement statement = DBConnection.dbcon.createStatement();
			DateTime valid = new DateTime().minusHours(2);
			ResultSet res = statement.executeQuery("SELECT `last` FROM `redeem_fails` WHERE `cpukey` = '"+cpukey+"' AND `last` > "+valid.getMillis()+"");
			int i = 0;
			while (res.next()){
				i++;
			}
			if (i >= 5){
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static byte[] trimZeros(byte[] input){
		int i = input.length;
		while (i-- > 0 && input[i] == 0) {}

		byte[] output = new byte[i+1];
		System.arraycopy(input, 0, output, 0, i+1);
		return output;
	}
	
	public static String readUrl(String urlString) throws Exception {
	    BufferedReader reader = null;
	    try {
	        URL url = new URL(urlString);
	        reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        return buffer.toString();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
	
	public static String readURLFirefox(String urlString) throws Exception {
		BufferedReader reader = null;
		try {
			URL url = new URL(urlString);
			HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
			httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
			reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
			StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        return buffer.toString();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
}

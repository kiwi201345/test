package com.cpx1989.LS.mysql;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {

	public static MySQL mysql;
	public static Connection dbcon = null;
	static boolean init = false;
	static boolean check = false;

	public static boolean isDBCon() {
		try {
			if (dbcon == null || dbcon.isClosed()) {
				return initialize();
			} else {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean initialize() {
		String host = "localhost";
		String port = "3306";
		String db = "LS";
		String user = "xbl";
		String pass = "1234ESZxdr";
		mysql = new MySQL(host, port, db, user, pass);
		try {
			dbcon = mysql.openConnection();
			if (!dbcon.isClosed()) {
				return true;
			} else {
				return false;
			}
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}

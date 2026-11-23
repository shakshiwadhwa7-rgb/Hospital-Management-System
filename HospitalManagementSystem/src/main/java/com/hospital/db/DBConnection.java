package com.hospital.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gives a connection to the hospital_db database.
 * Every module uses this class instead of writing its own connection code.
 *
 * Usage:
 *   try (Connection con = DBConnection.getConnection()) { ... }
 */
public class DBConnection {

    // Same on every teammate's laptop (created by database/schema.sql)
    private static final String URL      = "jdbc:mysql://localhost:3306/hospital_db";
    private static final String USER     = "hms_user";
    private static final String PASSWORD = "hms123";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Quick check used at program start
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }
}

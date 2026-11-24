package com.hospital.admin;

import com.hospital.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * All SQL queries of the Admin module.
 * (DAO = Data Access Object: the class that talks to the database.)
 */
public class AdminDAO {

    // ---------------------------------------------------------
    // 1. View all staff accounts
    // ---------------------------------------------------------
    public static void viewAllStaff() {
        String sql = "SELECT user_id, username, role, full_name, phone, is_active "
                   + "FROM users ORDER BY role, user_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-4s %-12s %-14s %-22s %-12s %-8s%n",
                    "ID", "Username", "Role", "Full Name", "Phone", "Status");
            System.out.println("-".repeat(76));

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-4d %-12s %-14s %-22s %-12s %-8s%n",
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("full_name"),
                        rs.getString("phone") == null ? "-" : rs.getString("phone"),
                        rs.getBoolean("is_active") ? "Active" : "Inactive");
                count++;
            }
            System.out.println("-".repeat(76));
            System.out.println("Total staff: " + count);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 2. Add a new staff account
    //    returns true if saved, false if not
    // ---------------------------------------------------------
    public static boolean addStaff(String username, String password, String role,
                                   String fullName, String phone) {
        String sql = "INSERT INTO users (username, password, role, full_name, phone) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.setString(5, phone);

            int rows = ps.executeUpdate();   // number of rows inserted
            return rows == 1;

        } catch (SQLIntegrityConstraintViolationException e) {
            // username column is UNIQUE, so the same username can't be used twice
            System.out.println("Username '" + username + "' already exists. Choose another.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 3. Deactivate a staff account (they can no longer log in)
    //    Admin accounts can't be deactivated.
    // ---------------------------------------------------------
    public static boolean deactivateStaff(int userId) {
        String sql = "UPDATE users SET is_active = FALSE "
                   + "WHERE user_id = ? AND role <> 'ADMIN' AND is_active = TRUE";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 4a. Doctors whose specialization/fee is not added yet
    //     returns how many were found
    // ---------------------------------------------------------
    public static int viewDoctorsWithoutDetails() {
        String sql = "SELECT u.user_id, u.username, u.full_name "
                   + "FROM users u LEFT JOIN doctors d ON u.user_id = d.user_id "
                   + "WHERE u.role = 'DOCTOR' AND d.doctor_id IS NULL";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (count == 0) {
                    System.out.printf("%-8s %-12s %-22s%n", "User ID", "Username", "Full Name");
                    System.out.println("-".repeat(44));
                }
                System.out.printf("%-8d %-12s %-22s%n",
                        rs.getInt("user_id"), rs.getString("username"), rs.getString("full_name"));
                count++;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    // Checks that this user is a DOCTOR without details
    public static boolean isDoctorWithoutDetails(int userId) {
        String sql = "SELECT u.user_id FROM users u "
                   + "LEFT JOIN doctors d ON u.user_id = d.user_id "
                   + "WHERE u.user_id = ? AND u.role = 'DOCTOR' AND d.doctor_id IS NULL";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 4b. Save specialization and fee for a doctor
    // ---------------------------------------------------------
    public static boolean addDoctorDetails(int userId, String specialization, double fee) {
        String sql = "INSERT INTO doctors (user_id, specialization, consultation_fee) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, specialization);
            ps.setDouble(3, fee);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 5. View all doctors (JOIN of users + doctors)
    // ---------------------------------------------------------
    public static void viewAllDoctors() {
        String sql = "SELECT d.doctor_id, u.full_name, d.specialization, d.consultation_fee, "
                   + "u.phone, u.is_active "
                   + "FROM doctors d JOIN users u ON d.user_id = u.user_id "
                   + "ORDER BY d.doctor_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-6s %-22s %-20s %-10s %-12s %-8s%n",
                    "Doc ID", "Name", "Specialization", "Fee (Rs)", "Phone", "Status");
            System.out.println("-".repeat(82));

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-6d %-22s %-20s %-10.2f %-12s %-8s%n",
                        rs.getInt("doctor_id"),
                        rs.getString("full_name"),
                        rs.getString("specialization"),
                        rs.getDouble("consultation_fee"),
                        rs.getString("phone") == null ? "-" : rs.getString("phone"),
                        rs.getBoolean("is_active") ? "Active" : "Inactive");
                count++;
            }
            System.out.println("-".repeat(82));
            System.out.println("Total doctors: " + count);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 6. Hospital summary (counts from many tables)
    // ---------------------------------------------------------
    public static void viewHospitalSummary() {
        try (Connection con = DBConnection.getConnection()) {

            System.out.println("Total patients            : "
                    + count(con, "SELECT COUNT(*) FROM patients"));
            System.out.println("Active doctors            : "
                    + count(con, "SELECT COUNT(*) FROM users WHERE role = 'DOCTOR' AND is_active = TRUE"));
            System.out.println("Active nurses             : "
                    + count(con, "SELECT COUNT(*) FROM users WHERE role = 'NURSE' AND is_active = TRUE"));
            System.out.println("Total active staff        : "
                    + count(con, "SELECT COUNT(*) FROM users WHERE is_active = TRUE"));
            System.out.println("Today's appointments      : "
                    + count(con, "SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE"));
            System.out.println("Beds occupied             : "
                    + count(con, "SELECT COUNT(*) FROM beds WHERE is_occupied = TRUE")
                    + " / " + count(con, "SELECT COUNT(*) FROM beds"));
            System.out.println("Pending prescriptions     : "
                    + count(con, "SELECT COUNT(*) FROM prescriptions WHERE status = 'PENDING'"));
            System.out.println("Medicines low in stock    : "
                    + count(con, "SELECT COUNT(*) FROM medicines WHERE stock_qty < 50"));
            System.out.println("Unpaid bills              : "
                    + count(con, "SELECT COUNT(*) FROM bills WHERE payment_status = 'UNPAID'"));

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // Runs a "SELECT COUNT(*) ..." query and returns the number
    private static int count(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}

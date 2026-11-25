package com.hospital.pharmacist;

import com.hospital.db.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * All SQL queries of the Pharmacist module.
 */
public class PharmacistDAO {

    // ---------------------------------------------------------
    // 1. View pending prescriptions (not yet dispensed)
    // returns how many were found
    // ---------------------------------------------------------
    public static int viewPendingPrescriptions() {
        String sql = "SELECT pr.prescription_id, pr.created_on, p.name AS patient, "
                   + "u.full_name AS doctor, pr.diagnosis, "
                   + "GROUP_CONCAT(CONCAT(m.name, ' x', pi.quantity) SEPARATOR ', ') AS medicines "
                   + "FROM prescriptions pr "
                   + "JOIN patients p ON pr.patient_id = p.patient_id "
                   + "JOIN doctors d ON pr.doctor_id = d.doctor_id "
                   + "JOIN users u ON d.user_id = u.user_id "
                   + "LEFT JOIN prescription_items pi ON pr.prescription_id = pi.prescription_id "
                   + "LEFT JOIN medicines m ON pi.medicine_id = m.medicine_id "
                   + "WHERE pr.status = 'PENDING' "
                   + "GROUP BY pr.prescription_id ORDER BY pr.created_on, pr.prescription_id";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                System.out.println("Prescription #" + rs.getInt("prescription_id")
                        + "   Date: " + rs.getDate("created_on"));
                System.out.println("   Patient   : " + rs.getString("patient"));
                System.out.println("   Doctor    : " + rs.getString("doctor"));
                System.out.println("   Diagnosis : " + rs.getString("diagnosis"));
                System.out.println("   Medicines : "
                        + (rs.getString("medicines") == null ? "-" : rs.getString("medicines")));
                System.out.println("-".repeat(60));
                count++;
            }
            if (count == 0) {
                System.out.println("No pending prescriptions.");
            } else {
                System.out.println("Pending prescriptions: " + count);
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    // ---------------------------------------------------------
    // 2. Dispense medicines
    // ---------------------------------------------------------

    /**
     * Shows each medicine of a PENDING prescription with stock and expiry.
     * returns:  1 = everything OK, can dispense
     *           0 = prescription not found / not pending
     *          -1 = found, but stock is short or a medicine is expired
     */
    public static int checkPrescription(int prescriptionId) {
        String sql = "SELECT m.name, pi.quantity, pi.dosage, m.stock_qty, m.price, m.expiry_date "
                   + "FROM prescriptions pr "
                   + "JOIN prescription_items pi ON pr.prescription_id = pi.prescription_id "
                   + "JOIN medicines m ON pi.medicine_id = m.medicine_id "
                   + "WHERE pr.prescription_id = ? AND pr.status = 'PENDING'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                boolean ok = true;
                double total = 0;

                System.out.printf("%-22s %-5s %-24s %-6s %-9s %-10s%n",
                        "Medicine", "Qty", "Dosage", "Stock", "Amount", "Check");
                System.out.println("-".repeat(80));

                while (rs.next()) {
                    found = true;
                    int qty = rs.getInt("quantity");
                    int stock = rs.getInt("stock_qty");
                    double amount = qty * rs.getDouble("price");
                    LocalDate expiry = rs.getDate("expiry_date").toLocalDate();

                    String check = "OK";
                    if (expiry.isBefore(LocalDate.now())) {
                        check = "EXPIRED";
                        ok = false;
                    } else if (stock < qty) {
                        check = "LOW STOCK";
                        ok = false;
                    }

                    System.out.printf("%-22s %-5d %-24s %-6d %-9.2f %-10s%n",
                            rs.getString("name"), qty, rs.getString("dosage"), stock, amount, check);
                    total += amount;
                }

                if (!found) {
                    return 0;
                }
                System.out.println("-".repeat(80));
                System.out.printf("Total medicine amount: Rs %.2f%n", total);
                return ok ? 1 : -1;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Dispenses a prescription in ONE TRANSACTION:
     *   a) reduce stock of every medicine in it
     *   b) mark the prescription DISPENSED
     * If any medicine does not have enough stock, everything is rolled back.
     */
    public static boolean dispense(int prescriptionId) {
        String itemsSql  = "SELECT medicine_id, quantity FROM prescription_items WHERE prescription_id = ?";
        // "AND stock_qty >= ?" makes sure stock never goes below zero
        String stockSql  = "UPDATE medicines SET stock_qty = stock_qty - ? "
                         + "WHERE medicine_id = ? AND stock_qty >= ?";
        String statusSql = "UPDATE prescriptions SET status = 'DISPENSED' "
                         + "WHERE prescription_id = ? AND status = 'PENDING'";

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);           // start transaction

            // a) reduce stock
            try (PreparedStatement items = con.prepareStatement(itemsSql);
                 PreparedStatement stock = con.prepareStatement(stockSql)) {

                items.setInt(1, prescriptionId);
                try (ResultSet rs = items.executeQuery()) {
                    while (rs.next()) {
                        int qty = rs.getInt("quantity");
                        stock.setInt(1, qty);
                        stock.setInt(2, rs.getInt("medicine_id"));
                        stock.setInt(3, qty);
                        if (stock.executeUpdate() != 1) {
                            throw new SQLException("Not enough stock for medicine ID " + rs.getInt("medicine_id"));
                        }
                    }
                }
            }

            // b) mark dispensed
            try (PreparedStatement ps = con.prepareStatement(statusSql)) {
                ps.setInt(1, prescriptionId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Prescription is not pending any more.");
                }
            }

            con.commit();                       // save everything
            return true;

        } catch (SQLException e) {
            System.out.println("Could not dispense: " + e.getMessage());
            try {
                if (con != null) con.rollback();   // undo everything
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " + ex.getMessage());
            }
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                // ignore
            }
        }
        return false;
    }

    // ---------------------------------------------------------
    // 3. Add a new medicine; returns new medicine_id or -1
    // ---------------------------------------------------------
    public static int addMedicine(String name, String manufacturer, double price,
                                  int stock, LocalDate expiry) {
        String sql = "INSERT INTO medicines (name, manufacturer, price, stock_qty, expiry_date) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, manufacturer);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.setDate(5, Date.valueOf(expiry));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return -1;
    }

    // ---------------------------------------------------------
    // 4. Add stock to an existing medicine
    // ---------------------------------------------------------
    public static boolean addStock(int medicineId, int quantity) {
        String sql = "UPDATE medicines SET stock_qty = stock_qty + ? WHERE medicine_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setInt(2, medicineId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 5 & 6. View medicines
    //    onlyAlerts = false -> all medicines
    //    onlyAlerts = true  -> stock below 50 OR expiring within 90 days (or expired)
    // returns how many were shown
    // ---------------------------------------------------------
    public static final int LOW_STOCK_LIMIT = 50;
    public static final int EXPIRY_ALERT_DAYS = 90;

    public static int viewMedicines(boolean onlyAlerts) {
        String sql = "SELECT * FROM medicines "
                   + (onlyAlerts
                        ? "WHERE stock_qty < ? OR expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL ? DAY) "
                        : "")
                   + "ORDER BY medicine_id";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (onlyAlerts) {
                ps.setInt(1, LOW_STOCK_LIMIT);
                ps.setInt(2, EXPIRY_ALERT_DAYS);
            }

            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-5s %-22s %-14s %-9s %-6s %-11s %-16s%n",
                        "ID", "Medicine", "Manufacturer", "Price", "Stock", "Expiry", "Alert");
                System.out.println("-".repeat(88));

                LocalDate today = LocalDate.now();
                while (rs.next()) {
                    int stock = rs.getInt("stock_qty");
                    LocalDate expiry = rs.getDate("expiry_date").toLocalDate();

                    String alert = "-";
                    if (expiry.isBefore(today)) {
                        alert = "EXPIRED";
                    } else if (!expiry.isAfter(today.plusDays(EXPIRY_ALERT_DAYS))) {
                        alert = "EXPIRING SOON";
                    } else if (stock < LOW_STOCK_LIMIT) {
                        alert = "LOW STOCK";
                    }

                    System.out.printf("%-5d %-22s %-14s %-9.2f %-6d %-11s %-16s%n",
                            rs.getInt("medicine_id"),
                            rs.getString("name"),
                            rs.getString("manufacturer") == null ? "-" : rs.getString("manufacturer"),
                            rs.getDouble("price"),
                            stock,
                            expiry,
                            alert);
                    count++;
                }
                System.out.println("-".repeat(88));
                System.out.println("Medicines shown: " + count);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }
}

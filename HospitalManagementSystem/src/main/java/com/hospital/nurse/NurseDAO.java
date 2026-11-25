package com.hospital.nurse;

import com.hospital.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * All SQL queries of the Nurse module.
 */
public class NurseDAO {

    // ---------------------------------------------------------
    // 1. View beds
    //    onlyFree = true  -> only empty beds
    //    onlyFree = false -> all beds with patient name
    // returns how many beds were shown
    // ---------------------------------------------------------
    public static int viewBeds(boolean onlyFree) {
        String sql = "SELECT b.bed_id, b.ward_name, b.bed_number, b.charge_per_day, "
                   + "b.is_occupied, p.name, b.admitted_on "
                   + "FROM beds b LEFT JOIN patients p ON b.patient_id = p.patient_id "
                   + (onlyFree ? "WHERE b.is_occupied = FALSE " : "")
                   + "ORDER BY b.ward_name, b.bed_number";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-6s %-9s %-6s %-10s %-9s %-18s %-11s%n",
                    "Bed ID", "Ward", "Bed", "Rs/day", "Status", "Patient", "Admitted");
            System.out.println("-".repeat(75));

            while (rs.next()) {
                boolean occupied = rs.getBoolean("is_occupied");
                System.out.printf("%-6d %-9s %-6s %-10.2f %-9s %-18s %-11s%n",
                        rs.getInt("bed_id"),
                        rs.getString("ward_name"),
                        rs.getString("bed_number"),
                        rs.getDouble("charge_per_day"),
                        occupied ? "Occupied" : "Free",
                        occupied ? rs.getString("name") : "-",
                        occupied ? String.valueOf(rs.getDate("admitted_on")) : "-");
                count++;
            }
            System.out.println("-".repeat(75));
            System.out.println("Beds shown: " + count);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    // Shows only occupied beds (used before discharge)
    // returns how many are occupied
    public static int viewOccupiedBeds() {
        String sql = "SELECT b.bed_id, b.ward_name, b.bed_number, p.patient_id, p.name, b.admitted_on "
                   + "FROM beds b JOIN patients p ON b.patient_id = p.patient_id "
                   + "WHERE b.is_occupied = TRUE ORDER BY b.ward_name, b.bed_number";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (count == 0) {
                    System.out.printf("%-6s %-9s %-6s %-6s %-18s %-11s%n",
                            "Bed ID", "Ward", "Bed", "Pt ID", "Patient", "Admitted");
                    System.out.println("-".repeat(60));
                }
                System.out.printf("%-6d %-9s %-6s %-6d %-18s %-11s%n",
                        rs.getInt("bed_id"),
                        rs.getString("ward_name"),
                        rs.getString("bed_number"),
                        rs.getInt("patient_id"),
                        rs.getString("name"),
                        rs.getDate("admitted_on"));
                count++;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    // ---------------------------------------------------------
    // Helpers used by the menu
    // ---------------------------------------------------------

    // Returns the patient's name, or null if no such patient
    public static String getPatientName(int patientId) {
        String sql = "SELECT name FROM patients WHERE patient_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    // Returns true if this patient already has a bed
    public static boolean patientHasBed(int patientId) {
        String sql = "SELECT 1 FROM beds WHERE patient_id = ? AND is_occupied = TRUE";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 2. Allot a free bed to a patient
    // ---------------------------------------------------------
    public static boolean allotBed(int bedId, int patientId) {
        // "AND is_occupied = FALSE" makes sure we never give away an occupied bed
        String sql = "UPDATE beds SET is_occupied = TRUE, patient_id = ?, admitted_on = CURRENT_DATE "
                   + "WHERE bed_id = ? AND is_occupied = FALSE";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            ps.setInt(2, bedId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 3. Discharge: free the bed and print the room charge
    // ---------------------------------------------------------
    public static boolean dischargePatient(int bedId) {
        // Days stayed = today - admitted date + 1 (same day counts as 1 day)
        String selectSql = "SELECT p.name, b.charge_per_day, "
                         + "DATEDIFF(CURRENT_DATE, b.admitted_on) + 1 AS days "
                         + "FROM beds b JOIN patients p ON b.patient_id = p.patient_id "
                         + "WHERE b.bed_id = ? AND b.is_occupied = TRUE";
        String updateSql = "UPDATE beds SET is_occupied = FALSE, patient_id = NULL, admitted_on = NULL "
                         + "WHERE bed_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            String name;
            int days;
            double perDay;

            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setInt(1, bedId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;           // bed not occupied / wrong ID
                    }
                    name = rs.getString("name");
                    days = rs.getInt("days");
                    perDay = rs.getDouble("charge_per_day");
                }
            }

            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, bedId);
                ps.executeUpdate();
            }

            System.out.println(name + " discharged.");
            System.out.printf("Stayed %d day(s) x Rs %.2f = Room charge Rs %.2f%n",
                    days, perDay, days * perDay);
            System.out.println("Please inform the Receptionist to add this room charge to the bill.");
            return true;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 4. Record vitals (BP, temperature, pulse)
    // ---------------------------------------------------------
    public static boolean recordVitals(int patientId, int nurseUserId,
                                       String bp, double temperature, int pulse) {
        String sql = "INSERT INTO vitals (patient_id, nurse_id, bp, temperature, pulse) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            ps.setInt(2, nurseUserId);
            ps.setString(3, bp);
            ps.setDouble(4, temperature);
            ps.setInt(5, pulse);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 5. View all vitals of one patient (newest first)
    // ---------------------------------------------------------
    public static void viewVitals(int patientId) {
        String sql = "SELECT v.recorded_at, v.bp, v.temperature, v.pulse, u.full_name AS nurse "
                   + "FROM vitals v JOIN users u ON v.nurse_id = u.user_id "
                   + "WHERE v.patient_id = ? ORDER BY v.recorded_at DESC, v.vital_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-20s %-8s %-11s %-6s %-18s%n",
                        "Recorded at", "BP", "Temp (F)", "Pulse", "Nurse");
                System.out.println("-".repeat(66));

                int count = 0;
                while (rs.next()) {
                    System.out.printf("%-20s %-8s %-11.1f %-6d %-18s%n",
                            rs.getTimestamp("recorded_at").toString().substring(0, 16),
                            rs.getString("bp"),
                            rs.getDouble("temperature"),
                            rs.getInt("pulse"),
                            rs.getString("nurse"));
                    count++;
                }
                System.out.println("-".repeat(66));
                if (count == 0) {
                    System.out.println("No vitals recorded yet for this patient.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}

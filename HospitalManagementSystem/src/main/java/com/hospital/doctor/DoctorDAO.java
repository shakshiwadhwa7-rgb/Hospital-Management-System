package com.hospital.doctor;

import com.hospital.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * All SQL queries of the Doctor module.
 */
public class DoctorDAO {

    // One medicine line inside a prescription (used while writing a prescription)
    public static class MedicineItem {
        int medicineId;
        int quantity;
        String dosage;

        public MedicineItem(int medicineId, int quantity, String dosage) {
            this.medicineId = medicineId;
            this.quantity = quantity;
            this.dosage = dosage;
        }
    }

    // ---------------------------------------------------------
    // Finds doctor_id (doctors table) for the logged-in user_id
    // returns -1 if admin has not added this doctor's details
    // ---------------------------------------------------------
    public static int getDoctorId(int userId) {
        String sql = "SELECT doctor_id FROM doctors WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("doctor_id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return -1;
    }

    // ---------------------------------------------------------
    // 1 & 2. View this doctor's appointments
    //   todayOnly = true  -> only today's appointments
    //   todayOnly = false -> all appointments
    // returns how many were found
    // ---------------------------------------------------------
    public static int viewAppointments(int doctorId, boolean todayOnly) {
        String sql = "SELECT a.appointment_id, a.appointment_date, a.appointment_time, "
                   + "p.patient_id, p.name, p.age, p.gender, a.reason, a.status "
                   + "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id "
                   + "WHERE a.doctor_id = ? "
                   + (todayOnly ? "AND a.appointment_date = CURRENT_DATE " : "")
                   + "ORDER BY a.appointment_date DESC, a.appointment_time";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-6s %-11s %-6s %-6s %-18s %-4s %-7s %-18s %-10s%n",
                        "Appt", "Date", "Time", "Pt ID", "Patient", "Age", "Gender", "Reason", "Status");
                System.out.println("-".repeat(94));

                while (rs.next()) {
                    System.out.printf("%-6d %-11s %-6s %-6d %-18s %-4d %-7s %-18s %-10s%n",
                            rs.getInt("appointment_id"),
                            rs.getDate("appointment_date"),
                            rs.getString("appointment_time").substring(0, 5),
                            rs.getInt("patient_id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("gender"),
                            rs.getString("reason") == null ? "-" : rs.getString("reason"),
                            rs.getString("status"));
                    count++;
                }
                System.out.println("-".repeat(94));
                System.out.println("Total: " + count);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    // ---------------------------------------------------------
    // 3. Patient history: details + past visits + prescriptions
    // returns false if the patient does not exist
    // ---------------------------------------------------------
    public static boolean viewPatientHistory(int patientId) {
        String patientSql = "SELECT * FROM patients WHERE patient_id = ?";

        String visitsSql = "SELECT a.appointment_date, u.full_name AS doctor, a.reason, a.status "
                         + "FROM appointments a "
                         + "JOIN doctors d ON a.doctor_id = d.doctor_id "
                         + "JOIN users u ON d.user_id = u.user_id "
                         + "WHERE a.patient_id = ? ORDER BY a.appointment_date DESC";

        String prescSql = "SELECT pr.prescription_id, pr.created_on, pr.diagnosis, pr.notes, pr.status, "
                        + "GROUP_CONCAT(CONCAT(m.name, ' x', pi.quantity, ' (', pi.dosage, ')') SEPARATOR ', ') AS medicines "
                        + "FROM prescriptions pr "
                        + "LEFT JOIN prescription_items pi ON pr.prescription_id = pi.prescription_id "
                        + "LEFT JOIN medicines m ON pi.medicine_id = m.medicine_id "
                        + "WHERE pr.patient_id = ? "
                        + "GROUP BY pr.prescription_id ORDER BY pr.created_on DESC";

        try (Connection con = DBConnection.getConnection()) {

            // Patient details
            try (PreparedStatement ps = con.prepareStatement(patientSql)) {
                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    System.out.println("Name        : " + rs.getString("name"));
                    System.out.println("Age/Gender  : " + rs.getInt("age") + " / " + rs.getString("gender"));
                    System.out.println("Blood group : " + rs.getString("blood_group"));
                    System.out.println("Phone       : " + rs.getString("phone"));
                }
            }

            // Past visits
            System.out.println();
            System.out.println("--- Visits ---");
            try (PreparedStatement ps = con.prepareStatement(visitsSql)) {
                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean any = false;
                    while (rs.next()) {
                        System.out.printf("%-11s %-20s %-18s %-10s%n",
                                rs.getDate("appointment_date"),
                                rs.getString("doctor"),
                                rs.getString("reason") == null ? "-" : rs.getString("reason"),
                                rs.getString("status"));
                        any = true;
                    }
                    if (!any) System.out.println("No visits yet.");
                }
            }

            // Prescriptions
            System.out.println();
            System.out.println("--- Prescriptions ---");
            try (PreparedStatement ps = con.prepareStatement(prescSql)) {
                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean any = false;
                    while (rs.next()) {
                        System.out.println("#" + rs.getInt("prescription_id")
                                + "  " + rs.getDate("created_on")
                                + "  [" + rs.getString("status") + "]");
                        System.out.println("   Diagnosis : " + rs.getString("diagnosis"));
                        System.out.println("   Medicines : "
                                + (rs.getString("medicines") == null ? "-" : rs.getString("medicines")));
                        System.out.println("   Notes     : "
                                + (rs.getString("notes") == null ? "-" : rs.getString("notes")));
                        any = true;
                    }
                    if (!any) System.out.println("No prescriptions yet.");
                }
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // Returns patient_id if this appointment belongs to this doctor
    // and is still SCHEDULED, otherwise -1
    // ---------------------------------------------------------
    public static int getScheduledAppointmentPatient(int appointmentId, int doctorId) {
        String sql = "SELECT patient_id FROM appointments "
                   + "WHERE appointment_id = ? AND doctor_id = ? AND status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);
            ps.setInt(2, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("patient_id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return -1;
    }

    // ---------------------------------------------------------
    // Shows all medicines with stock (to pick from while prescribing)
    // ---------------------------------------------------------
    public static void viewMedicines() {
        String sql = "SELECT medicine_id, name, stock_qty FROM medicines ORDER BY medicine_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-5s %-22s %-6s%n", "ID", "Medicine", "Stock");
            System.out.println("-".repeat(35));
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-6d%n",
                        rs.getInt("medicine_id"), rs.getString("name"), rs.getInt("stock_qty"));
            }
            System.out.println("-".repeat(35));

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // Returns true if a medicine with this ID exists
    public static boolean medicineExists(int medicineId) {
        String sql = "SELECT 1 FROM medicines WHERE medicine_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 4. Save a prescription (TRANSACTION)
    //    a) insert into prescriptions
    //    b) insert every medicine into prescription_items
    //    c) mark the appointment COMPLETED
    //    Either all three happen, or none (rollback).
    // ---------------------------------------------------------
    public static boolean savePrescription(int appointmentId, int patientId, int doctorId,
                                           String diagnosis, String notes,
                                           List<MedicineItem> items) {

        String prescSql = "INSERT INTO prescriptions (appointment_id, patient_id, doctor_id, diagnosis, notes) "
                        + "VALUES (?, ?, ?, ?, ?)";
        String itemSql  = "INSERT INTO prescription_items (prescription_id, medicine_id, quantity, dosage) "
                        + "VALUES (?, ?, ?, ?)";
        String apptSql  = "UPDATE appointments SET status = 'COMPLETED' WHERE appointment_id = ?";

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);   // start transaction

            // a) prescription
            int prescriptionId;
            try (PreparedStatement ps = con.prepareStatement(prescSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, appointmentId);
                ps.setInt(2, patientId);
                ps.setInt(3, doctorId);
                ps.setString(4, diagnosis);
                ps.setString(5, notes);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    prescriptionId = keys.getInt(1);   // the new auto-increment ID
                }
            }

            // b) medicines
            try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                for (MedicineItem item : items) {
                    ps.setInt(1, prescriptionId);
                    ps.setInt(2, item.medicineId);
                    ps.setInt(3, item.quantity);
                    ps.setString(4, item.dosage);
                    ps.executeUpdate();
                }
            }

            // c) appointment completed
            try (PreparedStatement ps = con.prepareStatement(apptSql)) {
                ps.setInt(1, appointmentId);
                ps.executeUpdate();
            }

            con.commit();               // save everything
            System.out.println("Prescription #" + prescriptionId + " saved.");
            return true;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
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
    // 5. Mark an appointment COMPLETED (without prescription)
    // ---------------------------------------------------------
    public static boolean markCompleted(int appointmentId, int doctorId) {
        String sql = "UPDATE appointments SET status = 'COMPLETED' "
                   + "WHERE appointment_id = ? AND doctor_id = ? AND status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);
            ps.setInt(2, doctorId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }
}

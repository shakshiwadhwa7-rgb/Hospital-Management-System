package com.hospital.receptionist;

import com.hospital.db.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * All SQL queries of the Receptionist module.
 */
public class ReceptionistDAO {

    // ---------------------------------------------------------
    // 1. Register a new patient
    //    returns the new patient_id, or -1 if it failed
    // ---------------------------------------------------------
    public static int registerPatient(String name, int age, String gender, String phone,
                                      String address, String bloodGroup) {
        String sql = "INSERT INTO patients (name, age, gender, phone, address, blood_group) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, gender);
            ps.setString(4, phone);
            ps.setString(5, address);
            ps.setString(6, bloodGroup);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);      // new auto-increment ID
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return -1;
    }

    // ---------------------------------------------------------
    // 2 & 3. View patients
    //    keyword = null -> all patients
    //    keyword = text -> patients whose name or phone contains the text
    // returns how many were found
    // ---------------------------------------------------------
    public static int viewPatients(String keyword) {
        String sql = "SELECT * FROM patients "
                   + (keyword == null ? "" : "WHERE name LIKE ? OR phone LIKE ? ")
                   + "ORDER BY patient_id";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (keyword != null) {
                ps.setString(1, "%" + keyword + "%");   // % means "anything"
                ps.setString(2, "%" + keyword + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-6s %-18s %-4s %-7s %-12s %-6s %-14s %-11s%n",
                        "Pt ID", "Name", "Age", "Gender", "Phone", "Blood", "Address", "Registered");
                System.out.println("-".repeat(84));

                while (rs.next()) {
                    System.out.printf("%-6d %-18s %-4d %-7s %-12s %-6s %-14s %-11s%n",
                            rs.getInt("patient_id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("gender"),
                            rs.getString("phone"),
                            rs.getString("blood_group"),
                            rs.getString("address"),
                            rs.getDate("registered_on"));
                    count++;
                }
                System.out.println("-".repeat(84));
                System.out.println("Patients found: " + count);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

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

    // ---------------------------------------------------------
    // 4. Book appointment
    // ---------------------------------------------------------

    // Shows active doctors who have details; returns how many
    public static int viewActiveDoctors() {
        String sql = "SELECT d.doctor_id, u.full_name, d.specialization, d.consultation_fee "
                   + "FROM doctors d JOIN users u ON d.user_id = u.user_id "
                   + "WHERE u.is_active = TRUE ORDER BY d.doctor_id";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-6s %-22s %-20s %-10s%n", "Doc ID", "Doctor", "Specialization", "Fee (Rs)");
            System.out.println("-".repeat(60));
            while (rs.next()) {
                System.out.printf("%-6d %-22s %-20s %-10.2f%n",
                        rs.getInt("doctor_id"),
                        rs.getString("full_name"),
                        rs.getString("specialization"),
                        rs.getDouble("consultation_fee"));
                count++;
            }
            System.out.println("-".repeat(60));

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    // Returns true if this doctor exists and is active
    public static boolean isActiveDoctor(int doctorId) {
        String sql = "SELECT 1 FROM doctors d JOIN users u ON d.user_id = u.user_id "
                   + "WHERE d.doctor_id = ? AND u.is_active = TRUE";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // Returns true if the doctor already has a SCHEDULED appointment at this date + time
    public static boolean isSlotTaken(int doctorId, LocalDate date, LocalTime time) {
        String sql = "SELECT 1 FROM appointments WHERE doctor_id = ? AND appointment_date = ? "
                   + "AND appointment_time = ? AND status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return true;   // on error, treat as taken (safe side)
    }

    // Saves the appointment; returns new appointment_id or -1
    public static int bookAppointment(int patientId, int doctorId, LocalDate date,
                                      LocalTime time, String reason) {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, reason) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, Time.valueOf(time));
            ps.setString(5, reason);
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
    // 5. Cancel appointment
    // ---------------------------------------------------------

    // Shows all SCHEDULED appointments; returns how many
    public static int viewScheduledAppointments() {
        String sql = "SELECT a.appointment_id, a.appointment_date, a.appointment_time, "
                   + "p.name AS patient, u.full_name AS doctor, a.reason "
                   + "FROM appointments a "
                   + "JOIN patients p ON a.patient_id = p.patient_id "
                   + "JOIN doctors d ON a.doctor_id = d.doctor_id "
                   + "JOIN users u ON d.user_id = u.user_id "
                   + "WHERE a.status = 'SCHEDULED' "
                   + "ORDER BY a.appointment_date, a.appointment_time";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (count == 0) {
                    System.out.printf("%-6s %-11s %-6s %-18s %-20s %-16s%n",
                            "Appt", "Date", "Time", "Patient", "Doctor", "Reason");
                    System.out.println("-".repeat(80));
                }
                System.out.printf("%-6d %-11s %-6s %-18s %-20s %-16s%n",
                        rs.getInt("appointment_id"),
                        rs.getDate("appointment_date"),
                        rs.getString("appointment_time").substring(0, 5),
                        rs.getString("patient"),
                        rs.getString("doctor"),
                        rs.getString("reason") == null ? "-" : rs.getString("reason"));
                count++;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    public static boolean cancelAppointment(int appointmentId) {
        String sql = "UPDATE appointments SET status = 'CANCELLED' "
                   + "WHERE appointment_id = ? AND status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    // ---------------------------------------------------------
    // 6. Generate bill
    //    Only charges that came AFTER the patient's last bill are counted,
    //    so the same visit is not billed twice.
    // ---------------------------------------------------------

    // Doctor fees of COMPLETED appointments not yet billed
    public static double getUnbilledConsultation(int patientId) {
        String sql = "SELECT COALESCE(SUM(d.consultation_fee), 0) "
                   + "FROM appointments a JOIN doctors d ON a.doctor_id = d.doctor_id "
                   + "WHERE a.patient_id = ? AND a.status = 'COMPLETED' "
                   + "AND a.appointment_date > COALESCE("
                   + "  (SELECT MAX(bill_date) FROM bills WHERE patient_id = ?), '1900-01-01')";
        return sumQuery(sql, patientId);
    }

    // Price of medicines DISPENSED by the pharmacist, not yet billed
    public static double getUnbilledMedicines(int patientId) {
        String sql = "SELECT COALESCE(SUM(pi.quantity * m.price), 0) "
                   + "FROM prescriptions pr "
                   + "JOIN prescription_items pi ON pr.prescription_id = pi.prescription_id "
                   + "JOIN medicines m ON pi.medicine_id = m.medicine_id "
                   + "WHERE pr.patient_id = ? AND pr.status = 'DISPENSED' "
                   + "AND pr.created_on > COALESCE("
                   + "  (SELECT MAX(bill_date) FROM bills WHERE patient_id = ?), '1900-01-01')";
        return sumQuery(sql, patientId);
    }

    // Runs a SUM query that needs the patient_id twice
    private static double sumQuery(String sql, int patientId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            ps.setInt(2, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return 0;
    }

    // Saves the bill; returns new bill_id or -1
    public static int saveBill(int patientId, double consultation, double medicine,
                               double room, boolean paid) {
        String sql = "INSERT INTO bills (patient_id, consultation_charge, medicine_charge, "
                   + "room_charge, total_amount, payment_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, patientId);
            ps.setDouble(2, consultation);
            ps.setDouble(3, medicine);
            ps.setDouble(4, room);
            ps.setDouble(5, consultation + medicine + room);
            ps.setString(6, paid ? "PAID" : "UNPAID");
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
    // 7. View bills and mark them paid
    // ---------------------------------------------------------
    public static int viewBills() {
        String sql = "SELECT b.bill_id, b.bill_date, p.name, b.consultation_charge, "
                   + "b.medicine_charge, b.room_charge, b.total_amount, b.payment_status "
                   + "FROM bills b JOIN patients p ON b.patient_id = p.patient_id "
                   + "ORDER BY b.payment_status DESC, b.bill_id DESC";   // UNPAID first

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-5s %-11s %-18s %-9s %-9s %-9s %-10s %-7s%n",
                    "Bill", "Date", "Patient", "Doctor", "Medicine", "Room", "Total", "Status");
            System.out.println("-".repeat(84));
            while (rs.next()) {
                System.out.printf("%-5d %-11s %-18s %-9.2f %-9.2f %-9.2f %-10.2f %-7s%n",
                        rs.getInt("bill_id"),
                        rs.getDate("bill_date"),
                        rs.getString("name"),
                        rs.getDouble("consultation_charge"),
                        rs.getDouble("medicine_charge"),
                        rs.getDouble("room_charge"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_status"));
                count++;
            }
            System.out.println("-".repeat(84));
            System.out.println("Total bills: " + count);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return count;
    }

    public static boolean markBillPaid(int billId) {
        String sql = "UPDATE bills SET payment_status = 'PAID' "
                   + "WHERE bill_id = ? AND payment_status = 'UNPAID'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, billId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }
}

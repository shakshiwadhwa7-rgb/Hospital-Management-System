package com.hospital.receptionist;

import com.hospital.model.User;
import com.hospital.util.InputHelper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Receptionist module: menu and user input.
 * SQL queries are in ReceptionistDAO.
 */
public class ReceptionistMenu {

    private static final String[] BLOOD_GROUPS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    public static void show(User user) {
        while (true) {
            InputHelper.printHeader("RECEPTIONIST MENU");
            System.out.println("1. Register New Patient");
            System.out.println("2. View All Patients");
            System.out.println("3. Search Patient");
            System.out.println("4. Book Appointment");
            System.out.println("5. Cancel Appointment");
            System.out.println("6. Generate Bill");
            System.out.println("7. View Bills / Mark Paid");
            System.out.println("8. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1: registerPatient(); break;
                case 2:
                    InputHelper.printHeader("ALL PATIENTS");
                    ReceptionistDAO.viewPatients(null);
                    break;
                case 3: searchPatient(); break;
                case 4: bookAppointment(); break;
                case 5: cancelAppointment(); break;
                case 6: generateBill(); break;
                case 7: viewBills(); break;
                case 8: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }

    // ---------------------------------------------------------
    // 1. Register patient
    // ---------------------------------------------------------
    private static void registerPatient() {
        InputHelper.printHeader("REGISTER NEW PATIENT");

        String name = InputHelper.readString("Full name: ");

        int age = InputHelper.readInt("Age: ");
        if (age < 0 || age > 120) {
            System.out.println("Age must be between 0 and 120.");
            return;
        }

        System.out.println("Gender: 1. Male  2. Female  3. Other");
        String gender;
        switch (InputHelper.readInt("Enter gender (1-3): ")) {
            case 1: gender = "MALE"; break;
            case 2: gender = "FEMALE"; break;
            case 3: gender = "OTHER"; break;
            default:
                System.out.println("Invalid gender.");
                return;
        }

        String phone = InputHelper.readString("Phone (10 digits): ");
        if (!phone.matches("\\d{10}")) {
            System.out.println("Phone number must be exactly 10 digits.");
            return;
        }

        String address = InputHelper.readString("Address / city: ");

        System.out.println("Blood group: 1.A+ 2.A- 3.B+ 4.B- 5.AB+ 6.AB- 7.O+ 8.O-");
        int bg = InputHelper.readInt("Enter blood group (1-8): ");
        if (bg < 1 || bg > 8) {
            System.out.println("Invalid blood group.");
            return;
        }
        String bloodGroup = BLOOD_GROUPS[bg - 1];

        int newId = ReceptionistDAO.registerPatient(name, age, gender, phone, address, bloodGroup);
        if (newId != -1) {
            System.out.println("Patient registered! Patient ID: " + newId);
        } else {
            System.out.println("Patient was NOT registered.");
        }
    }

    // ---------------------------------------------------------
    // 3. Search patient
    // ---------------------------------------------------------
    private static void searchPatient() {
        InputHelper.printHeader("SEARCH PATIENT");
        String keyword = InputHelper.readString("Enter name or phone (full or part): ");
        ReceptionistDAO.viewPatients(keyword);
    }

    // ---------------------------------------------------------
    // 4. Book appointment
    // ---------------------------------------------------------
    private static void bookAppointment() {
        InputHelper.printHeader("BOOK APPOINTMENT");

        int patientId = InputHelper.readInt("Enter Patient ID: ");
        String name = ReceptionistDAO.getPatientName(patientId);
        if (name == null) {
            System.out.println("No patient with ID " + patientId + ". Register the patient first (option 1).");
            return;
        }
        System.out.println("Patient: " + name);

        System.out.println("Available doctors:");
        if (ReceptionistDAO.viewActiveDoctors() == 0) {
            System.out.println("No doctors available.");
            return;
        }
        int doctorId = InputHelper.readInt("Enter Doctor ID: ");
        if (!ReceptionistDAO.isActiveDoctor(doctorId)) {
            System.out.println("Invalid Doctor ID.");
            return;
        }

        // Date
        LocalDate date;
        try {
            date = LocalDate.parse(InputHelper.readString("Date (YYYY-MM-DD, e.g. " + LocalDate.now() + "): "));
        } catch (DateTimeParseException e) {
            System.out.println("Wrong date format. Use YYYY-MM-DD.");
            return;
        }
        if (date.isBefore(LocalDate.now())) {
            System.out.println("Date cannot be in the past.");
            return;
        }

        // Time
        LocalTime time;
        try {
            time = LocalTime.parse(InputHelper.readString("Time (HH:MM, 24-hour, 09:00 to 20:00): "));
        } catch (DateTimeParseException e) {
            System.out.println("Wrong time format. Use HH:MM, e.g. 10:30 or 15:00.");
            return;
        }
        if (time.isBefore(LocalTime.of(9, 0)) || time.isAfter(LocalTime.of(20, 0))) {
            System.out.println("Hospital OPD timing is 09:00 to 20:00.");
            return;
        }

        if (ReceptionistDAO.isSlotTaken(doctorId, date, time)) {
            System.out.println("This doctor already has an appointment at that time. Choose another time.");
            return;
        }

        String reason = InputHelper.readString("Reason for visit: ");

        int apptId = ReceptionistDAO.bookAppointment(patientId, doctorId, date, time, reason);
        if (apptId != -1) {
            System.out.println("Appointment booked! Appointment ID: " + apptId);
        } else {
            System.out.println("Appointment was NOT booked.");
        }
    }

    // ---------------------------------------------------------
    // 5. Cancel appointment
    // ---------------------------------------------------------
    private static void cancelAppointment() {
        InputHelper.printHeader("CANCEL APPOINTMENT");

        if (ReceptionistDAO.viewScheduledAppointments() == 0) {
            System.out.println("There are no scheduled appointments.");
            return;
        }

        int apptId = InputHelper.readInt("Enter Appointment ID to cancel (0 to go back): ");
        if (apptId == 0) {
            return;
        }
        if (ReceptionistDAO.cancelAppointment(apptId)) {
            System.out.println("Appointment cancelled.");
        } else {
            System.out.println("Could not cancel. Check the ID (only SCHEDULED appointments can be cancelled).");
        }
    }

    // ---------------------------------------------------------
    // 6. Generate bill
    // ---------------------------------------------------------
    private static void generateBill() {
        InputHelper.printHeader("GENERATE BILL");

        int patientId = InputHelper.readInt("Enter Patient ID: ");
        String name = ReceptionistDAO.getPatientName(patientId);
        if (name == null) {
            System.out.println("No patient with ID " + patientId);
            return;
        }

        double consultation = ReceptionistDAO.getUnbilledConsultation(patientId);
        double medicine = ReceptionistDAO.getUnbilledMedicines(patientId);

        System.out.println("Patient: " + name);
        System.out.printf("Doctor fees (completed visits)  : Rs %.2f%n", consultation);
        System.out.printf("Medicines (given by pharmacist) : Rs %.2f%n", medicine);

        double room = InputHelper.readDouble("Room charge (from nurse at discharge, 0 if none): Rs ");
        if (room < 0) {
            System.out.println("Room charge cannot be negative.");
            return;
        }

        double total = consultation + medicine + room;
        if (total == 0) {
            System.out.println("Nothing to bill for this patient.");
            return;
        }

        boolean paid = InputHelper.readInt("Payment received now? 1. Yes  2. No : ") == 1;

        int billId = ReceptionistDAO.saveBill(patientId, consultation, medicine, room, paid);
        if (billId == -1) {
            System.out.println("Bill was NOT saved.");
            return;
        }

        // Print the bill
        System.out.println();
        System.out.println("==========================================");
        System.out.println("           HOSPITAL BILL  #" + billId);
        System.out.println("==========================================");
        System.out.println("Patient : " + name + "  (ID " + patientId + ")");
        System.out.println("Date    : " + LocalDate.now());
        System.out.println("------------------------------------------");
        System.out.printf("Consultation charge     Rs %10.2f%n", consultation);
        System.out.printf("Medicine charge         Rs %10.2f%n", medicine);
        System.out.printf("Room charge             Rs %10.2f%n", room);
        System.out.println("------------------------------------------");
        System.out.printf("TOTAL                   Rs %10.2f%n", total);
        System.out.println("Status  : " + (paid ? "PAID" : "UNPAID"));
        System.out.println("==========================================");
    }

    // ---------------------------------------------------------
    // 7. View bills / mark paid
    // ---------------------------------------------------------
    private static void viewBills() {
        InputHelper.printHeader("ALL BILLS");

        if (ReceptionistDAO.viewBills() == 0) {
            return;
        }

        int billId = InputHelper.readInt("Enter Bill ID to mark PAID (0 to go back): ");
        if (billId == 0) {
            return;
        }
        if (ReceptionistDAO.markBillPaid(billId)) {
            System.out.println("Bill #" + billId + " marked PAID.");
        } else {
            System.out.println("Could not update. Check the ID (bill may already be PAID).");
        }
    }
}

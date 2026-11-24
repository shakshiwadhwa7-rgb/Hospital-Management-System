package com.hospital.doctor;

import com.hospital.model.User;
import com.hospital.util.InputHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Doctor module: menu and user input.
 * SQL queries are in DoctorDAO.
 */
public class DoctorMenu {

    public static void show(User user) {

        // Find this doctor's ID in the doctors table
        int doctorId = DoctorDAO.getDoctorId(user.getUserId());
        if (doctorId == -1) {
            System.out.println("Your doctor details are not added yet.");
            System.out.println("Please ask the Admin to add your specialization and fee (Admin option 4).");
            return;
        }

        while (true) {
            InputHelper.printHeader("DOCTOR MENU");
            System.out.println("1. View Today's Appointments");
            System.out.println("2. View All My Appointments");
            System.out.println("3. View Patient History");
            System.out.println("4. Write Prescription");
            System.out.println("5. Mark Appointment Completed");
            System.out.println("6. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1:
                    InputHelper.printHeader("TODAY'S APPOINTMENTS");
                    DoctorDAO.viewAppointments(doctorId, true);
                    break;
                case 2:
                    InputHelper.printHeader("ALL MY APPOINTMENTS");
                    DoctorDAO.viewAppointments(doctorId, false);
                    break;
                case 3: viewPatientHistory(); break;
                case 4: writePrescription(doctorId); break;
                case 5: markCompleted(doctorId); break;
                case 6: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }

    private static void viewPatientHistory() {
        InputHelper.printHeader("PATIENT HISTORY");
        int patientId = InputHelper.readInt("Enter Patient ID: ");
        if (!DoctorDAO.viewPatientHistory(patientId)) {
            System.out.println("No patient found with ID " + patientId);
        }
    }

    private static void writePrescription(int doctorId) {
        InputHelper.printHeader("WRITE PRESCRIPTION");
        DoctorDAO.viewAppointments(doctorId, false);

        int appointmentId = InputHelper.readInt("Enter Appointment ID (only SCHEDULED, 0 to cancel): ");
        if (appointmentId == 0) {
            return;
        }

        int patientId = DoctorDAO.getScheduledAppointmentPatient(appointmentId, doctorId);
        if (patientId == -1) {
            System.out.println("This appointment is not yours or is not SCHEDULED.");
            return;
        }

        String diagnosis = InputHelper.readString("Diagnosis: ");
        String notes     = InputHelper.readString("Notes / advice: ");

        // Add medicines one by one
        List<DoctorDAO.MedicineItem> items = new ArrayList<>();
        System.out.println();
        System.out.println("Add medicines (enter 0 as Medicine ID when done):");
        DoctorDAO.viewMedicines();

        while (true) {
            int medicineId = InputHelper.readInt("Medicine ID (0 = done): ");
            if (medicineId == 0) {
                break;
            }
            if (!DoctorDAO.medicineExists(medicineId)) {
                System.out.println("No medicine with ID " + medicineId);
                continue;
            }
            int qty = InputHelper.readInt("Quantity: ");
            if (qty <= 0) {
                System.out.println("Quantity must be more than 0.");
                continue;
            }
            String dosage = InputHelper.readString("Dosage (e.g. 1-0-1 after food): ");
            items.add(new DoctorDAO.MedicineItem(medicineId, qty, dosage));
            System.out.println("Added. Medicines so far: " + items.size());
        }

        if (items.isEmpty()) {
            System.out.println("No medicines added. Prescription NOT saved.");
            return;
        }

        if (DoctorDAO.savePrescription(appointmentId, patientId, doctorId, diagnosis, notes, items)) {
            System.out.println("Appointment marked COMPLETED. Pharmacist can now see this prescription.");
        } else {
            System.out.println("Prescription was NOT saved.");
        }
    }

    private static void markCompleted(int doctorId) {
        InputHelper.printHeader("MARK APPOINTMENT COMPLETED");
        DoctorDAO.viewAppointments(doctorId, false);

        int appointmentId = InputHelper.readInt("Enter Appointment ID (0 to cancel): ");
        if (appointmentId == 0) {
            return;
        }

        if (DoctorDAO.markCompleted(appointmentId, doctorId)) {
            System.out.println("Appointment marked COMPLETED.");
        } else {
            System.out.println("Could not update. Check that the appointment is yours and SCHEDULED.");
        }
    }
}

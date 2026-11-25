package com.hospital.nurse;

import com.hospital.model.User;
import com.hospital.util.InputHelper;

/**
 * Nurse module: menu and user input.
 * SQL queries are in NurseDAO.
 */
public class NurseMenu {

    public static void show(User user) {
        while (true) {
            InputHelper.printHeader("NURSE MENU");
            System.out.println("1. View All Beds");
            System.out.println("2. Allot Bed to Patient");
            System.out.println("3. Discharge Patient");
            System.out.println("4. Record Patient Vitals");
            System.out.println("5. View Patient Vitals");
            System.out.println("6. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1:
                    InputHelper.printHeader("ALL BEDS");
                    NurseDAO.viewBeds(false);
                    break;
                case 2: allotBed(); break;
                case 3: discharge(); break;
                case 4: recordVitals(user); break;
                case 5: viewVitals(); break;
                case 6: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }

    private static void allotBed() {
        InputHelper.printHeader("ALLOT BED");

        int patientId = InputHelper.readInt("Enter Patient ID: ");
        String name = NurseDAO.getPatientName(patientId);
        if (name == null) {
            System.out.println("No patient found with ID " + patientId);
            return;
        }
        if (NurseDAO.patientHasBed(patientId)) {
            System.out.println(name + " already has a bed.");
            return;
        }
        System.out.println("Patient: " + name);

        System.out.println("Free beds:");
        if (NurseDAO.viewBeds(true) == 0) {
            System.out.println("Sorry, no free beds available.");
            return;
        }

        int bedId = InputHelper.readInt("Enter Bed ID to allot: ");
        if (NurseDAO.allotBed(bedId, patientId)) {
            System.out.println("Bed allotted to " + name + ".");
        } else {
            System.out.println("Could not allot. Check that the Bed ID is in the free list.");
        }
    }

    private static void discharge() {
        InputHelper.printHeader("DISCHARGE PATIENT");

        if (NurseDAO.viewOccupiedBeds() == 0) {
            System.out.println("No patients are admitted right now.");
            return;
        }

        int bedId = InputHelper.readInt("Enter Bed ID to discharge (0 to cancel): ");
        if (bedId == 0) {
            return;
        }
        if (!NurseDAO.dischargePatient(bedId)) {
            System.out.println("This bed is not occupied or the ID is wrong.");
        }
    }

    private static void recordVitals(User nurse) {
        InputHelper.printHeader("RECORD VITALS");

        int patientId = InputHelper.readInt("Enter Patient ID: ");
        String name = NurseDAO.getPatientName(patientId);
        if (name == null) {
            System.out.println("No patient found with ID " + patientId);
            return;
        }
        System.out.println("Patient: " + name);

        String bp = InputHelper.readString("Blood pressure (e.g. 120/80): ");
        if (!bp.matches("\\d{2,3}/\\d{2,3}")) {
            System.out.println("BP must look like 120/80.");
            return;
        }

        double temperature = InputHelper.readDouble("Temperature in F (e.g. 98.6): ");
        if (temperature < 90 || temperature > 110) {
            System.out.println("Temperature should be between 90 and 110 F.");
            return;
        }

        int pulse = InputHelper.readInt("Pulse (beats per minute): ");
        if (pulse < 30 || pulse > 220) {
            System.out.println("Pulse should be between 30 and 220.");
            return;
        }

        if (NurseDAO.recordVitals(patientId, nurse.getUserId(), bp, temperature, pulse)) {
            System.out.println("Vitals saved for " + name + ".");
        } else {
            System.out.println("Vitals were NOT saved.");
        }
    }

    private static void viewVitals() {
        InputHelper.printHeader("PATIENT VITALS");

        int patientId = InputHelper.readInt("Enter Patient ID: ");
        String name = NurseDAO.getPatientName(patientId);
        if (name == null) {
            System.out.println("No patient found with ID " + patientId);
            return;
        }
        System.out.println("Patient: " + name);
        NurseDAO.viewVitals(patientId);
    }
}

package com.hospital.admin;

import com.hospital.model.User;
import com.hospital.util.InputHelper;

/**
 * Admin module: menu and user input.
 * SQL queries are in AdminDAO.
 */
public class AdminMenu {

    public static void show(User user) {
        while (true) {
            InputHelper.printHeader("ADMIN MENU");
            System.out.println("1. Add Staff Account");
            System.out.println("2. View All Staff");
            System.out.println("3. Deactivate Staff Account");
            System.out.println("4. Add Doctor Details");
            System.out.println("5. View All Doctors");
            System.out.println("6. View Hospital Summary");
            System.out.println("7. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1: addStaff(); break;
                case 2:
                    InputHelper.printHeader("ALL STAFF");
                    AdminDAO.viewAllStaff();
                    break;
                case 3: deactivateStaff(); break;
                case 4: addDoctorDetails(); break;
                case 5:
                    InputHelper.printHeader("ALL DOCTORS");
                    AdminDAO.viewAllDoctors();
                    break;
                case 6:
                    InputHelper.printHeader("HOSPITAL SUMMARY");
                    AdminDAO.viewHospitalSummary();
                    break;
                case 7: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }

    // Asks the admin for the new staff member's details and saves them
    private static void addStaff() {
        InputHelper.printHeader("ADD STAFF ACCOUNT");

        String fullName = InputHelper.readString("Full name: ");
        String username = InputHelper.readString("Username: ");
        String password = InputHelper.readString("Password: ");
        String phone    = InputHelper.readString("Phone: ");

        System.out.println("Select role:");
        System.out.println("1. Doctor");
        System.out.println("2. Nurse");
        System.out.println("3. Receptionist");
        System.out.println("4. Pharmacist");

        String role = null;
        while (role == null) {
            int r = InputHelper.readInt("Enter role (1-4): ");
            switch (r) {
                case 1: role = "DOCTOR"; break;
                case 2: role = "NURSE"; break;
                case 3: role = "RECEPTIONIST"; break;
                case 4: role = "PHARMACIST"; break;
                default: System.out.println("Please enter a number from 1 to 4.");
            }
        }

        if (AdminDAO.addStaff(username, password, role, fullName, phone)) {
            System.out.println("Staff account created for " + fullName + " (" + role + ").");
            if (role.equals("DOCTOR")) {
                System.out.println("Now use option 4 (Add Doctor Details) to add specialization and fee.");
            }
        } else {
            System.out.println("Staff account was NOT created.");
        }
    }

    // Shows all staff, asks for an ID and deactivates that account
    private static void deactivateStaff() {
        InputHelper.printHeader("DEACTIVATE STAFF ACCOUNT");
        AdminDAO.viewAllStaff();

        int userId = InputHelper.readInt("Enter User ID to deactivate (0 to cancel): ");
        if (userId == 0) {
            return;
        }

        if (AdminDAO.deactivateStaff(userId)) {
            System.out.println("Account deactivated. This user can no longer log in.");
        } else {
            System.out.println("Could not deactivate. Check the ID (admin or already inactive accounts can't be deactivated).");
        }
    }

    // Adds specialization and fee for a doctor account
    private static void addDoctorDetails() {
        InputHelper.printHeader("ADD DOCTOR DETAILS");

        int found = AdminDAO.viewDoctorsWithoutDetails();
        if (found == 0) {
            System.out.println("All doctors already have their details. Add a new doctor with option 1 first.");
            return;
        }

        int userId = InputHelper.readInt("Enter User ID of the doctor: ");
        if (!AdminDAO.isDoctorWithoutDetails(userId)) {
            System.out.println("This User ID is not in the list above.");
            return;
        }

        String specialization = InputHelper.readString("Specialization (e.g. Cardiologist): ");
        double fee = InputHelper.readDouble("Consultation fee (Rs): ");
        if (fee < 0) {
            System.out.println("Fee cannot be negative.");
            return;
        }

        if (AdminDAO.addDoctorDetails(userId, specialization, fee)) {
            System.out.println("Doctor details saved.");
        } else {
            System.out.println("Doctor details were NOT saved.");
        }
    }
}

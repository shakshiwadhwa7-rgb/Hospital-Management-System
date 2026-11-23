package com.hospital;

import com.hospital.admin.AdminMenu;
import com.hospital.db.DBConnection;
import com.hospital.doctor.DoctorMenu;
import com.hospital.login.LoginService;
import com.hospital.model.User;
import com.hospital.nurse.NurseMenu;
import com.hospital.pharmacist.PharmacistMenu;
import com.hospital.receptionist.ReceptionistMenu;
import com.hospital.util.InputHelper;

/**
 * Starting point of the program.
 * Shows the login screen, then opens the menu for the user's role.
 */
public class HospitalManagementSystem {

    public static void main(String[] args) {

        System.out.println("==========================================");
        System.out.println("      HOSPITAL MANAGEMENT SYSTEM");
        System.out.println("==========================================");

        if (!DBConnection.testConnection()) {
            System.out.println("Could not connect to MySQL.");
            System.out.println("Check that MySQL is running and database/schema.sql has been run.");
            return;
        }

        while (true) {
            InputHelper.printHeader("MAIN MENU");
            System.out.println("1. Login");
            System.out.println("2. Exit");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1:
                    loginAndOpenMenu();
                    break;
                case 2:
                    System.out.println("Thank you for using Hospital Management System!");
                    return;
                default:
                    System.out.println("Invalid choice! Enter 1 or 2.");
            }
        }
    }

    private static void loginAndOpenMenu() {
        InputHelper.printHeader("LOGIN");
        String username = InputHelper.readString("Username: ");
        String password = InputHelper.readString("Password: ");

        User user = LoginService.login(username, password);
        if (user == null) {
            System.out.println("Wrong username or password!");
            return;
        }

        System.out.println("Welcome, " + user.getFullName() + "!");

        // Open the menu for this role. Each teammate builds their own menu class.
        switch (user.getRole()) {
            case "ADMIN":        AdminMenu.show(user);        break;
            case "DOCTOR":       DoctorMenu.show(user);       break;
            case "NURSE":        NurseMenu.show(user);        break;
            case "RECEPTIONIST": ReceptionistMenu.show(user); break;
            case "PHARMACIST":   PharmacistMenu.show(user);   break;
            default: System.out.println("Unknown role: " + user.getRole());
        }

        System.out.println("Logged out successfully.");
    }
}

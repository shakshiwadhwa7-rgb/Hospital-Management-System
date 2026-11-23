package com.hospital.doctor;

import com.hospital.model.User;
import com.hospital.util.InputHelper;

/**
 * Doctor module.
 * Each option below is a TODO for the teammate who owns this module.
 * Replace the "Coming soon" lines with real code (use a DoctorDAO class for SQL).
 */
public class DoctorMenu {

    public static void show(User user) {
        while (true) {
            InputHelper.printHeader("DOCTOR MENU");
            System.out.println("1. View Today's Appointments");
            System.out.println("2. View Patient History");
            System.out.println("3. Write Prescription");
            System.out.println("4. Mark Appointment Completed");
            System.out.println("5. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1: System.out.println("View Today's Appointments - Coming soon"); break;
                case 2: System.out.println("View Patient History - Coming soon"); break;
                case 3: System.out.println("Write Prescription - Coming soon"); break;
                case 4: System.out.println("Mark Appointment Completed - Coming soon"); break;
                case 5: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }
}

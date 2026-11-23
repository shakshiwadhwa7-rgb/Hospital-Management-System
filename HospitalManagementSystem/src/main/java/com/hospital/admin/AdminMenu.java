package com.hospital.admin;

import com.hospital.model.User;
import com.hospital.util.InputHelper;

/**
 * Admin module.
 * Each option below is a TODO for the teammate who owns this module.
 * Replace the "Coming soon" lines with real code (use a AdminDAO class for SQL).
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
                case 1: System.out.println("Add Staff Account - Coming soon"); break;
                case 2: System.out.println("View All Staff - Coming soon"); break;
                case 3: System.out.println("Deactivate Staff Account - Coming soon"); break;
                case 4: System.out.println("Add Doctor Details - Coming soon"); break;
                case 5: System.out.println("View All Doctors - Coming soon"); break;
                case 6: System.out.println("View Hospital Summary - Coming soon"); break;
                case 7: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }
}

package com.hospital.pharmacist;

import com.hospital.model.User;
import com.hospital.util.InputHelper;

/**
 * Pharmacist module.
 * Each option below is a TODO for the teammate who owns this module.
 * Replace the "Coming soon" lines with real code (use a PharmacistDAO class for SQL).
 */
public class PharmacistMenu {

    public static void show(User user) {
        while (true) {
            InputHelper.printHeader("PHARMACIST MENU");
            System.out.println("1. View Pending Prescriptions");
            System.out.println("2. Dispense Medicines");
            System.out.println("3. Add New Medicine");
            System.out.println("4. Update Medicine Stock");
            System.out.println("5. View All Medicines");
            System.out.println("6. View Low Stock / Expiring Medicines");
            System.out.println("7. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1: System.out.println("View Pending Prescriptions - Coming soon"); break;
                case 2: System.out.println("Dispense Medicines - Coming soon"); break;
                case 3: System.out.println("Add New Medicine - Coming soon"); break;
                case 4: System.out.println("Update Medicine Stock - Coming soon"); break;
                case 5: System.out.println("View All Medicines - Coming soon"); break;
                case 6: System.out.println("View Low Stock / Expiring Medicines - Coming soon"); break;
                case 7: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }
}

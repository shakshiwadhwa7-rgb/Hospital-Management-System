package com.hospital.nurse;

import com.hospital.model.User;
import com.hospital.util.InputHelper;

/**
 * Nurse module.
 * Each option below is a TODO for the teammate who owns this module.
 * Replace the "Coming soon" lines with real code (use a NurseDAO class for SQL).
 */
public class NurseMenu {

    public static void show(User user) {
        while (true) {
            InputHelper.printHeader("NURSE MENU");
            System.out.println("1. View Available Beds");
            System.out.println("2. Allot Bed to Patient");
            System.out.println("3. Discharge Patient");
            System.out.println("4. Record Patient Vitals");
            System.out.println("5. View Patient Vitals");
            System.out.println("6. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1: System.out.println("View Available Beds - Coming soon"); break;
                case 2: System.out.println("Allot Bed to Patient - Coming soon"); break;
                case 3: System.out.println("Discharge Patient - Coming soon"); break;
                case 4: System.out.println("Record Patient Vitals - Coming soon"); break;
                case 5: System.out.println("View Patient Vitals - Coming soon"); break;
                case 6: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }
}

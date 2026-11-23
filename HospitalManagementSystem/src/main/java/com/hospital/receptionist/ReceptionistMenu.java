package com.hospital.receptionist;

import com.hospital.model.User;
import com.hospital.util.InputHelper;

/**
 * Receptionist module.
 * Each option below is a TODO for the teammate who owns this module.
 * Replace the "Coming soon" lines with real code (use a ReceptionistDAO class for SQL).
 */
public class ReceptionistMenu {

    public static void show(User user) {
        while (true) {
            InputHelper.printHeader("RECEPTIONIST MENU");
            System.out.println("1. Register New Patient");
            System.out.println("2. View All Patients");
            System.out.println("3. Search Patient");
            System.out.println("4. Book Appointment");
            System.out.println("5. Cancel Appointment");
            System.out.println("6. Generate Bill");
            System.out.println("7. Logout");
            int choice = InputHelper.readInt("Enter choice: ");

            switch (choice) {
                case 1: System.out.println("Register New Patient - Coming soon"); break;
                case 2: System.out.println("View All Patients - Coming soon"); break;
                case 3: System.out.println("Search Patient - Coming soon"); break;
                case 4: System.out.println("Book Appointment - Coming soon"); break;
                case 5: System.out.println("Cancel Appointment - Coming soon"); break;
                case 6: System.out.println("Generate Bill - Coming soon"); break;
                case 7: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }
}

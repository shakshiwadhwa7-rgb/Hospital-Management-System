package com.hospital.pharmacist;

import com.hospital.model.User;
import com.hospital.util.InputHelper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Pharmacist module: menu and user input.
 * SQL queries are in PharmacistDAO.
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
                case 1:
                    InputHelper.printHeader("PENDING PRESCRIPTIONS");
                    PharmacistDAO.viewPendingPrescriptions();
                    break;
                case 2: dispense(); break;
                case 3: addMedicine(); break;
                case 4: updateStock(); break;
                case 5:
                    InputHelper.printHeader("ALL MEDICINES");
                    PharmacistDAO.viewMedicines(false);
                    break;
                case 6:
                    InputHelper.printHeader("LOW STOCK / EXPIRING (stock < "
                            + PharmacistDAO.LOW_STOCK_LIMIT + " or expiry within "
                            + PharmacistDAO.EXPIRY_ALERT_DAYS + " days)");
                    if (PharmacistDAO.viewMedicines(true) == 0) {
                        System.out.println("All medicines are fine.");
                    }
                    break;
                case 7: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }

    // ---------------------------------------------------------
    // 2. Dispense
    // ---------------------------------------------------------
    private static void dispense() {
        InputHelper.printHeader("DISPENSE MEDICINES");

        if (PharmacistDAO.viewPendingPrescriptions() == 0) {
            return;
        }

        int prescriptionId = InputHelper.readInt("Enter Prescription ID to dispense (0 to go back): ");
        if (prescriptionId == 0) {
            return;
        }

        System.out.println();
        int check = PharmacistDAO.checkPrescription(prescriptionId);
        if (check == 0) {
            System.out.println("No PENDING prescription with ID " + prescriptionId);
            return;
        }
        if (check == -1) {
            System.out.println("Cannot dispense: some medicine is EXPIRED or LOW in stock.");
            System.out.println("Update the stock first (option 4).");
            return;
        }

        int confirm = InputHelper.readInt("Give these medicines to the patient? 1. Yes  2. No : ");
        if (confirm != 1) {
            System.out.println("Cancelled.");
            return;
        }

        if (PharmacistDAO.dispense(prescriptionId)) {
            System.out.println("Medicines dispensed. Stock updated and prescription marked DISPENSED.");
            System.out.println("The Receptionist will now include these medicines in the bill.");
        }
    }

    // ---------------------------------------------------------
    // 3. Add new medicine
    // ---------------------------------------------------------
    private static void addMedicine() {
        InputHelper.printHeader("ADD NEW MEDICINE");

        String name = InputHelper.readString("Medicine name (e.g. Ibuprofen 400mg): ");
        String manufacturer = InputHelper.readString("Manufacturer: ");

        double price = InputHelper.readDouble("Price per unit (Rs): ");
        if (price <= 0) {
            System.out.println("Price must be more than 0.");
            return;
        }

        int stock = InputHelper.readInt("Opening stock quantity: ");
        if (stock < 0) {
            System.out.println("Stock cannot be negative.");
            return;
        }

        LocalDate expiry;
        try {
            expiry = LocalDate.parse(InputHelper.readString("Expiry date (YYYY-MM-DD): "));
        } catch (DateTimeParseException e) {
            System.out.println("Wrong date format. Use YYYY-MM-DD, e.g. 2028-03-31.");
            return;
        }
        if (!expiry.isAfter(LocalDate.now())) {
            System.out.println("Cannot add a medicine that is already expired.");
            return;
        }

        int newId = PharmacistDAO.addMedicine(name, manufacturer, price, stock, expiry);
        if (newId != -1) {
            System.out.println("Medicine added! Medicine ID: " + newId);
        } else {
            System.out.println("Medicine was NOT added.");
        }
    }

    // ---------------------------------------------------------
    // 4. Update stock
    // ---------------------------------------------------------
    private static void updateStock() {
        InputHelper.printHeader("UPDATE MEDICINE STOCK");
        PharmacistDAO.viewMedicines(false);

        int medicineId = InputHelper.readInt("Enter Medicine ID (0 to go back): ");
        if (medicineId == 0) {
            return;
        }

        int qty = InputHelper.readInt("Quantity received (to add to stock): ");
        if (qty <= 0) {
            System.out.println("Quantity must be more than 0.");
            return;
        }

        if (PharmacistDAO.addStock(medicineId, qty)) {
            System.out.println("Stock updated: +" + qty);
        } else {
            System.out.println("No medicine with ID " + medicineId);
        }
    }
}

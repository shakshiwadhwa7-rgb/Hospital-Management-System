package com.hospital.util;

import java.util.Scanner;

/**
 * Safe console input for every module.
 * Use these methods instead of making your own Scanner,
 * so wrong input (like "abc" for a number) never crashes the program.
 */
public class InputHelper {

    private static final Scanner sc = new Scanner(System.in);

    // Reads a full line of text (never empty)
    public static String readString(String message) {
        while (true) {
            System.out.print(message);
            String text = sc.nextLine().trim();
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("This field cannot be empty. Try again.");
        }
    }

    // Reads a whole number
    public static int readInt(String message) {
        while (true) {
            System.out.print(message);
            String text = sc.nextLine().trim();
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }

    // Reads a number with decimals (price, fee, temperature)
    public static double readDouble(String message) {
        while (true) {
            System.out.print(message);
            String text = sc.nextLine().trim();
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number like 250 or 99.5");
            }
        }
    }

    // Prints a heading like  ===== ADMIN MENU =====
    public static void printHeader(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}

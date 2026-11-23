package com.hospital.model;

/**
 * One row of the "users" table: the person who is logged in.
 */
public class User {

    private int userId;
    private String username;
    private String role;      // ADMIN, DOCTOR, NURSE, RECEPTIONIST, PHARMACIST
    private String fullName;

    public User(int userId, String username, String role, String fullName) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }

    public int getUserId()      { return userId; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
    public String getFullName() { return fullName; }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}

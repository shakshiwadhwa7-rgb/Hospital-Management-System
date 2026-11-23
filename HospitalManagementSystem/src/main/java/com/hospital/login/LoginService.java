package com.hospital.login;

import com.hospital.db.DBConnection;
import com.hospital.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Checks username + password against the "users" table.
 */
public class LoginService {

    /**
     * @return the logged-in User, or null if username/password is wrong
     */
    public static User login(String username, String password) {
        String sql = "SELECT user_id, username, role, full_name FROM users "
                   + "WHERE username = ? AND password = ? AND is_active = TRUE";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("full_name")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }
}

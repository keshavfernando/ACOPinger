package org.example;
import java.sql.*;

public class DatabaseManager
{

    private static final String DB_URL = "jdbc:sqlite:data/user_data.db";

    public DatabaseManager()
    {
        createUserTable();
    }

    private void createUserTable()
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                discord_id TEXT NOT NULL,
                profile_name TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL
            );
        """;


        try (Connection conn = DriverManager.getConnection(DB_URL);
        Statement stmt = conn.createStatement()){
            stmt.execute(sql);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

    }

    public boolean insertUser(String discordId, String profileName, String email)
    {
        String sql = "INSERT INTO users (discord_id, profile_name, email) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement pststmt = conn.prepareStatement(sql))
        {
            pststmt.setString(1, discordId);
            pststmt.setString(2, profileName);
            pststmt.setString(3, email);
            pststmt.executeUpdate();
            return true;
        }
        catch (Exception e)
        {
            System.out.println("Insert failed: " + e.getMessage());
            return false;
        }
    }

    public String getDiscordIDbyProfile(String profileName)
    {
        String sql = "SELECT discord_id FROM users WHERE profile_name = ?";
        return getValueFromQuery(sql, profileName);
    }

    public String getDiscordIDbyEmail(String email)
    {
        String sql = "SELECT discord_id FROM users WHERE email = ?";
        return getValueFromQuery(sql, email);
    }

    private String getValueFromQuery(String sql, String value)
    {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, value);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
        } catch (SQLException e) {
            System.out.println("Lookup failed: " + e.getMessage());
        }
        return null;
    }
}

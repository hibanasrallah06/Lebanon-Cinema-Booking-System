package cinemasystem.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = System.getenv().getOrDefault(
            "CINEMA_DB_URL",
            "jdbc:mysql://localhost:3306/cinema_booking_system"
    );

    private static final String USER = System.getenv().getOrDefault(
            "CINEMA_DB_USER",
            "root"
    );

    private static final String PASSWORD = System.getenv().getOrDefault(
            "CINEMA_DB_PASSWORD",
            ""
    );

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Database Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }
}

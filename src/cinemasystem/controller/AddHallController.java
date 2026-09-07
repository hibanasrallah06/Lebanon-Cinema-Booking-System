package cinemasystem.controller;

import cinemasystem.database.DBConnection;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddHallController {

    @FXML
    private TextField txtHallName;

    @FXML
    private TextField txtCapacity;

    private int cinemaId;

    // Receive the selected cinema
    public void setCinemaId(int cinemaId) {
        this.cinemaId = cinemaId;
    }

    // =========================
    // ADD HALL
    // =========================

    @FXML
    private void handleAdd() {

        String hallName = txtHallName.getText().trim();
        String capacityText = txtCapacity.getText().trim();

        // Check empty fields
        if (hallName.isEmpty() || capacityText.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Hall",
                    "Please fill in all fields."
            );

            return;
        }

        int capacity;

        // Check capacity
        try {

            capacity = Integer.parseInt(capacityText);

        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Capacity",
                    "Please enter a valid number for capacity."
            );

            return;
        }

        // Capacity must be positive
        if (capacity <= 0) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Capacity",
                    "Capacity must be greater than 0."
            );

            return;
        }

        // Check cinema
        if (cinemaId <= 0) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Add Hall",
                    "No cinema was selected."
            );

            return;
        }

        String sql =
                "INSERT INTO hall " +
                "(hall_name, capacity, cinema_id) " +
                "VALUES (?, ?, ?)";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, hallName);
            statement.setInt(2, capacity);
            statement.setInt(3, cinemaId);

            statement.executeUpdate();

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Hall added successfully."
            );

            closeWindow();

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not add the hall."
            );
        }
    }

    // =========================
    // CANCEL
    // =========================

    @FXML
    private void handleCancel() {

        closeWindow();
    }

    // =========================
    // CLOSE WINDOW
    // =========================

    private void closeWindow() {

        Stage stage =
                (Stage) txtHallName
                        .getScene()
                        .getWindow();

        stage.close();
    }

    // =========================
    // ALERT
    // =========================

    private void showAlert(
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert =
                new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package cinemasystem.controller;

import cinemasystem.database.DBConnection;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EditHallController {

    @FXML
    private TextField txtHallName;

    @FXML
    private TextField txtCapacity;

    private int hallId;


    // =========================
    // RECEIVE SELECTED HALL
    // =========================

    public void setHall(
            int hallId,
            String hallName,
            int capacity) {

        this.hallId = hallId;

        txtHallName.setText(hallName);
        txtCapacity.setText(
                String.valueOf(capacity)
        );
    }


    // =========================
    // UPDATE HALL
    // =========================

    @FXML
    private void handleUpdate() {

        String hallName =
                txtHallName.getText().trim();

        String capacityText =
                txtCapacity.getText().trim();


        // Check empty fields
        if (hallName.isEmpty() ||
            capacityText.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Hall",
                    "Please fill in all fields."
            );

            return;
        }


        int capacity;

        try {

            capacity =
                    Integer.parseInt(capacityText);

        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Capacity",
                    "Please enter a valid number."
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


        String sql =
                "UPDATE hall " +
                "SET hall_name = ?, " +
                "capacity = ? " +
                "WHERE hall_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    hallName
            );

            statement.setInt(
                    2,
                    capacity
            );

            statement.setInt(
                    3,
                    hallId
            );


            int rows =
                    statement.executeUpdate();


            if (rows > 0) {

                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Hall updated successfully."
                );

                closeWindow();

            } else {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Could not update the hall."
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not update the hall."
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
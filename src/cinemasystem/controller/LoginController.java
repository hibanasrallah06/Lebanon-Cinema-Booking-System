package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.util.Session;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class LoginController {


    @FXML
    private TextField txtUsername;


    @FXML
    private PasswordField txtPassword;


    // =========================================================
    // LOGIN
    // =========================================================

    @FXML
    private void handleLogin(ActionEvent event) {

        String username =
                txtUsername.getText();

        String password =
                txtPassword.getText();


        // =====================================================
        // VALIDATION
        // =====================================================

        if (username.isEmpty() ||
            password.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Login",
                    "Please enter username and password."
            );

            return;
        }


        // =====================================================
        // LOGIN QUERY
        // =====================================================

        String sql =
                "SELECT " +
                "employee_id, " +
                "full_name, " +
                "role, " +
                "cinema_Id " +
                "FROM employee " +
                "WHERE username = ? " +
                "AND password = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {


            statement.setString(
                    1,
                    username
            );

            statement.setString(
                    2,
                    password
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {


                if (!result.next()) {

                    showAlert(
                            Alert.AlertType.ERROR,
                            "Login Failed",
                            "Invalid username or password."
                    );

                    return;
                }


                // =================================================
                // GET USER DATA
                // =================================================

                int employeeId =
                        result.getInt(
                                "employee_id"
                        );

                String fullName =
                        result.getString(
                                "full_name"
                        );

                String role =
                        result.getString(
                                "role"
                        );

                int cinemaId =
                        result.getInt(
                                "cinema_Id"
                        );


                // =================================================
                // SAVE SESSION
                // =================================================

                Session.setEmployee(
                        employeeId,
                        fullName,
                        role,
                        cinemaId
                );


                // =================================================
                // UPDATE LAST LOGIN
                // =================================================

                String updateLogin =
                        "UPDATE employee " +
                        "SET last_login = CURRENT_TIMESTAMP " +
                        "WHERE employee_id = ?";


                try (
                        PreparedStatement loginStatement =
                                connection.prepareStatement(
                                        updateLogin
                                )
                ) {

                    loginStatement.setInt(
                            1,
                            employeeId
                    );

                    loginStatement.executeUpdate();
                }


                // =================================================
                // OPEN DASHBOARD
                // =================================================

                FXMLLoader loader =
                        new FXMLLoader(
                                getClass().getResource(
                                        "/cinemasystem/view/Dashboard.fxml"
                                )
                        );


                Parent root =
                        loader.load();


                DashboardController controller =
                        loader.getController();


                controller.setEmployeeName(
                        fullName
                );


                Stage stage =
                        (Stage) ((Node) event.getSource())
                                .getScene()
                                .getWindow();


                stage.setScene(
                        new Scene(root)
                );

                stage.setTitle(
                        "Cinema Dashboard"
                );

                stage.show();
            }


        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not connect to the database.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // ALERT
    // =========================================================

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
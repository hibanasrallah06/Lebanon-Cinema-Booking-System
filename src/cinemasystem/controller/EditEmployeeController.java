package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.util.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class EditEmployeeController {

    // =========================
    // FIELDS
    // =========================

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtPhone;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private ComboBox<String> cmbRole;

    @FXML
    private ComboBox<CinemaItem> cmbCinema;


    // =========================
    // CINEMA LIST
    // =========================

    private ObservableList<CinemaItem> cinemaList =
            FXCollections.observableArrayList();


    // =========================
    // EMPLOYEE CONTROLLER
    // =========================

    private EmployeeController employeeController;


    // =========================
    // EMPLOYEE ID
    // =========================

    private int employeeId;


    // =========================
    // SET CONTROLLER
    // =========================

    public void setEmployeeController(
            EmployeeController controller) {

        this.employeeController = controller;
    }


    // =========================
    // INITIALIZE
    // =========================

    @FXML
public void initialize() {

    String role = Session.getRole();

    if (role != null && role.trim().equalsIgnoreCase("manager")) {

        cmbRole.getItems().add("Employee");

    } else {

        cmbRole.getItems().addAll(
                "Admin",
                "Manager",
                "Employee"
        );
    }

    loadCinemas();

    cmbCinema.setItems(cinemaList);
}


    // =========================
    // RECEIVE EMPLOYEE
    // =========================

    public void setEmployee(
            EmployeeController.EmployeeItem employee) {

        employeeId =
                employee.getEmployeeId();


        txtFullName.setText(
                employee.getFullName()
        );


        txtEmail.setText(
                employee.getEmail()
        );


        txtPhone.setText(
                employee.getPhone()
        );


        cmbRole.setValue(
                employee.getRole()
        );


        // Select current cinema

        for (CinemaItem cinema : cinemaList) {

            if (cinema.getCinemaId()
                    == employee.getCinemaId()) {

                cmbCinema.setValue(cinema);

                break;
            }
        }
    }


    // =========================
    // LOAD CINEMAS
    // =========================

    private void loadCinemas() {

    String role = Session.getRole();

    if (role == null) {
        role = "";
    }

    role = role.trim().toLowerCase();

    String sql;

    if (role.equals("manager")) {

        sql =
                "SELECT cinema_id, cinema_name " +
                "FROM cinema " +
                "WHERE cinema_id = ? " +
                "ORDER BY cinema_name";

    } else {

        sql =
                "SELECT cinema_id, cinema_name " +
                "FROM cinema " +
                "ORDER BY cinema_name";
    }

    try (
            Connection connection =
                    DBConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        if (role.equals("manager")) {

            statement.setInt(
                    1,
                    Session.getCinemaId()
            );
        }

        try (ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                cinemaList.add(
                        new CinemaItem(
                                result.getInt("cinema_id"),
                                result.getString("cinema_name")
                        )
                );
            }
        }

    } catch (SQLException e) {

        e.printStackTrace();

        showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Could not load cinemas."
        );
    }
}


    // =========================
    // UPDATE EMPLOYEE
    // =========================

    @FXML
    private void handleUpdate() {

        String fullName =
                txtFullName.getText().trim();

        String email =
                txtEmail.getText().trim();

        String phone =
                txtPhone.getText().trim();

        String newPassword =
                txtPassword.getText().trim();


        // =========================
        // VALIDATION
        // =========================

        if (fullName.isEmpty()
                || email.isEmpty()
                || phone.isEmpty()
                || cmbRole.getValue() == null
                || cmbCinema.getValue() == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Employee",
                    "Please fill in all required fields."
            );

            return;
        }


        // =========================
        // EMAIL VALIDATION
        // =========================

        if (!email.contains("@")
                || !email.contains(".")) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Email",
                    "Please enter a valid email address."
            );

            return;
        }


        String role =
                cmbRole.getValue();


        int cinemaId =
                cmbCinema.getValue()
                        .getCinemaId();


        /*
         * Username is no longer displayed.
         *
         * Your database still requires username,
         * so we keep it internally equal to email.
         */

        String username = email;


        // =========================
        // SQL
        // =========================

        String sql;


        boolean changePassword =
                !newPassword.isEmpty();


        if (changePassword) {

            sql =
                    "UPDATE employee " +
                    "SET full_name = ?, " +
                    "email = ?, " +
                    "phone = ?, " +
                    "username = ?, " +
                    "password = ?, " +
                    "role = ?, " +
                    "cinema_Id = ? " +
                    "WHERE employee_id = ?";

        } else {

            sql =
                    "UPDATE employee " +
                    "SET full_name = ?, " +
                    "email = ?, " +
                    "phone = ?, " +
                    "username = ?, " +
                    "role = ?, " +
                    "cinema_Id = ? " +
                    "WHERE employee_id = ?";
        }


        // =========================
        // EXECUTE UPDATE
        // =========================

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {


            if (changePassword) {

                statement.setString(
                        1,
                        fullName
                );

                statement.setString(
                        2,
                        email
                );

                statement.setString(
                        3,
                        phone
                );

                statement.setString(
                        4,
                        username
                );

                statement.setString(
                        5,
                        newPassword
                );

                statement.setString(
                        6,
                        role
                );

                statement.setInt(
                        7,
                        cinemaId
                );

                statement.setInt(
                        8,
                        employeeId
                );

            } else {

                statement.setString(
                        1,
                        fullName
                );

                statement.setString(
                        2,
                        email
                );

                statement.setString(
                        3,
                        phone
                );

                statement.setString(
                        4,
                        username
                );

                statement.setString(
                        5,
                        role
                );

                statement.setInt(
                        6,
                        cinemaId
                );

                statement.setInt(
                        7,
                        employeeId
                );
            }


            statement.executeUpdate();


            // =========================
            // REFRESH EMPLOYEE TABLE
            // =========================

            if (employeeController != null) {

                employeeController.loadEmployees();
            }


            // =========================
            // SUCCESS
            // =========================

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Employee updated successfully."
            );


            closeWindow();


        } catch (SQLException e) {

            e.printStackTrace();


            String error =
                    e.getMessage() == null
                            ? ""
                            : e.getMessage().toLowerCase();


            if (error.contains("duplicate")
                    || error.contains("unique")) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Email Already Exists",
                        "This email is already used.\n"
                        + "Please choose another email address."
                );

            } else {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Could not update employee.\n\n"
                        + e.getMessage()
                );
            }
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
    // CLOSE
    // =========================

    private void closeWindow() {

        if (txtFullName == null
                || txtFullName.getScene() == null) {

            return;
        }


        Stage stage =
                (Stage)
                        txtFullName
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


    // =========================
    // CINEMA ITEM
    // =========================

    public static class CinemaItem {

        private final int cinemaId;

        private final String cinemaName;


        public CinemaItem(
                int cinemaId,
                String cinemaName) {

            this.cinemaId = cinemaId;

            this.cinemaName = cinemaName;
        }


        public int getCinemaId() {

            return cinemaId;
        }


        public String getCinemaName() {

            return cinemaName;
        }


        @Override
        public String toString() {

            return cinemaName;
        }
    }
}
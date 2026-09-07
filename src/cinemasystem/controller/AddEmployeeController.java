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


public class AddEmployeeController {

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
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private ComboBox<String> cmbRole;

    @FXML
    private ComboBox<CinemaItem> cmbCinema;


    // =========================
    // CINEMA LIST
    // =========================

    private final ObservableList<CinemaItem> cinemaList =
            FXCollections.observableArrayList();


    // =========================
    // EMPLOYEE CONTROLLER
    // =========================

    private EmployeeController employeeController;


    // =========================
    // SET EMPLOYEE CONTROLLER
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

        if (role != null
                && role.trim().equalsIgnoreCase("manager")) {

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

            try (ResultSet result =
                         statement.executeQuery()) {

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
    // SAVE EMPLOYEE
    // =========================

    @FXML
    private void handleSave() {

        String fullName =
                txtFullName.getText().trim();

        String email =
                txtEmail.getText().trim();

        String phone =
                txtPhone.getText().trim();

        String username =
                txtUsername.getText().trim();

        String password =
                txtPassword.getText().trim();


        // =========================
        // VALIDATION
        // =========================

        if (fullName.isEmpty()
                || email.isEmpty()
                || phone.isEmpty()
                || username.isEmpty()
                || password.isEmpty()
                || cmbRole.getValue() == null
                || cmbCinema.getValue() == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Employee",
                    "Please fill in all fields."
            );

            return;
        }


        // =========================
        // EMAIL VALIDATION
        // =========================

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Email",
                    "Please enter a valid email address."
            );

            return;
        }


        // =========================
        // PHONE VALIDATION
        // =========================

        if (!phone.matches("\\d+")) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Phone",
                    "Phone number must contain digits only."
            );

            return;
        }


        // =========================
        // USERNAME VALIDATION
        // =========================

        if (!username.matches("^[A-Za-z0-9_]+$")) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Username",
                    "Username can contain letters, numbers, and underscore only."
            );

            return;
        }


        // =========================
        // ROLE
        // =========================

        String role =
                cmbRole.getValue();


        // =========================
        // CINEMA
        // =========================

        int cinemaId =
                cmbCinema.getValue()
                        .getCinemaId();


        // =========================
        // INSERT
        // =========================

        String sql =
                "INSERT INTO employee " +
                "(full_name, email, phone, username, password, role, cinema_Id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

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
                    password
            );

            statement.setString(
                    6,
                    role
            );

            statement.setInt(
                    7,
                    cinemaId
            );


            statement.executeUpdate();


            // =========================
            // REFRESH TABLE
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
                    "Employee added successfully."
            );


            closeWindow();


        } catch (SQLException e) {

            e.printStackTrace();

            String error =
                    e.getMessage() == null
                            ? ""
                            : e.getMessage().toLowerCase();


            // =========================
            // DUPLICATE
            // =========================

            if (error.contains("duplicate")
                    || error.contains("unique")) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Already Exists",
                        "This email or username is already used."
                );

            } else {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Could not add employee.\n\n"
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
    // CLOSE WINDOW
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
package cinemasystem.controller;

import cinemasystem.database.DBConnection;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class NewCustomerController {

    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private TextField txtCustomerName;

    @FXML
    private TextField txtCustomerPhone;

    @FXML
    private TextField txtCustomerEmail;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnCancel;
    
    private BookingController bookingController;
    
    public void setBookingController(
        BookingController bookingController) {

    this.bookingController = bookingController;
    }


    // =========================================================
    // SAVE CUSTOMER
    // =========================================================

    @FXML
    private void handleSave() {

        // -----------------------------------------------------
        // GET VALUES
        // -----------------------------------------------------

        String name =
                txtCustomerName.getText().trim();

        String phone =
                txtCustomerPhone.getText().trim();

        String email =
                txtCustomerEmail.getText().trim();
        
        
        // -----------------------------------------------------
        // CHECK REQUIRED NAME
        // -----------------------------------------------------

        if (name.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Customer",
                    "Please enter the customer name."
            );

            txtCustomerName.requestFocus();

            return;
        }


        // -----------------------------------------------------
        // CHECK PHONE
        // -----------------------------------------------------

        if (phone.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Customer",
                    "Please enter the customer phone."
            );

            txtCustomerPhone.requestFocus();

            return;
        }


        // -----------------------------------------------------
        // CHECK EMAIL
        // -----------------------------------------------------

        if (email.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Customer",
                    "Please enter the customer email."
            );

            txtCustomerEmail.requestFocus();

            return;
        }


        // -----------------------------------------------------
        // BASIC EMAIL VALIDATION
        // -----------------------------------------------------

        if (!email.contains("@") ||
            !email.contains(".")) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Email",
                    "Please enter a valid email address."
            );

            txtCustomerEmail.requestFocus();

            return;
        }


        // =====================================================
        // CHECK IF CUSTOMER ALREADY EXISTS
        // =====================================================

        String checkSql =
                "SELECT customer_id " +
                "FROM customer " +
                "WHERE customer_phone = ? " +
                "OR customer_email = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(checkSql)
        ) {

            statement.setString(
                    1,
                    phone
            );

            statement.setString(
                    2,
                    email
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    showAlert(
                            Alert.AlertType.WARNING,
                            "Customer Already Exists",
                            "A customer with this phone number " +
                            "or email already exists."
                    );
                    
                    return;
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not check the customer.\n\n"
                    + e.getMessage()
            );

            return;
        }


        // =====================================================
        // INSERT CUSTOMER
        // =====================================================

        String sql =
                "INSERT INTO customer " +
                "(customer_name, customer_phone, customer_email) " +
                "VALUES (?, ?, ?)";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    name
            );

            statement.setString(
                    2,
                    phone
            );

            statement.setString(
                    3,
                    email
            );


            int rows =
                    statement.executeUpdate();


            // -------------------------------------------------
            // CHECK INSERT
            // -------------------------------------------------

            if (rows > 0) {
                
                if (bookingController != null) {
                    bookingController.reloadCustomers();
                }

                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Customer added successfully."
                );

                closeWindow();
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not add the customer.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void handleCancel() {

        closeWindow();
    }


    // =========================================================
    // CLOSE WINDOW
    // =========================================================

    private void closeWindow() {

        Stage stage =
                (Stage) txtCustomerName
                        .getScene()
                        .getWindow();

        stage.close();
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
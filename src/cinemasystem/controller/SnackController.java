package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Snack;
import cinemasystem.util.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Locale;


/**
 * Snack Management Controller
 *
 * Handles:
 * - Loading snacks from database
 * - Adding snacks
 * - Updating snacks
 * - Activating / deactivating snacks
 * - Searching snacks
 * - Displaying snacks as beautiful cards
 */
public class SnackController {

    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private TextField txtSearch;

    @FXML
    private TextField txtSnackName;

    @FXML
    private TextField txtPrice;

    @FXML
    private CheckBox chkActive;

    @FXML
    private FlowPane snacksPane;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnClear;

    @FXML
    private Button btnToggleStatus;


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<Snack> snacks =
            FXCollections.observableArrayList();

    private Snack selectedSnack = null;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    private void initialize() {

        chkActive.setSelected(true);

        setupSearch();

        loadSnacks();

        displaySnacks();
    }


    // =========================================================
    // LOAD SNACKS
    // =========================================================

    private void loadSnacks() {

        snacks.clear();

        String sql =
                "SELECT snack_id, snack_name, price, active " +
                "FROM snack " +
                "ORDER BY snack_name";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                Snack snack =
                        new Snack(
                                result.getInt("snack_id"),
                                result.getString("snack_name"),
                                result.getDouble("price"),
                                result.getBoolean("active")
                        );

                snacks.add(snack);
            }

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load snacks.\n\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // DISPLAY SNACK CARDS
    // =========================================================

    private void displaySnacks() {

        if (snacksPane == null) {
            return;
        }

        snacksPane.getChildren().clear();

        String search = "";

        if (txtSearch != null) {

            search =
                    txtSearch.getText()
                            .trim()
                            .toLowerCase();
        }


        for (Snack snack : snacks) {

            if (!search.isEmpty()
                    && !snack.getSnackName()
                            .toLowerCase()
                            .contains(search)) {

                continue;
            }

            snacksPane.getChildren().add(
                    createSnackCard(snack)
            );
        }
    }


    // =========================================================
    // CREATE SNACK CARD
    // =========================================================

    private VBox createSnackCard(Snack snack) {

        VBox card =
                new VBox(10);

        card.setPrefWidth(260);
        card.setMinHeight(220);

        card.setStyle(
                "-fx-background-color:white;" +
                "-fx-background-radius:14;" +
                "-fx-padding:18;" +
                "-fx-effect:dropshadow(" +
                "gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                "-fx-cursor:hand;"
        );


        // =====================================================
        // ICON
        // =====================================================

        Label icon =
                new Label(
                        getSnackIcon(
                                snack.getSnackName()
                        )
                );

        icon.setStyle(
                "-fx-font-size:38px;"
        );


        // =====================================================
        // NAME
        // =====================================================

        Label name =
                new Label(
                        snack.getSnackName()
                );

        name.setWrapText(true);

        name.setStyle(
                "-fx-font-size:19px;" +
                "-fx-font-weight:bold;" +
                "-fx-text-fill:#222222;"
        );


        // =====================================================
        // PRICE
        // =====================================================

        Label price =
                new Label(
                        String.format(
                                Locale.US,
                                "%.2f $",
                                snack.getPrice()
                        )
                );

        price.setStyle(
                "-fx-font-size:22px;" +
                "-fx-font-weight:bold;" +
                "-fx-text-fill:#2E7D32;"
        );


        // =====================================================
        // STATUS
        // =====================================================

        Label status =
                new Label(
                        snack.isActive()
                                ? "● Available"
                                : "● Unavailable"
                );

        status.setStyle(
                snack.isActive()
                        ? "-fx-text-fill:#2E7D32;" +
                          "-fx-font-weight:bold;"
                        : "-fx-text-fill:#C62828;" +
                          "-fx-font-weight:bold;"
        );


        // =====================================================
        // SPACER
        // =====================================================

        Region spacer =
                new Region();

        VBox.setVgrow(
                spacer,
                javafx.scene.layout.Priority.ALWAYS
        );


        // =====================================================
        // EDIT BUTTON
        // =====================================================

        Button edit =
                new Button("✏ Edit Snack");

        edit.setMaxWidth(
                Double.MAX_VALUE
        );

        edit.setPrefHeight(38);

        edit.setStyle(
                "-fx-background-color:#F1F1F1;" +
                "-fx-text-fill:#333333;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:8;" +
                "-fx-cursor:hand;"
        );


        edit.setOnAction(event -> {

            selectSnack(snack);

            txtSnackName.requestFocus();
        });


        // =====================================================
        // CARD CLICK
        // =====================================================

        card.setOnMouseClicked(event -> {

            if (event.getTarget() != edit) {

                selectSnack(snack);
            }
        });


        // =====================================================
        // ADD ELEMENTS
        // =====================================================

        card.getChildren().addAll(
                icon,
                name,
                price,
                status,
                spacer,
                edit
        );


        return card;
    }


    // =========================================================
    // SNACK ICON
    // =========================================================

    private String getSnackIcon(String name) {

        if (name == null) {
            return "🍴";
        }

        String value =
                name.toLowerCase();


        if (value.contains("popcorn")) {
            return "🍿";
        }

        if (value.contains("pepsi")
                || value.contains("cola")
                || value.contains("drink")
                || value.contains("juice")
                || value.contains("soda")) {

            return "🥤";
        }

        if (value.contains("hot dog")
                || value.contains("hotdog")) {

            return "🌭";
        }

        if (value.contains("nachos")) {
            return "🧀";
        }

        if (value.contains("chocolate")) {
            return "🍫";
        }

        if (value.contains("water")) {
            return "💧";
        }

        if (value.contains("coffee")) {
            return "☕";
        }

        if (value.contains("ice cream")
                || value.contains("icecream")) {

            return "🍦";
        }

        if (value.contains("burger")) {
            return "🍔";
        }

        return "🍴";
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        txtSearch.textProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                displaySnacks()
                );
    }


    // =========================================================
    // SELECT SNACK
    // =========================================================

    private void selectSnack(Snack snack) {

        if (snack == null) {
            return;
        }

        selectedSnack = snack;


        txtSnackName.setText(
                snack.getSnackName()
        );


        txtPrice.setText(
                String.format(
                        Locale.US,
                        "%.2f",
                        snack.getPrice()
                )
        );


        chkActive.setSelected(
                snack.isActive()
        );


        btnSave.setText(
                "Update Snack"
        );


        btnToggleStatus.setText(
                snack.isActive()
                        ? "Deactivate"
                        : "Activate"
        );
    }


    // =========================================================
    // SAVE / UPDATE
    // =========================================================

    @FXML
    private void handleSave(ActionEvent event) {

        String name =
                txtSnackName.getText()
                        .trim();

        String priceText =
                txtPrice.getText()
                        .trim();


        if (name.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Name",
                    "Please enter the snack name."
            );

            return;
        }


        if (priceText.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Price",
                    "Please enter the snack price."
            );

            return;
        }


        double price;


        try {

            price =
                    Double.parseDouble(
                            priceText
                    );

        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Price",
                    "Please enter a valid price."
            );

            return;
        }


        if (price < 0) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Price",
                    "Price cannot be negative."
            );

            return;
        }


        boolean active =
                chkActive.isSelected();


        // =====================================================
        // UPDATE
        // =====================================================

        if (selectedSnack != null) {

            updateSnack(
                    selectedSnack.getSnackId(),
                    name,
                    price,
                    active
            );

        }

        // =====================================================
        // INSERT
        // =====================================================

        else {

            insertSnack(
                    name,
                    price,
                    active
            );
        }
    }


    // =========================================================
    // INSERT
    // =========================================================

    private void insertSnack(
            String name,
            double price,
            boolean active) {

        String sql =
                "INSERT INTO snack " +
                "(snack_name, price, active) " +
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

            statement.setDouble(
                    2,
                    price
            );

            statement.setBoolean(
                    3,
                    active
            );


            statement.executeUpdate();


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Snack added successfully."
            );


            clearForm();

            loadSnacks();

            displaySnacks();


        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not add snack.\n\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // UPDATE
    // =========================================================

    private void updateSnack(
            int snackId,
            String name,
            double price,
            boolean active) {

        String sql =
                "UPDATE snack " +
                "SET snack_name = ?, " +
                "price = ?, " +
                "active = ? " +
                "WHERE snack_id = ?";


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

            statement.setDouble(
                    2,
                    price
            );

            statement.setBoolean(
                    3,
                    active
            );

            statement.setInt(
                    4,
                    snackId
            );


            statement.executeUpdate();


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Snack updated successfully."
            );


            clearForm();

            loadSnacks();

            displaySnacks();


        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not update snack.\n\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================

    @FXML
    private void handleToggleStatus(
            ActionEvent event) {

        if (selectedSnack == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Selection",
                    "Please select a snack first."
            );

            return;
        }


        boolean newStatus =
                !selectedSnack.isActive();


        String sql =
                "UPDATE snack " +
                "SET active = ? " +
                "WHERE snack_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setBoolean(
                    1,
                    newStatus
            );

            statement.setInt(
                    2,
                    selectedSnack.getSnackId()
            );


            statement.executeUpdate();


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    newStatus
                            ? "Snack activated successfully."
                            : "Snack deactivated successfully."
            );


            clearForm();

            loadSnacks();

            displaySnacks();


        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not change snack status.\n\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // CLEAR
    // =========================================================

    @FXML
    private void handleClear(ActionEvent event) {

        clearForm();
    }


    private void clearForm() {

        selectedSnack = null;


        txtSnackName.clear();

        txtPrice.clear();

        chkActive.setSelected(true);


        btnSave.setText(
                "Save Snack"
        );


        btnToggleStatus.setText(
                "Activate / Deactivate"
        );
    }


    // =========================================================
    // BACK TO DASHBOARD
    // =========================================================

    @FXML
    private void handleBack(ActionEvent event) {

        try {

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
                    Session.getFullName()
            );


            Stage stage =
                    (Stage)
                            ((Node) event.getSource())
                                    .getScene()
                                    .getWindow();


            stage.setScene(
                    new Scene(root)
            );


            stage.setTitle(
                    "Cinema Dashboard"
            );


            stage.show();


        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Navigation Error",
                    "Could not return to dashboard.\n\n"
                            + e.getMessage()
            );

            e.printStackTrace();
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
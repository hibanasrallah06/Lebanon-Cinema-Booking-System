package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.util.Session;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import javafx.event.ActionEvent;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.control.ButtonType;


public class EmployeeController {

    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private FlowPane employeeContainer;

    @FXML
    private TextField txtSearch;


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<EmployeeItem> employeeList =
            FXCollections.observableArrayList();


    // =========================================================
    // EXECUTOR
    // =========================================================

    private final ExecutorService executor =
            Executors.newCachedThreadPool();


    // =========================================================
    // EMPLOYEE NAME
    // =========================================================

    private String employeeName;


    public void setEmployeeName(String fullName) {
        employeeName = fullName;
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadEmployees();

        setupSearch();
    }


    // =========================================================
    // LOAD EMPLOYEES
    // =========================================================

    public void loadEmployees() {

        employeeList.clear();

        String role = Session.getRole();

        if (role == null) {
            role = "";
        }

        role = role.trim().toLowerCase();


        String sql;


        // =====================================================
        // ADMIN
        // =====================================================

        if (role.equals("admin")) {

            sql =
                    "SELECT e.employee_id, " +
                    "e.full_name, " +
                    "e.email, " +
                    "e.phone, " +
                    "e.role, " +
                    "e.cinema_Id, " +
                    "e.last_login, " +
                    "c.cinema_name " +
                    "FROM employee e " +
                    "INNER JOIN cinema c " +
                    "ON e.cinema_Id = c.cinema_id " +
                    "ORDER BY e.employee_id";
        }


        // =====================================================
        // MANAGER
        // =====================================================

        else if (role.equals("manager")) {

            sql =
                    "SELECT e.employee_id, " +
                    "e.full_name, " +
                    "e.email, " +
                    "e.phone, " +
                    "e.role, " +
                    "e.cinema_Id, " +
                    "e.last_login, " +
                    "c.cinema_name " +
                    "FROM employee e " +
                    "INNER JOIN cinema c " +
                    "ON e.cinema_Id = c.cinema_id " +
                    "WHERE e.cinema_Id = ? " +
                    "AND LOWER(e.role) = 'employee' " +
                    "ORDER BY e.employee_id";
        }


        // =====================================================
        // OTHER ROLES
        // =====================================================

        else {

            displayEmployees();
            return;
        }


        // =====================================================
        // DATABASE
        // =====================================================

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {


            // =================================================
            // MANAGER CINEMA
            // =================================================

            if (role.equals("manager")) {

                statement.setInt(
                        1,
                        Session.getCinemaId()
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    Timestamp timestamp =
                            result.getTimestamp("last_login");


                    String lastLogin;


                    if (timestamp == null) {

                        lastLogin = "Never";

                    } else {

                        lastLogin =
                                timestamp.toString();
                    }


                    EmployeeItem employee =
                            new EmployeeItem(
                                    result.getInt("employee_id"),
                                    result.getString("full_name"),
                                    result.getString("email"),
                                    result.getString("phone"),
                                    result.getString("role"),
                                    result.getInt("cinema_Id"),
                                    result.getString("cinema_name"),
                                    lastLogin
                            );


                    employeeList.add(employee);
                }
            }


            displayEmployees();


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load employees.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        if (txtSearch == null) {
            return;
        }


        txtSearch.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    String searchText =
                            newValue == null
                                    ? ""
                                    : newValue.trim().toLowerCase();


                    displayEmployees(searchText);
                }
        );
    }


    // =========================================================
    // DISPLAY EMPLOYEES
    // =========================================================

    private void displayEmployees() {

        displayEmployees("");
    }


    private void displayEmployees(
            String searchText) {

        if (employeeContainer == null) {

            System.err.println(
                    "ERROR: employeeContainer is null. "
                    + "Check Employee.fxml "
                    + "fx:id=\"employeeContainer\"."
            );

            return;
        }


        employeeContainer.getChildren().clear();


        String search =
                searchText == null
                        ? ""
                        : searchText.trim().toLowerCase();


        for (EmployeeItem employee : employeeList) {

            if (!search.isEmpty()
                    && !matchesSearch(
                            employee,
                            search
                    )) {

                continue;
            }


            VBox card =
                    createEmployeeCard(employee);


            employeeContainer
                    .getChildren()
                    .add(card);
        }
    }


    // =========================================================
    // SEARCH MATCH
    // =========================================================

    private boolean matchesSearch(
            EmployeeItem employee,
            String search) {

        return safe(employee.getFullName())
                    .contains(search)

                || safe(employee.getEmail())
                    .contains(search)

                || safe(employee.getPhone())
                    .contains(search)

                || safe(employee.getRole())
                    .contains(search)

                || safe(employee.getCinemaName())
                    .contains(search)

                || String.valueOf(
                        employee.getEmployeeId()
                ).contains(search);
    }


    // =========================================================
    // CREATE EMPLOYEE CARD
    // =========================================================

    private VBox createEmployeeCard(
            EmployeeItem employee) {

        VBox card = new VBox();

        card.setPrefWidth(300);
        card.setMinWidth(300);
        card.setMaxWidth(300);

        card.setPrefHeight(350);
        card.setMinHeight(350);
        card.setMaxHeight(350);

        card.setSpacing(10);

        card.setPadding(
                new Insets(18)
        );

        card.setAlignment(
                Pos.TOP_LEFT
        );

        card.setStyle(
                normalCardStyle()
        );


        // =====================================================
        // HEADER
        // =====================================================

        HBox header =
                new HBox();

        header.setAlignment(
                Pos.CENTER_LEFT
        );


        Label icon =
                new Label("👤");

        icon.setStyle(
                "-fx-font-size:32px;"
        );


        Region headerSpacer =
                new Region();

        HBox.setHgrow(
                headerSpacer,
                javafx.scene.layout.Priority.ALWAYS
        );


        Label id =
                new Label(
                        "ID: "
                        + employee.getEmployeeId()
                );

        id.setStyle(
                "-fx-font-size:12px;"
                + "-fx-text-fill:#888888;"
                + "-fx-font-weight:bold;"
        );


        header.getChildren().addAll(
                icon,
                headerSpacer,
                id
        );


        // =====================================================
        // NAME
        // =====================================================

        Label name =
                new Label(
                        safe(employee.getFullName())
                );

        name.setWrapText(true);

        name.setMaxWidth(260);

        name.setStyle(
                "-fx-font-size:20px;"
                + "-fx-font-weight:bold;"
                + "-fx-text-fill:#222222;"
        );


        // =====================================================
        // ROLE
        // =====================================================

        Label role =
                new Label(
                        safe(employee.getRole())
                );

        role.setStyle(
                "-fx-background-color:#E8F5E9;"
                + "-fx-text-fill:#2E7D32;"
                + "-fx-background-radius:15;"
                + "-fx-padding:5 12 5 12;"
                + "-fx-font-size:12px;"
                + "-fx-font-weight:bold;"
        );


        // =====================================================
        // EMAIL
        // =====================================================

        Label email =
                createInfoLabel(
                        "✉  ",
                        safe(employee.getEmail())
                );


        // =====================================================
        // PHONE
        // =====================================================

        Label phone =
                createInfoLabel(
                        "☎  ",
                        safe(employee.getPhone())
                );


        // =====================================================
        // CINEMA
        // =====================================================

        Label cinema =
                createInfoLabel(
                        "🎬  ",
                        safe(employee.getCinemaName())
                );


        // =====================================================
        // LAST LOGIN
        // =====================================================

        Label lastLogin =
                createInfoLabel(
                        "🕒  ",
                        "Last login: "
                        + safe(employee.getLastLogin())
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
        // BUTTONS
        // =====================================================

        HBox buttons =
                new HBox(8);

        buttons.setAlignment(
                Pos.CENTER
        );


        Button editButton =
                new Button("Edit");

        Button deleteButton =
                new Button("Delete");


        editButton.setPrefWidth(120);
        editButton.setPrefHeight(38);

        deleteButton.setPrefWidth(120);
        deleteButton.setPrefHeight(38);


        editButton.setStyle(
                "-fx-background-color:#757575;"
                + "-fx-text-fill:white;"
                + "-fx-font-weight:bold;"
                + "-fx-background-radius:7;"
                + "-fx-cursor:hand;"
        );


        deleteButton.setStyle(
                "-fx-background-color:#D32F2F;"
                + "-fx-text-fill:white;"
                + "-fx-font-weight:bold;"
                + "-fx-background-radius:7;"
                + "-fx-cursor:hand;"
        );


        // =====================================================
        // EDIT
        // =====================================================

        editButton.setOnAction(
                event ->
                        openEditEmployee(employee)
        );


        // =====================================================
        // DELETE
        // =====================================================

        deleteButton.setOnAction(
                event ->
                        deleteEmployee(employee)
        );


        buttons.getChildren().addAll(
                editButton,
                deleteButton
        );


        card.getChildren().addAll(
                header,
                name,
                role,
                email,
                phone,
                cinema,
                lastLogin,
                spacer,
                buttons
        );


        return card;
    }


    // =========================================================
    // INFO LABEL
    // =========================================================

    private Label createInfoLabel(
            String prefix,
            String value) {

        Label label =
                new Label(
                        prefix + value
                );

        label.setWrapText(true);

        label.setMaxWidth(260);

        label.setStyle(
                "-fx-font-size:13px;"
                + "-fx-text-fill:#555555;"
        );

        return label;
    }


    // =========================================================
    // CARD STYLE
    // =========================================================

    private String normalCardStyle() {

        return
                "-fx-background-color:white;"
                + "-fx-background-radius:12;"
                + "-fx-border-radius:12;"
                + "-fx-border-color:#DDDDDD;"
                + "-fx-border-width:1;"
                + "-fx-effect:"
                + "dropshadow(gaussian,#00000022,8,0,0,3);";
    }


    // =========================================================
    // ADD EMPLOYEE
    // =========================================================

    @FXML
    private void handleAddEmployee() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/AddEmployee.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            AddEmployeeController controller =
                    loader.getController();


            controller.setEmployeeController(
                    this
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Add Employee"
            );


            stage.setScene(
                    new Scene(root)
            );


            stage.getIcons().add(
                    new javafx.scene.image.Image(
                            getClass()
                                    .getResourceAsStream(
                                            "/cinemasystem/images/icon.png"
                                    )
                    )
            );


            stage.show();


        } catch (IOException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Add Employee window."
            );
        }
    }


    // =========================================================
    // OPEN EDIT EMPLOYEE
    // =========================================================

    private void openEditEmployee(
            EmployeeItem selected) {

        if (selected == null) {
            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/EditEmployee.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            EditEmployeeController controller =
                    loader.getController();


            controller.setEmployeeController(
                    this
            );


            controller.setEmployee(
                    selected
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Edit Employee"
            );


            stage.setScene(
                    new Scene(root)
            );


            stage.getIcons().add(
                    new javafx.scene.image.Image(
                            getClass()
                                    .getResourceAsStream(
                                            "/cinemasystem/images/icon.png"
                                    )
                    )
            );


            stage.show();


        } catch (IOException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Edit Employee window."
            );
        }
    }


    // =========================================================
    // TOP EDIT BUTTON
    // =========================================================

    @FXML
    private void handleEditEmployee() {

        EmployeeItem selected =
                getSelectedEmployeeFromCard();


        if (selected == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Employee",
                    "Please select an employee card first."
            );

            return;
        }


        openEditEmployee(
                selected
        );
    }


    // =========================================================
    // GET SELECTED CARD
    // =========================================================

    private EmployeeItem getSelectedEmployeeFromCard() {

        if (employeeContainer == null) {
            return null;
        }


        for (
                Node node :
                employeeContainer.getChildren()
        ) {

            if (node instanceof VBox) {

                VBox card =
                        (VBox) node;


                Object data =
                        card.getUserData();


                if (data instanceof EmployeeItem) {

                    String style =
                            card.getStyle();


                    if (style.contains(
                            "#2E7D32"
                    )) {

                        return (EmployeeItem) data;
                    }
                }
            }
        }


        return null;
    }


    // =========================================================
    // DELETE FROM TOP BUTTON
    // =========================================================

    @FXML
    private void handleDeleteEmployee() {

        EmployeeItem selected =
                getSelectedEmployeeFromCard();


        if (selected == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Delete Employee",
                    "Please select an employee card first."
            );

            return;
        }


        deleteEmployee(
                selected
        );
    }


    // =========================================================
    // DELETE EMPLOYEE
    // =========================================================

    private void deleteEmployee(
            EmployeeItem selected) {

        if (selected == null) {
            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Delete Employee"
        );


        confirmation.setHeaderText(
                null
        );


        confirmation.setContentText(
                "Are you sure you want to delete employee:\n\n"
                + selected.getFullName()
                + "?"
        );


        Optional<ButtonType> result =
                confirmation.showAndWait();


        if (result.isEmpty()
                || result.get()
                != ButtonType.OK) {

            return;
        }


        String sql =
                "DELETE FROM employee "
                + "WHERE employee_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    selected.getEmployeeId()
            );


            statement.executeUpdate();


            loadEmployees();


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Employee deleted successfully."
            );


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not delete this employee.\n\n"
                    + "The employee may be linked to other records."
            );
        }
    }


    // =========================================================
    // DASHBOARD
    // =========================================================

    @FXML
    private void handleDashboard(
            ActionEvent event) {

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


            if (employeeName != null) {

                controller.setEmployeeName(
                        employeeName
                );
            }


            Stage stage =
                    (Stage)
                            ((Node) event.getSource())
                                    .getScene()
                                    .getWindow();


            stage.setScene(
                    new Scene(root)
            );


            stage.setTitle(
                    "Dashboard"
            );


            stage.show();


        } catch (IOException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Dashboard."
            );
        }
    }


    // =========================================================
    // SELECT CARD
    // =========================================================

    private void makeCardSelectable(
            VBox card,
            EmployeeItem employee) {

        card.setUserData(
                employee
        );


        card.setOnMouseClicked(
                event -> {

                    for (
                            Node node :
                            employeeContainer.getChildren()
                    ) {

                        if (node instanceof VBox) {

                            VBox otherCard =
                                    (VBox) node;


                            otherCard.setStyle(
                                    normalCardStyle()
                            );
                        }
                    }


                    card.setStyle(
                            selectedCardStyle()
                    );
                }
        );
    }


    // =========================================================
    // SELECTED CARD STYLE
    // =========================================================

    private String selectedCardStyle() {

        return
                "-fx-background-color:white;"
                + "-fx-background-radius:12;"
                + "-fx-border-radius:12;"
                + "-fx-border-color:#2E7D32;"
                + "-fx-border-width:3;"
                + "-fx-effect:"
                + "dropshadow(gaussian,#00000044,10,0,0,4);";
    }


    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            String value) {

        if (value == null) {
            return "";
        }

        return value.toLowerCase();
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


        alert.setTitle(
                title
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message
        );


        alert.showAndWait();
    }


    // =========================================================
    // EMPLOYEE ITEM
    // =========================================================

    public static class EmployeeItem {

        private final int employeeId;

        private final String fullName;

        private final String email;

        private final String phone;

        private final String role;

        private final int cinemaId;

        private final String cinemaName;

        private final String lastLogin;


        public EmployeeItem(
                int employeeId,
                String fullName,
                String email,
                String phone,
                String role,
                int cinemaId,
                String cinemaName,
                String lastLogin) {

            this.employeeId = employeeId;

            this.fullName = fullName;

            this.email = email;

            this.phone = phone;

            this.role = role;

            this.cinemaId = cinemaId;

            this.cinemaName = cinemaName;

            this.lastLogin = lastLogin;
        }


        public int getEmployeeId() {
            return employeeId;
        }


        public String getFullName() {
            return fullName;
        }


        public String getEmail() {
            return email;
        }


        public String getPhone() {
            return phone;
        }


        public String getRole() {
            return role;
        }


        public int getCinemaId() {
            return cinemaId;
        }


        public String getCinemaName() {
            return cinemaName;
        }


        public String getLastLogin() {
            return lastLogin;
        }
    }
}
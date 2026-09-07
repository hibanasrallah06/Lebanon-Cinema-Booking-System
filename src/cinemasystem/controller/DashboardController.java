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
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DashboardController {

    // =========================================================
    // WELCOME
    // =========================================================

    @FXML
    private Label lblWelcome;

    private String employeeName;


    // =========================================================
    // STATISTICS
    // =========================================================

    @FXML
    private Label lblMovieCount;

    @FXML
    private Label lblShowCount;

    @FXML
    private Label lblCinemaCount;

    @FXML
    private Label lblEmployeeCount;


    // =========================================================
    // SIDEBAR BUTTONS
    // =========================================================

    @FXML
    private Button btnMovie;

    @FXML
    private Button btnShow;

    @FXML
    private Button btnCinema;

    @FXML
    private Button btnEmployee;

    @FXML
    private Button btnBooking;

    @FXML
    private Button btnSnack;

    @FXML
    private Button btnReport;

    @FXML
    private Button btnLogout;


    // =========================================================
    // QUICK ACTION BUTTONS
    // =========================================================
    
    @FXML
    private Button btnManageBooking;

    @FXML
    private Button btnAddMovie;

    @FXML
    private Button btnBookTicket;
    
    @FXML
    private Button btnAIRecommendations;
    
    @FXML
    private Button btnManageQuick;

    @FXML
    private Button btnEmployeesQuick;
    
    @FXML
    private Button btnReportsQuick;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        applyRolePermissions();

        loadStatistics();
    }


    // =========================================================
    // SET EMPLOYEE NAME
    // =========================================================

    public void setEmployeeName(String fullName) {

        employeeName = fullName;

        if (lblWelcome != null) {

            lblWelcome.setText(
                    fullName
            );
        }
    }


    // =========================================================
    // ROLE PERMISSIONS
    // =========================================================

    private void applyRolePermissions() {

        String role = Session.getRole();

        if (role == null) {
            role = "";
        }

        role = role.trim().toLowerCase();


        // =====================================================
        // DEFAULT
        // =====================================================

        hideButton(btnCinema);
        hideButton(btnEmployee);
        hideButton(btnReport);
        hideButton(btnSnack);

        hideButton(btnAddMovie);
        hideButton(btnEmployeesQuick);
        hideButton(btnReportsQuick);


        // =====================================================
        // ADMIN
        // =====================================================

        if (role.equals("admin")) {

            showButton(btnCinema);
            showButton(btnEmployee);
            showButton(btnReport);
            showButton(btnSnack);

            showButton(btnAddMovie);
            showButton(btnEmployeesQuick);
            showButton(btnReportsQuick);

            return;
        }


        // =====================================================
        // MANAGER
        // =====================================================

        if (role.equals("manager")) {

            /*
             * Manager can access:
             *
             * Movies
             * Shows
             * Booking
             * Cinema
             * Employees
             * Snacks
             * Reports
             */

            showButton(btnCinema);
            showButton(btnEmployee);
            showButton(btnReport);
            showButton(btnSnack);

            showButton(btnAddMovie);
            showButton(btnEmployeesQuick);
            showButton(btnReportsQuick);

            return;
        }


        // =====================================================
        // EMPLOYEE
        // =====================================================

        if (role.equals("employee")) {

            /*
             * Employee can access:
             *
             * Movies
             * Shows
             * Booking
             *
             * Snacks are hidden.
             * Reports are hidden.
             * Employees are hidden.
             * Cinemas are hidden.
             */

            return;
        }
    }


    // =========================================================
    // SHOW BUTTON
    // =========================================================

    private void showButton(Button button) {

        if (button != null) {

            button.setVisible(true);
            button.setManaged(true);
        }
    }


    // =========================================================
    // HIDE BUTTON
    // =========================================================

    private void hideButton(Button button) {

        if (button != null) {

            button.setVisible(false);
            button.setManaged(false);
        }
    }


    // =========================================================
    // LOAD STATISTICS
    // =========================================================

    public void loadStatistics() {

        String role = Session.getRole();

        if (role == null) {
            role = "";
        }

        role = role.trim().toLowerCase();


        if (role.equals("admin")) {

            loadAdminStatistics();

            return;
        }


        if (role.equals("manager")
                || role.equals("employee")) {

            loadCinemaStatistics();

            return;
        }


        lblMovieCount.setText("0");
        lblShowCount.setText("0");
        lblCinemaCount.setText("0");
        lblEmployeeCount.setText("0");
    }


    // =========================================================
    // ADMIN STATISTICS
    // =========================================================

    private void loadAdminStatistics() {

        String sql =
                "SELECT " +
                "(SELECT COUNT(*) FROM movie) AS movie_count, " +
                "(SELECT COUNT(*) FROM movie_show) AS show_count, " +
                "(SELECT COUNT(*) FROM cinema) AS cinema_count, " +
                "(SELECT COUNT(*) FROM employee) AS employee_count";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            if (result.next()) {

                lblMovieCount.setText(
                        String.valueOf(
                                result.getInt("movie_count")
                        )
                );

                lblShowCount.setText(
                        String.valueOf(
                                result.getInt("show_count")
                        )
                );

                lblCinemaCount.setText(
                        String.valueOf(
                                result.getInt("cinema_count")
                        )
                );

                lblEmployeeCount.setText(
                        String.valueOf(
                                result.getInt("employee_count")
                        )
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load dashboard statistics.\n\n"
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // CINEMA STATISTICS
    // =========================================================

    private void loadCinemaStatistics() {

        int cinemaId =
                Session.getCinemaId();


        String sql =
                "SELECT " +

                "(SELECT COUNT(DISTINCT ms.movie_id) " +
                " FROM movie_show ms " +
                " JOIN hall h ON ms.hall_id = h.hall_id " +
                " WHERE h.cinema_id = ?) AS movie_count, " +

                "(SELECT COUNT(*) " +
                " FROM movie_show ms " +
                " JOIN hall h ON ms.hall_id = h.hall_id " +
                " WHERE h.cinema_id = ?) AS show_count, " +

                "(SELECT COUNT(*) " +
                " FROM employee " +
                " WHERE cinema_id = ?) AS employee_count";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, cinemaId);
            statement.setInt(2, cinemaId);
            statement.setInt(3, cinemaId);


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    int movieCount =
                            result.getInt(
                                    "movie_count"
                            );

                    int showCount =
                            result.getInt(
                                    "show_count"
                            );

                    int employeeCount =
                            result.getInt(
                                    "employee_count"
                            );


                    lblMovieCount.setText(
                            String.valueOf(movieCount)
                    );

                    lblShowCount.setText(
                            String.valueOf(showCount)
                    );

                    lblCinemaCount.setText("1");

                    lblEmployeeCount.setText(
                            String.valueOf(employeeCount)
                    );
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load cinema statistics.\n\n"
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // SNACK MANAGEMENT
    // =========================================================

    @FXML
    private void handleSnacks(ActionEvent event)
            throws Exception {

        /*
         * Only Admin and Manager can manage snacks.
         */

        if (isEmployee()) {

            showAccessDenied();

            return;
        }


        openPage(
                event,
                "/cinemasystem/view/Snack.fxml",
                "Snack Management",
                false
        );
    }
    
    // =========================================================
// MANAGE / CANCEL BOOKING
// =========================================================

@FXML
private void handleManageBooking(ActionEvent event)
        throws Exception {

    FXMLLoader loader =
            new FXMLLoader(
                    getClass().getResource(
                            "/cinemasystem/view/ManageBooking.fxml"
                    )
            );

    Parent root =
            loader.load();

    ManageBookingController controller =
            loader.getController();

    // Pass logged-in employee information
    controller.setEmployeeName(
            employeeName
    );

    controller.setUserRole(
            Session.getRole()
    );

    controller.setCinemaId(
            Session.getCinemaId()
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
            "Manage / Cancel Booking"
    );

    stage.show();
}


    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    private void handleLogout(ActionEvent event)
            throws Exception {

        Session.clear();


        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/cinemasystem/view/Login.fxml"
                        )
                );


        Parent root =
                loader.load();


        Stage stage =
                (Stage)
                        ((Node) event.getSource())
                                .getScene()
                                .getWindow();


        stage.setScene(
                new Scene(root)
        );

        stage.setTitle("Login");

        stage.show();
    }


    // =========================================================
    // MOVIES
    // =========================================================

    @FXML
    private void handleMovies(ActionEvent event)
            throws Exception {

        openPage(
                event,
                "/cinemasystem/view/Movie.fxml",
                "Movie Management",
                true
        );
    }


    // =========================================================
    // SHOWS
    // =========================================================

    @FXML

private void handleShows(ActionEvent event)
        throws Exception {

    FXMLLoader loader =
            new FXMLLoader(
                    getClass().getResource(
                            "/cinemasystem/view/Schedule.fxml"
                    )
            );

    Parent root =
            loader.load();

    ScheduleController controller =
            loader.getController();

    // Send the user's name to ScheduleController
    controller.setEmployeeName(
            employeeName
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
            "Show Management"
    );

    stage.show();
}

    // =========================================================
    // CINEMAS
    // =========================================================

    @FXML
    private void handleCinemas(ActionEvent event)
            throws Exception {

        if (isEmployee()) {

            showAccessDenied();

            return;
        }


        openPage(
                event,
                "/cinemasystem/view/Cinema.fxml",
                "Cinema Management",
                true
        );
    }


    // =========================================================
    // EMPLOYEES
    // =========================================================

    @FXML
    private void handleEmployees(ActionEvent event)
            throws Exception {

        /*
         * Admin and Manager can access Employees
         * according to your current permission setup.
         */

        if (!isAdmin() && !isManager()) {

            showAccessDenied();

            return;
        }


        openPage(
                event,
                "/cinemasystem/view/Employee.fxml",
                "Employee Management",
                true
        );
    }


    // =========================================================
    // BOOKING
    // =========================================================

    @FXML
    private void handleBooking(ActionEvent event)
            throws Exception {

        openPage(
                event,
                "/cinemasystem/view/Booking.fxml",
                "Booking Management",
                true
        );
    }


    // =========================================================
    // REPORTS
    // =========================================================

    @FXML
    private void handleReports(ActionEvent event)
            throws Exception {

        if (isEmployee()) {

            showAccessDenied();

            return;
        }


        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/cinemasystem/view/Report.fxml"
                        )
                );


        Parent root =
                loader.load();


        ReportController controller =
                loader.getController();


        controller.setUserContext(
                employeeName,
                Session.getRole(),
                Session.getCinemaId()
        );


        Stage stage =
                (Stage)
                        ((Node) event.getSource())
                                .getScene()
                                .getWindow();


        stage.setScene(
                new Scene(root)
        );

        stage.setTitle("Reports");

        stage.show();
    }


    // =========================================================
    // ADD MOVIE
    // =========================================================

    @FXML
    private void handleAddMovies(ActionEvent event) {

        if (isEmployee()) {

            showAccessDenied();

            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/AddMovie.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Movie Management"
            );


            stage.setScene(
                    new Scene(root)
            );
            
            stage.getIcons().add(
    new javafx.scene.image.Image(
        getClass().getResourceAsStream("/cinemasystem/images/icon.png")
    )
);


            stage.show();

        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Add Movie.\n\n"
                            + e.getMessage()
            );
        }
    }
    
    // =========================================================
// AI RECOMMENDATIONS
// =========================================================

@FXML
private void handleAIRecommendations() {

    try {

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/cinemasystem/view/AIRecommendation.fxml"
                        )
                );

        Parent root = loader.load();

        AIRecommendationController controller =
                loader.getController();

        // IMPORTANT: send employee name to AI page
        controller.setEmployeeName(employeeName);

        Stage stage =
                (Stage) btnAIRecommendations
                        .getScene()
                        .getWindow();

        stage.setScene(
                new Scene(root)
        );

        stage.setTitle(
                "AI Movie Recommendations"
        );

        stage.show();

    } catch (Exception e) {

        e.printStackTrace();

        showAlert(
                Alert.AlertType.ERROR,
                "Navigation Error",
                "Could not open AI Recommendations.\n\n"
                        + e.getMessage()
        );
    }
}


    // =========================================================
    // OPEN PAGE
    // =========================================================

    private void openPage(
            ActionEvent event,
            String fxmlPath,
            String title,
            boolean passName)
            throws Exception {


        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                fxmlPath
                        )
                );


        Parent root =
                loader.load();


        Object controller =
                loader.getController();


        if (passName) {

            if (controller instanceof MovieController) {

                ((MovieController) controller)
                        .setEmployeeName(
                                employeeName
                        );

            } else if (controller instanceof ShowController) {

                ShowController showController =
                        (ShowController) controller;

                showController.setEmployeeName(
                        employeeName
                );

                showController.setUserRole(
                        Session.getRole()
                );

            } else if (controller instanceof CinemaController) {

                ((CinemaController) controller)
                        .setEmployeeName(
                                employeeName
                        );

            } else if (controller instanceof EmployeeController) {

                ((EmployeeController) controller)
                        .setEmployeeName(
                                employeeName
                        );

            } else if (controller instanceof BookingController) {

                ((BookingController) controller)
                        .setEmployeeName(
                                employeeName
                        );
            }
        }


        Stage stage =
                (Stage)
                        ((Node) event.getSource())
                                .getScene()
                                .getWindow();


        stage.setScene(
                new Scene(root)
        );


        stage.setTitle(title);

        stage.show();
    }


    // =========================================================
    // ROLE HELPERS
    // =========================================================

    private boolean isAdmin() {

        String role =
                Session.getRole();

        return role != null
                && role.trim()
                        .equalsIgnoreCase("admin");
    }


    private boolean isManager() {

        String role =
                Session.getRole();

        return role != null
                && role.trim()
                        .equalsIgnoreCase("manager");
    }


    private boolean isEmployee() {

        String role =
                Session.getRole();

        return role != null
                && role.trim()
                        .equalsIgnoreCase("employee");
    }


    // =========================================================
    // ACCESS DENIED
    // =========================================================

    private void showAccessDenied() {

        showAlert(
                Alert.AlertType.WARNING,
                "Access Denied",
                "You do not have permission to access this section."
        );
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
package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.util.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.util.Locale;


/**
 * ReportController
 *
 * REPORTS
 *
 * 1. Movie Performance
 *    - Bar Chart  : Tickets sold by movie
 *    - Pie Chart  : Ticket distribution by movie
 *    - Line Chart : Daily revenue trend
 *
 * 2. Cinema Report
 *
 *    A) Specific Cinema
 *       - Bar Chart  : Weekly revenue performance
 *       - Pie Chart  : Movie viewing distribution
 *       - Line Chart : Daily revenue trend
 *
 *    B) All Cinemas
 *       - Bar Chart  : Revenue comparison between cinemas
 *       - Pie Chart  : Revenue distribution between cinemas
 *       - Line Chart : Combined daily revenue trend
 *
 * Date filtering is applied to show_date.
 */
public class ReportController {

    // =========================================================
    // USER / SESSION
    // =========================================================

    private int selectedCinemaId = -1;

    private String selectedCinemaName = "";

    private String employeeName;

    private String userRole;

    private int userCinemaId;


    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private Button btnBack;

    @FXML
    private Button btnGenerate;

    @FXML
    private Button btnExport;

    @FXML
    private ComboBox<String> cmbCinema;

    @FXML
    private ComboBox<String> cmbReportType;

    @FXML
    private DatePicker dpFromDate;

    @FXML
    private DatePicker dpToDate;

    @FXML
    private Label lblCinema;

    @FXML
    private Label lblRevenue;

    @FXML
    private Label lblTickets;

    @FXML
    private Label lblShows;

    @FXML
    private Label lblOccupancy;

    @FXML
    private Label lblLineChartTitle;

    @FXML
    private LineChart<String, Number> lineChart;

    @FXML
    private CategoryAxis lineXAxis;

    @FXML
    private NumberAxis lineYAxis;


    // =========================================================
    // CHARTS
    // =========================================================

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private CategoryAxis barXAxis;

    @FXML
    private NumberAxis barYAxis;

    @FXML
    private PieChart pieChart;


    // =========================================================
    // REPORT DATA
    // =========================================================

    private final ObservableList<ReportRow> currentReportRows =
            FXCollections.observableArrayList();


    // =========================================================
    // TREND DATA
    // =========================================================

    private final ObservableList<TrendRow> currentTrendRows =
            FXCollections.observableArrayList();


    // =========================================================
    // USER CONTEXT
    // =========================================================

    public void setUserContext(
            String employeeName,
            String role,
            int cinemaId) {

        this.employeeName = employeeName;
        this.userRole = role;
        this.userCinemaId = cinemaId;

        configureCinemaSelection();
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        // -----------------------------------------------------
        // REPORT TYPES
        // -----------------------------------------------------

        cmbReportType.setItems(
                FXCollections.observableArrayList(
                        "Movie Performance",
                        "Cinema Report"
                )
        );

        cmbReportType.getSelectionModel().selectFirst();


        // -----------------------------------------------------
        // CHART SETTINGS
        // -----------------------------------------------------

        if (barChart != null) {
            barChart.setAnimated(false);
            barChart.setLegendVisible(true);
        }

        if (pieChart != null) {
            pieChart.setAnimated(false);
            pieChart.setLabelsVisible(true);
            pieChart.setLegendVisible(true);
        }

        if (lineChart != null) {
            lineChart.setAnimated(false);
            lineChart.setCreateSymbols(true);
            lineChart.setLegendVisible(false);
        }


        // -----------------------------------------------------
        // CINEMA
        // -----------------------------------------------------

        setupCinemaSelection();


        // -----------------------------------------------------
        // DATE DEFAULTS
        // -----------------------------------------------------

        if (dpFromDate != null) {
            dpFromDate.setValue(null);
        }

        if (dpToDate != null) {
            dpToDate.setValue(null);
        }


        // -----------------------------------------------------
        // RESET
        // -----------------------------------------------------

        resetStatistics();
        clearCharts();
    }


    // =========================================================
    // SETUP CINEMA SELECTION
    // =========================================================

    private void setupCinemaSelection() {

        String role = Session.getRole();

        if (role == null) {
            role = "";
        }

        role = role.trim().toLowerCase();


        // =====================================================
        // ADMIN
        // =====================================================

        if (role.equals("admin")) {

            showCinemaSelector();

            loadAllCinemasWithAllOption();

            cmbCinema.setDisable(false);

            return;
        }


        // =====================================================
        // MANAGER
        // =====================================================

        if (role.equals("manager")) {

            showCinemaSelector();

            selectedCinemaId = Session.getCinemaId();

            selectedCinemaName =
                    getCinemaName(selectedCinemaId);

            cmbCinema.getItems().clear();

            if (selectedCinemaId > 0) {

                cmbCinema.getItems().add(
                        selectedCinemaId
                        + " - "
                        + selectedCinemaName
                );

                cmbCinema.getSelectionModel()
                        .selectFirst();
            }

            cmbCinema.setDisable(true);

            return;
        }


        // =====================================================
        // OTHER ROLES
        // =====================================================

        hideCinemaSelector();
    }


    // =========================================================
    // CONFIGURE CINEMA SELECTION
    // =========================================================

    private void configureCinemaSelection() {

        if (cmbCinema == null) {
            return;
        }

        cmbCinema.getItems().clear();

        if (userRole == null) {
            userRole = "";
        }

        userRole =
                userRole.trim().toLowerCase();


        // =====================================================
        // ADMIN
        // =====================================================

        if (userRole.equals("admin")) {

            showCinemaSelector();

            loadAllCinemasWithAllOption();

            cmbCinema.setDisable(false);

            return;
        }


        // =====================================================
        // MANAGER
        // =====================================================

        if (userRole.equals("manager")) {

            showCinemaSelector();

            selectedCinemaId = userCinemaId;

            selectedCinemaName =
                    getCinemaName(userCinemaId);

            if (userCinemaId > 0) {

                cmbCinema.getItems().add(
                        userCinemaId
                        + " - "
                        + selectedCinemaName
                );

                cmbCinema.getSelectionModel()
                        .selectFirst();
            }

            cmbCinema.setDisable(true);

            return;
        }


        // =====================================================
        // OTHER
        // =====================================================

        hideCinemaSelector();
    }


    // =========================================================
    // REPORT TYPE CHANGED
    // =========================================================

    @FXML
    private void handleReportTypeChanged(ActionEvent event) {

        String reportType =
                cmbReportType.getValue();

        if (reportType == null) {
            return;
        }

        String role =
                getCurrentRole();


        // =====================================================
        // ADMIN
        // =====================================================

        if (role.equals("admin")) {

            cmbCinema.setDisable(false);

            loadAllCinemasWithAllOption();

            return;
        }


        // =====================================================
        // MANAGER
        // =====================================================

        if (role.equals("manager")) {

            cmbCinema.setDisable(true);

            selectedCinemaId =
                    userCinemaId;

            selectedCinemaName =
                    getCinemaName(userCinemaId);

            cmbCinema.getItems().clear();

            if (userCinemaId > 0) {

                cmbCinema.getItems().add(
                        userCinemaId
                        + " - "
                        + selectedCinemaName
                );

                cmbCinema.getSelectionModel()
                        .selectFirst();
            }
        }
    }


    // =========================================================
    // SHOW CINEMA SELECTOR
    // =========================================================

    private void showCinemaSelector() {

        if (cmbCinema != null) {

            cmbCinema.setVisible(true);
            cmbCinema.setManaged(true);
        }

        if (lblCinema != null) {

            lblCinema.setVisible(true);
            lblCinema.setManaged(true);
        }
    }


    // =========================================================
    // HIDE CINEMA SELECTOR
    // =========================================================

    private void hideCinemaSelector() {

        if (cmbCinema != null) {

            cmbCinema.setVisible(false);
            cmbCinema.setManaged(false);
        }

        if (lblCinema != null) {

            lblCinema.setVisible(false);
            lblCinema.setManaged(false);
        }
    }


    // =========================================================
    // LOAD ALL CINEMAS + ALL OPTION
    // =========================================================

    private void loadAllCinemasWithAllOption() {

        cmbCinema.getItems().clear();

        cmbCinema.getItems().add(
                "All Cinemas"
        );

        String sql =
                "SELECT cinema_id, cinema_name "
                + "FROM cinema "
                + "ORDER BY cinema_name";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                int cinemaId =
                        result.getInt("cinema_id");

                String cinemaName =
                        result.getString("cinema_name");

                cmbCinema.getItems().add(
                        cinemaId
                        + " - "
                        + cinemaName
                );
            }


            cmbCinema.getSelectionModel()
                    .selectFirst();

            selectedCinemaId = -1;

            selectedCinemaName =
                    "All Cinemas";

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load cinemas.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // CINEMA CHANGED
    // =========================================================

    @FXML
    private void handleCinemaChanged(ActionEvent event) {

        if (cmbCinema == null) {
            return;
        }

        String selected =
                cmbCinema.getValue();

        if (selected == null
                || selected.isEmpty()) {

            return;
        }


        // =====================================================
        // ALL CINEMAS
        // =====================================================

        if (selected.equals("All Cinemas")) {

            selectedCinemaId = -1;

            selectedCinemaName =
                    "All Cinemas";

        } else {

            updateSelectedCinema();
        }


        // -----------------------------------------------------
        // RESET SCREEN
        // -----------------------------------------------------

        resetStatistics();

        clearCharts();

        currentReportRows.clear();

        currentTrendRows.clear();


        // -----------------------------------------------------
        // UPDATE TITLE
        // -----------------------------------------------------

        String reportType =
                cmbReportType.getValue();

        if (reportType == null) {
            return;
        }


        if (reportType.equals("Cinema Report")) {

            if (selectedCinemaId <= 0) {

                lblLineChartTitle.setText(
                        "Daily Revenue Trend - All Cinemas"
                );

            } else {

                lblLineChartTitle.setText(
                        "Daily Revenue Trend - "
                        + selectedCinemaName
                );
            }

        } else {

            if (selectedCinemaId > 0) {

                lblLineChartTitle.setText(
                        "Daily Revenue Trend - "
                        + selectedCinemaName
                );
            }
        }
    }


    // =========================================================
    // UPDATE SELECTED CINEMA
    // =========================================================

    private void updateSelectedCinema() {

        if (cmbCinema == null) {
            return;
        }

        String selected =
                cmbCinema.getValue();

        if (selected == null
                || selected.isEmpty()) {

            selectedCinemaId = -1;
            selectedCinemaName = "";

            return;
        }


        try {

            String[] parts =
                    selected.split(" - ", 2);

            selectedCinemaId =
                    Integer.parseInt(
                            parts[0].trim()
                    );

            if (parts.length > 1) {

                selectedCinemaName =
                        parts[1].trim();

            } else {

                selectedCinemaName =
                        getCinemaName(
                                selectedCinemaId
                        );
            }

        } catch (Exception e) {

            selectedCinemaId = -1;
            selectedCinemaName = "";
        }
    }


    // =========================================================
    // GET CINEMA NAME
    // =========================================================

    private String getCinemaName(int cinemaId) {

        String sql =
                "SELECT cinema_name "
                + "FROM cinema "
                + "WHERE cinema_id = ?";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    cinemaId
            );

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getString(
                            "cinema_name"
                    );
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return "Unknown Cinema";
    }


    // =========================================================
    // GENERATE REPORT
    // =========================================================

    @FXML
    private void handleGenerate(ActionEvent event) {

        String selectedReport =
                cmbReportType.getValue();

        if (selectedReport == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Report",
                    "Please select a report type."
            );

            return;
        }


        // -----------------------------------------------------
        // DATE VALIDATION
        // -----------------------------------------------------

        LocalDate fromDate =
                dpFromDate.getValue();

        LocalDate toDate =
                dpToDate.getValue();

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Date Filter",
                    "From Date cannot be after To Date."
            );

            return;
        }


        // =====================================================
        // CINEMA REPORT
        // =====================================================

        if (selectedReport.equals("Cinema Report")) {

            String role =
                    getCurrentRole();


            // -------------------------------------------------
            // ADMIN
            // -------------------------------------------------

            if (role.equals("admin")) {

                /*
                 * IMPORTANT:
                 *
                 * If Admin selected:
                 *     All Cinemas
                 * -> compare cinemas
                 *
                 * If Admin selected:
                 *     Specific Cinema
                 * -> show weekly performance
                 */

                if (selectedCinemaId <= 0) {

                    selectedCinemaName =
                            "All Cinemas";

                    generateCinemaReport();

                    return;
                }


                // Specific cinema

                generateCinemaReport();

                return;
            }


            // -------------------------------------------------
            // MANAGER
            // -------------------------------------------------

            if (selectedCinemaId <= 0) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Cinema",
                        "Please select a cinema."
                );

                return;
            }

            generateCinemaReport();

            return;
        }


        // =====================================================
        // MOVIE PERFORMANCE
        // =====================================================

        if (selectedReport.equals("Movie Performance")) {

            if (selectedCinemaId <= 0) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Cinema",
                        "Please select a cinema."
                );

                return;
            }

            generateMovieReport();
        }
    }


    // =========================================================
    // MOVIE PERFORMANCE REPORT
    // =========================================================

    private void generateMovieReport() {

        lblLineChartTitle.setText(
                "Daily Revenue Trend - "
                + selectedCinemaName
        );

        currentReportRows.clear();

        LocalDate fromDate =
                dpFromDate.getValue();

        LocalDate toDate =
                dpToDate.getValue();


        StringBuilder sql =
                new StringBuilder();


        sql.append(
                "WITH movie_seats AS ( "
        );


        sql.append(
                "SELECT "
                + "ms.movie_id, "
                + "ms.show_id, "
                + "h.capacity, "
                + "COUNT(bs.booking_seat_id) AS tickets, "
                + "COALESCE(SUM(bs.price), 0) AS revenue "
                + "FROM movie_show ms "
                + "JOIN hall h "
                + "ON ms.hall_id = h.hall_id "
                + "LEFT JOIN booking b "
                + "ON ms.show_id = b.show_id "
                + "AND LOWER(b.status) NOT IN "
                + "('cancelled', 'canceled') "
                + "LEFT JOIN booking_seat bs "
                + "ON b.booking_id = bs.booking_id "
                + "WHERE h.cinema_id = ? "
        );


        if (fromDate != null) {

            sql.append(
                    "AND ms.show_date >= ? "
            );
        }


        if (toDate != null) {

            sql.append(
                    "AND ms.show_date <= ? "
            );
        }


        sql.append(
                "GROUP BY "
                + "ms.movie_id, "
                + "ms.show_id, "
                + "h.capacity "
        );


        sql.append(
                "), "
                + "movie_stats AS ( "
                + "SELECT "
                + "movie_id, "
                + "COUNT(show_id) AS shows, "
                + "COALESCE(SUM(tickets), 0) AS tickets, "
                + "COALESCE(SUM(revenue), 0) AS revenue, "
                + "COALESCE(SUM(capacity), 0) AS capacity "
                + "FROM movie_seats "
                + "GROUP BY movie_id "
                + ") "
        );


        sql.append(
                "SELECT "
                + "m.title AS name, "
                + "ms.shows, "
                + "ms.tickets, "
                + "ms.revenue, "
                + "CASE "
                + "WHEN ms.capacity = 0 THEN 0 "
                + "ELSE ms.tickets * 100.0 / ms.capacity "
                + "END AS occupancy "
                + "FROM movie_stats ms "
                + "JOIN movie m "
                + "ON ms.movie_id = m.movie_id "
                + "ORDER BY ms.revenue DESC"
        );


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int parameterIndex = 1;


            statement.setInt(
                    parameterIndex++,
                    selectedCinemaId
            );


            if (fromDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                fromDate
                        )
                );
            }


            if (toDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                toDate
                        )
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                ObservableList<ReportRow> rows =
                        FXCollections.observableArrayList();

                int totalShows = 0;
                int totalTickets = 0;
                double totalRevenue = 0.0;


                while (result.next()) {

                    String name =
                            result.getString("name");

                    int shows =
                            result.getInt("shows");

                    int tickets =
                            result.getInt("tickets");

                    double revenue =
                            result.getDouble("revenue");

                    double occupancy =
                            result.getDouble("occupancy");


                    rows.add(
                            new ReportRow(
                                    name,
                                    shows,
                                    tickets,
                                    revenue,
                                    occupancy
                            )
                    );


                    totalShows += shows;
                    totalTickets += tickets;
                    totalRevenue += revenue;
                }


                currentReportRows.setAll(rows);


                // -------------------------------------------------
                // BAR + PIE
                // -------------------------------------------------

                updateMovieCharts(rows);


                // -------------------------------------------------
                // LINE CHART
                // -------------------------------------------------

                generateRevenueTrend();


                // -------------------------------------------------
                // STATISTICS
                // -------------------------------------------------

                updateStatistics(
                        totalRevenue,
                        totalTickets,
                        totalShows,
                        calculateOverallOccupancy()
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Movie Report",
                    "Could not generate movie report.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // CINEMA REPORT
    // =========================================================
    //
    // SPECIFIC CINEMA:
    //
    // Bar  = Weekly Revenue
    // Pie  = Movie Viewing Distribution
    // Line = Daily Revenue
    //
    // ALL CINEMAS:
    //
    // Bar  = Revenue by Cinema
    // Pie  = Revenue Distribution by Cinema
    // Line = Combined Daily Revenue
    //
    // =========================================================

    private void generateCinemaReport() {

        LocalDate fromDate =
                dpFromDate.getValue();

        LocalDate toDate =
                dpToDate.getValue();


        // =====================================================
        // SPECIFIC CINEMA
        // =====================================================

        if (selectedCinemaId > 0) {

            /*
             * If no dates were entered,
             * automatically use the current month.
             */

            if (fromDate == null
                    && toDate == null) {

                LocalDate today =
                        LocalDate.now();

                fromDate =
                        today.withDayOfMonth(1);

                toDate =
                        today.withDayOfMonth(
                                today.lengthOfMonth()
                        );

                dpFromDate.setValue(fromDate);
                dpToDate.setValue(toDate);
            }


            lblLineChartTitle.setText(
                    "Daily Revenue Trend - "
                    + selectedCinemaName
            );


            generateSpecificCinemaStatistics(
                    fromDate,
                    toDate
            );


            generateWeeklyCinemaPerformance(
                    fromDate,
                    toDate
            );


            generateMovieViewingDistribution(
                    fromDate,
                    toDate
            );


            generateRevenueTrend();

            return;
        }


        // =====================================================
        // ALL CINEMAS
        // =====================================================

        lblLineChartTitle.setText(
                "Daily Revenue Trend - All Cinemas"
        );


        generateAllCinemasReport(
                fromDate,
                toDate
        );


        generateRevenueTrend();
    }


    // =========================================================
    // SPECIFIC CINEMA STATISTICS
    // =========================================================

    private void generateSpecificCinemaStatistics(
            LocalDate fromDate,
            LocalDate toDate) {

        currentReportRows.clear();


        StringBuilder sql =
                new StringBuilder();


        sql.append(
                "WITH cinema_show_stats AS ( "
                + "SELECT "
                + "ms.show_id, "
                + "h.capacity, "
                + "COUNT(bs.booking_seat_id) AS tickets, "
                + "COALESCE(SUM(bs.price), 0) AS revenue "
                + "FROM movie_show ms "
                + "JOIN hall h "
                + "ON ms.hall_id = h.hall_id "
                + "LEFT JOIN booking b "
                + "ON ms.show_id = b.show_id "
                + "AND LOWER(b.status) NOT IN "
                + "('cancelled', 'canceled') "
                + "LEFT JOIN booking_seat bs "
                + "ON b.booking_id = bs.booking_id "
                + "WHERE h.cinema_id = ? "
        );


        if (fromDate != null) {

            sql.append(
                    "AND ms.show_date >= ? "
            );
        }


        if (toDate != null) {

            sql.append(
                    "AND ms.show_date <= ? "
            );
        }


        sql.append(
                "GROUP BY "
                + "ms.show_id, "
                + "h.capacity "
                + ") "
        );


        sql.append(
                "SELECT "
                + "COUNT(show_id) AS shows, "
                + "COALESCE(SUM(tickets), 0) AS tickets, "
                + "COALESCE(SUM(revenue), 0) AS revenue, "
                + "COALESCE(SUM(capacity), 0) AS capacity "
                + "FROM cinema_show_stats"
        );


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int parameterIndex = 1;


            statement.setInt(
                    parameterIndex++,
                    selectedCinemaId
            );


            if (fromDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                fromDate
                        )
                );
            }


            if (toDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                toDate
                        )
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    int shows =
                            result.getInt("shows");

                    int tickets =
                            result.getInt("tickets");

                    double revenue =
                            result.getDouble("revenue");

                    double capacity =
                            result.getDouble("capacity");


                    double occupancy = 0.0;

                    if (capacity > 0) {

                        occupancy =
                                tickets
                                * 100.0
                                / capacity;
                    }


                    currentReportRows.add(
                            new ReportRow(
                                    selectedCinemaName,
                                    shows,
                                    tickets,
                                    revenue,
                                    occupancy
                            )
                    );


                    updateStatistics(
                            revenue,
                            tickets,
                            shows,
                            occupancy
                    );
                }
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Cinema Report",
                    "Could not calculate cinema statistics.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // WEEKLY CINEMA PERFORMANCE
    // =========================================================
    //
    // BAR CHART:
    //
    // Week 1
    // Week 2
    // Week 3
    // Week 4
    // Week 5
    //
    // Value = Revenue
    //
    // =========================================================

  private void generateWeeklyCinemaPerformance(
        LocalDate fromDate,
        LocalDate toDate) {

    if (barChart != null) {
        barChart.getData().clear();
    }

    /*
     * Weekly Cinema Performance
     *
     * Week 1 -> Revenue
     * Week 2 -> Revenue
     * Week 3 -> Revenue
     * Week 4 -> Revenue
     * Week 5 -> Revenue
     *
     * We use a subquery here so MySQL's
     * ONLY_FULL_GROUP_BY does not complain.
     */

    String sql =
            "SELECT "
            + "weekly.week_number, "
            + "COALESCE(SUM(weekly.revenue), 0) AS revenue "
            + "FROM ( "
            + "    SELECT "
            + "        FLOOR(DATEDIFF(ms.show_date, ?) / 7) + 1 "
            + "            AS week_number, "
            + "        COALESCE(SUM(bs.price), 0) AS revenue "
            + "    FROM movie_show ms "
            + "    JOIN hall h "
            + "        ON ms.hall_id = h.hall_id "
            + "    LEFT JOIN booking b "
            + "        ON ms.show_id = b.show_id "
            + "        AND LOWER(b.status) NOT IN "
            + "            ('cancelled', 'canceled') "
            + "    LEFT JOIN booking_seat bs "
            + "        ON b.booking_id = bs.booking_id "
            + "    WHERE h.cinema_id = ? "
            + "        AND ms.show_date >= ? "
            + "        AND ms.show_date <= ? "
            + "    GROUP BY "
            + "        ms.show_id, "
            + "        ms.show_date "
            + ") weekly "
            + "GROUP BY weekly.week_number "
            + "ORDER BY weekly.week_number";


    try (
            Connection connection =
                    DBConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        int parameterIndex = 1;


        // -----------------------------------------------------
        // First parameter:
        // First day of selected month / period
        // -----------------------------------------------------

        statement.setDate(
                parameterIndex++,
                java.sql.Date.valueOf(fromDate)
        );


        // -----------------------------------------------------
        // Cinema
        // -----------------------------------------------------

        statement.setInt(
                parameterIndex++,
                selectedCinemaId
        );


        // -----------------------------------------------------
        // From Date
        // -----------------------------------------------------

        statement.setDate(
                parameterIndex++,
                java.sql.Date.valueOf(fromDate)
        );


        // -----------------------------------------------------
        // To Date
        // -----------------------------------------------------

        statement.setDate(
                parameterIndex++,
                java.sql.Date.valueOf(toDate)
        );


        XYChart.Series<String, Number>
                series =
                new XYChart.Series<>();


        series.setName(
                "Weekly Revenue"
        );


        try (
                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                int week =
                        result.getInt(
                                "week_number"
                        );

                double revenue =
                        result.getDouble(
                                "revenue"
                        );


                series.getData().add(
                        new XYChart.Data<>(
                                "Week " + week,
                                revenue
                        )
                );
            }
        }


        // -----------------------------------------------------
        // AXIS LABELS
        // -----------------------------------------------------

        if (barXAxis != null) {

            barXAxis.setLabel(
                    "Week"
            );
        }


        if (barYAxis != null) {

            barYAxis.setLabel(
                    "Revenue ($)"
            );
        }


        // -----------------------------------------------------
        // CHART TITLE
        // -----------------------------------------------------

        if (barChart != null) {

            barChart.setTitle(
                    "Weekly Cinema Performance"
            );

            barChart.getData().clear();

            barChart.getData().add(
                    series
            );
        }


    } catch (SQLException e) {

        e.printStackTrace();

        showAlert(
                Alert.AlertType.ERROR,
                "Weekly Performance",
                "Could not generate weekly performance.\n\n"
                + e.getMessage()
        );
    }
}


    // =========================================================
    // MOVIE VIEWING DISTRIBUTION
    // =========================================================
    //
    // PIE CHART:
    //
    // Movie A -> number of tickets
    // Movie B -> number of tickets
    // Movie C -> number of tickets
    //
    // =========================================================

    private void generateMovieViewingDistribution(
            LocalDate fromDate,
            LocalDate toDate) {

        if (pieChart != null) {
            pieChart.getData().clear();
        }


        String sql =
                "SELECT "
                + "m.title AS movie_name, "
                + "COUNT(bs.booking_seat_id) AS tickets "
                + "FROM movie_show ms "
                + "JOIN hall h "
                + "ON ms.hall_id = h.hall_id "
                + "JOIN movie m "
                + "ON ms.movie_id = m.movie_id "
                + "LEFT JOIN booking b "
                + "ON ms.show_id = b.show_id "
                + "AND LOWER(b.status) NOT IN "
                + "('cancelled', 'canceled') "
                + "LEFT JOIN booking_seat bs "
                + "ON b.booking_id = bs.booking_id "
                + "WHERE h.cinema_id = ? "
                + "AND ms.show_date >= ? "
                + "AND ms.show_date <= ? "
                + "GROUP BY m.movie_id, m.title "
                + "HAVING COUNT(bs.booking_seat_id) > 0 "
                + "ORDER BY tickets DESC";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    selectedCinemaId
            );

            statement.setDate(
                    2,
                    java.sql.Date.valueOf(
                            fromDate
                    )
            );

            statement.setDate(
                    3,
                    java.sql.Date.valueOf(
                            toDate
                    )
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    String movieName =
                            result.getString(
                                    "movie_name"
                            );

                    int tickets =
                            result.getInt(
                                    "tickets"
                            );


                    pieChart.getData().add(
                            new PieChart.Data(
                                    movieName,
                                    tickets
                            )
                    );
                }
            }


            pieChart.setTitle(
                    "Movie Viewing Distribution"
            );


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Movie Distribution",
                    "Could not generate movie distribution.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // ALL CINEMAS REPORT
    // =========================================================

    private void generateAllCinemasReport(
            LocalDate fromDate,
            LocalDate toDate) {

        currentReportRows.clear();


        StringBuilder sql =
                new StringBuilder();


        sql.append(
                "WITH cinema_show_stats AS ( "
                + "SELECT "
                + "c.cinema_id, "
                + "c.cinema_name, "
                + "ms.show_id, "
                + "h.capacity, "
                + "COUNT(bs.booking_seat_id) AS tickets, "
                + "COALESCE(SUM(bs.price), 0) AS revenue "
                + "FROM cinema c "
                + "LEFT JOIN hall h "
                + "ON c.cinema_id = h.cinema_id "
                + "LEFT JOIN movie_show ms "
                + "ON h.hall_id = ms.hall_id "
        );


        if (fromDate != null) {

            sql.append(
                    "AND ms.show_date >= ? "
            );
        }


        if (toDate != null) {

            sql.append(
                    "AND ms.show_date <= ? "
            );
        }


        sql.append(
                "LEFT JOIN booking b "
                + "ON ms.show_id = b.show_id "
                + "AND LOWER(b.status) NOT IN "
                + "('cancelled', 'canceled') "
                + "LEFT JOIN booking_seat bs "
                + "ON b.booking_id = bs.booking_id "
                + "GROUP BY "
                + "c.cinema_id, "
                + "c.cinema_name, "
                + "ms.show_id, "
                + "h.capacity "
                + "), "
        );


        sql.append(
                "cinema_stats AS ( "
                + "SELECT "
                + "cinema_id, "
                + "cinema_name, "
                + "COUNT(show_id) AS shows, "
                + "COALESCE(SUM(tickets), 0) AS tickets, "
                + "COALESCE(SUM(revenue), 0) AS revenue, "
                + "COALESCE(SUM(capacity), 0) AS capacity "
                + "FROM cinema_show_stats "
                + "GROUP BY cinema_id, cinema_name "
                + ") "
        );


        sql.append(
                "SELECT "
                + "cinema_name AS name, "
                + "shows, "
                + "tickets, "
                + "revenue, "
                + "CASE "
                + "WHEN capacity = 0 THEN 0 "
                + "ELSE tickets * 100.0 / capacity "
                + "END AS occupancy "
                + "FROM cinema_stats "
                + "ORDER BY revenue DESC"
        );


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int parameterIndex = 1;


            if (fromDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                fromDate
                        )
                );
            }


            if (toDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                toDate
                        )
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                ObservableList<ReportRow> rows =
                        FXCollections.observableArrayList();

                int totalShows = 0;
                int totalTickets = 0;
                double totalRevenue = 0.0;


                while (result.next()) {

                    String name =
                            result.getString("name");

                    int shows =
                            result.getInt("shows");

                    int tickets =
                            result.getInt("tickets");

                    double revenue =
                            result.getDouble("revenue");

                    double occupancy =
                            result.getDouble("occupancy");


                    rows.add(
                            new ReportRow(
                                    name,
                                    shows,
                                    tickets,
                                    revenue,
                                    occupancy
                            )
                    );


                    totalShows += shows;
                    totalTickets += tickets;
                    totalRevenue += revenue;
                }


                currentReportRows.setAll(rows);


                // -------------------------------------------------
                // BAR + PIE
                // -------------------------------------------------

                updateAllCinemaCharts(
                        rows
                );


                // -------------------------------------------------
                // OCCUPANCY
                // -------------------------------------------------

                double occupancy =
                        calculateAllCinemasOccupancy();


                // -------------------------------------------------
                // STATISTICS
                // -------------------------------------------------

                updateStatistics(
                        totalRevenue,
                        totalTickets,
                        totalShows,
                        occupancy
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Cinema Report",
                    "Could not generate all cinemas report.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // MOVIE PERFORMANCE CHARTS
    // =========================================================

    private void updateMovieCharts(
            ObservableList<ReportRow> rows) {

        if (barChart != null) {
            barChart.getData().clear();
        }

        if (pieChart != null) {
            pieChart.getData().clear();
        }


        if (rows == null
                || rows.isEmpty()) {

            return;
        }


        XYChart.Series<String, Number>
                barSeries =
                new XYChart.Series<>();


        barSeries.setName(
                "Tickets Sold"
        );


        if (barXAxis != null) {

            barXAxis.setLabel(
                    "Movie"
            );
        }


        if (barYAxis != null) {

            barYAxis.setLabel(
                    "Tickets Sold"
            );
        }


        for (ReportRow row : rows) {

            int tickets =
                    row.getTickets();


            barSeries.getData().add(
                    new XYChart.Data<>(
                            row.getName(),
                            tickets
                    )
            );


            if (tickets > 0) {

                pieChart.getData().add(
                        new PieChart.Data(
                                row.getName(),
                                tickets
                        )
                );
            }
        }


        pieChart.setTitle(
                "Movie Ticket Distribution"
        );


        barChart.setTitle(
                "Movie Performance"
        );


        barChart.getData().add(
                barSeries
        );
    }


    // =========================================================
    // ALL CINEMA CHARTS
    // =========================================================

    private void updateAllCinemaCharts(
            ObservableList<ReportRow> rows) {

        if (barChart != null) {
            barChart.getData().clear();
        }

        if (pieChart != null) {
            pieChart.getData().clear();
        }


        if (rows == null
                || rows.isEmpty()) {

            return;
        }


        XYChart.Series<String, Number>
                barSeries =
                new XYChart.Series<>();


        barSeries.setName(
                "Revenue"
        );


        if (barXAxis != null) {

            barXAxis.setLabel(
                    "Cinema"
            );
        }


        if (barYAxis != null) {

            barYAxis.setLabel(
                    "Revenue ($)"
            );
        }


        for (ReportRow row : rows) {

            double revenue =
                    row.getRevenueValue();


            barSeries.getData().add(
                    new XYChart.Data<>(
                            row.getName(),
                            revenue
                    )
            );


            if (revenue > 0) {

                pieChart.getData().add(
                        new PieChart.Data(
                                row.getName(),
                                revenue
                        )
                );
            }
        }


        barChart.setTitle(
                "Cinema Revenue Comparison"
        );


        pieChart.setTitle(
                "Cinema Revenue Distribution"
        );


        barChart.getData().add(
                barSeries
        );
    }


    // =========================================================
    // REVENUE TREND
    // =========================================================
    //
    // Line chart:
    //
    // Specific Cinema:
    //     Daily revenue of selected cinema
    //
    // All Cinemas:
    //     Combined daily revenue
    //
    // =========================================================

    private void generateRevenueTrend() {

        currentTrendRows.clear();


        if (lineChart != null) {

            lineChart.getData().clear();
        }


        String role =
                getCurrentRole();


        LocalDate fromDate =
                dpFromDate.getValue();

        LocalDate toDate =
                dpToDate.getValue();


        StringBuilder sql =
                new StringBuilder();


        sql.append(
                "SELECT "
                + "ms.show_date AS report_date, "
                + "COALESCE(SUM(bs.price), 0) AS revenue "
                + "FROM movie_show ms "
                + "JOIN hall h "
                + "ON ms.hall_id = h.hall_id "
                + "LEFT JOIN booking b "
                + "ON ms.show_id = b.show_id "
                + "AND LOWER(b.status) NOT IN "
                + "('cancelled', 'canceled') "
                + "LEFT JOIN booking_seat bs "
                + "ON b.booking_id = bs.booking_id "
                + "WHERE 1 = 1 "
        );


        // -----------------------------------------------------
        // CINEMA FILTER
        // -----------------------------------------------------

        if (selectedCinemaId > 0) {

            sql.append(
                    "AND h.cinema_id = ? "
            );
        }


        // -----------------------------------------------------
        // DATE FILTER
        // -----------------------------------------------------

        if (fromDate != null) {

            sql.append(
                    "AND ms.show_date >= ? "
            );
        }


        if (toDate != null) {

            sql.append(
                    "AND ms.show_date <= ? "
            );
        }


        sql.append(
                "GROUP BY ms.show_date "
                + "ORDER BY ms.show_date"
        );


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int parameterIndex = 1;


            // -------------------------------------------------
            // CINEMA
            // -------------------------------------------------

            if (selectedCinemaId > 0) {

                statement.setInt(
                        parameterIndex++,
                        selectedCinemaId
                );
            }


            // -------------------------------------------------
            // FROM DATE
            // -------------------------------------------------

            if (fromDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                fromDate
                        )
                );
            }


            // -------------------------------------------------
            // TO DATE
            // -------------------------------------------------

            if (toDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                toDate
                        )
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                XYChart.Series<String, Number>
                        lineSeries =
                        new XYChart.Series<>();


                lineSeries.setName(
                        "Revenue"
                );


                while (result.next()) {

                    LocalDate date =
                            result.getDate(
                                    "report_date"
                            ).toLocalDate();

                    double revenue =
                            result.getDouble(
                                    "revenue"
                            );


                    currentTrendRows.add(
                            new TrendRow(
                                    date,
                                    revenue
                            )
                    );


                    lineSeries.getData().add(
                            new XYChart.Data<>(
                                    date.toString(),
                                    revenue
                            )
                    );
                }


                if (lineChart != null) {

                    lineXAxis.setLabel(
                            "Date"
                    );

                    lineYAxis.setLabel(
                            "Revenue ($)"
                    );

                    lineChart.setTitle(
                            ""
                    );

                    lineChart.getData().add(
                            lineSeries
                    );
                }


                updateTrendTitle();
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Trend Report",
                    "Could not generate revenue trend.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // TREND TITLE
    // =========================================================

    private void updateTrendTitle() {

        String reportType =
                cmbReportType.getValue();


        if (reportType == null) {

            lblLineChartTitle.setText(
                    "Daily Revenue Trend"
            );

            return;
        }


        if (reportType.equals("Cinema Report")) {

            if (selectedCinemaId > 0) {

                lblLineChartTitle.setText(
                        "Daily Revenue Trend - "
                        + selectedCinemaName
                );

            } else {

                lblLineChartTitle.setText(
                        "Daily Revenue Trend - All Cinemas"
                );
            }

            return;
        }


        if (reportType.equals("Movie Performance")) {

            lblLineChartTitle.setText(
                    "Daily Revenue Trend - "
                    + selectedCinemaName
            );
        }
    }


    // =========================================================
    // CLEAR CHARTS
    // =========================================================

    private void clearCharts() {

        if (barChart != null) {
            barChart.getData().clear();
        }

        if (pieChart != null) {
            pieChart.getData().clear();
        }

        if (lineChart != null) {
            lineChart.getData().clear();
        }

        currentTrendRows.clear();
    }


    // =========================================================
    // OVERALL OCCUPANCY
    // =========================================================

    private double calculateOverallOccupancy() {

        LocalDate fromDate =
                dpFromDate.getValue();

        LocalDate toDate =
                dpToDate.getValue();


        if (selectedCinemaId <= 0) {
            return 0.0;
        }


        StringBuilder sql =
                new StringBuilder();


        sql.append(
                "WITH show_stats AS ( "
                + "SELECT "
                + "ms.show_id, "
                + "h.capacity, "
                + "COUNT(bs.booking_seat_id) AS tickets "
                + "FROM movie_show ms "
                + "JOIN hall h "
                + "ON ms.hall_id = h.hall_id "
                + "LEFT JOIN booking b "
                + "ON ms.show_id = b.show_id "
                + "AND LOWER(b.status) NOT IN "
                + "('cancelled', 'canceled') "
                + "LEFT JOIN booking_seat bs "
                + "ON b.booking_id = bs.booking_id "
                + "WHERE h.cinema_id = ? "
        );


        if (fromDate != null) {

            sql.append(
                    "AND ms.show_date >= ? "
            );
        }


        if (toDate != null) {

            sql.append(
                    "AND ms.show_date <= ? "
            );
        }


        sql.append(
                "GROUP BY "
                + "ms.show_id, "
                + "h.capacity "
                + ") "
                + "SELECT "
                + "COALESCE(SUM(tickets), 0) AS tickets, "
                + "COALESCE(SUM(capacity), 0) AS capacity "
                + "FROM show_stats"
        );


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int parameterIndex = 1;


            statement.setInt(
                    parameterIndex++,
                    selectedCinemaId
            );


            if (fromDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                fromDate
                        )
                );
            }


            if (toDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                toDate
                        )
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    double tickets =
                            result.getDouble(
                                    "tickets"
                            );

                    double capacity =
                            result.getDouble(
                                    "capacity"
                            );


                    if (capacity == 0) {
                        return 0.0;
                    }


                    return
                            (tickets / capacity)
                            * 100.0;
                }
            }


        } catch (SQLException e) {

            e.printStackTrace();
        }


        return 0.0;
    }


    // =========================================================
    // ALL CINEMAS OCCUPANCY
    // =========================================================

    private double calculateAllCinemasOccupancy() {

        LocalDate fromDate =
                dpFromDate.getValue();

        LocalDate toDate =
                dpToDate.getValue();


        StringBuilder sql =
                new StringBuilder();


        sql.append(
                "WITH show_stats AS ( "
                + "SELECT "
                + "ms.show_id, "
                + "h.capacity, "
                + "COUNT(bs.booking_seat_id) AS tickets "
                + "FROM movie_show ms "
                + "JOIN hall h "
                + "ON ms.hall_id = h.hall_id "
                + "LEFT JOIN booking b "
                + "ON ms.show_id = b.show_id "
                + "AND LOWER(b.status) NOT IN "
                + "('cancelled', 'canceled') "
                + "LEFT JOIN booking_seat bs "
                + "ON b.booking_id = bs.booking_id "
                + "WHERE 1 = 1 "
        );


        if (fromDate != null) {

            sql.append(
                    "AND ms.show_date >= ? "
            );
        }


        if (toDate != null) {

            sql.append(
                    "AND ms.show_date <= ? "
            );
        }


        sql.append(
                "GROUP BY "
                + "ms.show_id, "
                + "h.capacity "
                + ") "
                + "SELECT "
                + "COALESCE(SUM(tickets), 0) AS tickets, "
                + "COALESCE(SUM(capacity), 0) AS capacity "
                + "FROM show_stats"
        );


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int parameterIndex = 1;


            if (fromDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                fromDate
                        )
                );
            }


            if (toDate != null) {

                statement.setDate(
                        parameterIndex++,
                        java.sql.Date.valueOf(
                                toDate
                        )
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    double tickets =
                            result.getDouble(
                                    "tickets"
                            );

                    double capacity =
                            result.getDouble(
                                    "capacity"
                            );


                    if (capacity == 0) {
                        return 0.0;
                    }


                    return
                            (tickets / capacity)
                            * 100.0;
                }
            }


        } catch (SQLException e) {

            e.printStackTrace();
        }


        return 0.0;
    }


    // =========================================================
    // UPDATE STATISTICS
    // =========================================================

    private void updateStatistics(
            double revenue,
            int tickets,
            int shows,
            double occupancy) {

        lblRevenue.setText(
                formatMoney(revenue)
        );

        lblTickets.setText(
                String.valueOf(tickets)
        );

        lblShows.setText(
                String.valueOf(shows)
        );

        lblOccupancy.setText(
                formatPercent(occupancy)
        );
    }


    // =========================================================
    // RESET
    // =========================================================

    private void resetStatistics() {

        lblRevenue.setText(
                "0.00 $"
        );

        lblTickets.setText(
                "0"
        );

        lblShows.setText(
                "0"
        );

        lblOccupancy.setText(
                "0%"
        );
    }


    // =========================================================
    // MONEY
    // =========================================================

    private String formatMoney(
            double amount) {

        return String.format(
                Locale.US,
                "%.2f $",
                amount
        );
    }


    // =========================================================
    // PERCENT
    // =========================================================

    private String formatPercent(
            double value) {

        return String.format(
                Locale.US,
                "%.1f%%",
                value
        );
    }


    // =========================================================
    // EXPORT
    // =========================================================

    @FXML
    private void handleExport(
            ActionEvent event) {

        if (currentReportRows.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Export Report",
                    "Please generate a report first."
            );

            return;
        }


        FileChooser fileChooser =
                new FileChooser();


        fileChooser.setTitle(
                "Save Cinema Report"
        );


        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "CSV Files",
                        "*.csv"
                )
        );


        String reportType =
                cmbReportType.getValue();


        String fileName =
                "Cinema_Report.csv";


        if (reportType != null) {

            if (reportType.equals(
                    "Movie Performance")) {

                fileName =
                        "Movie_Performance_Report.csv";

            } else {

                fileName =
                        "Cinema_Report.csv";
            }
        }


        fileChooser.setInitialFileName(
                fileName
        );


        Stage stage =
                (Stage) btnExport
                        .getScene()
                        .getWindow();


        File file =
                fileChooser.showSaveDialog(stage);


        if (file == null) {
            return;
        }


        try (
                FileWriter writer =
                        new FileWriter(file)
        ) {

            // -------------------------------------------------
            // HEADER
            // -------------------------------------------------

            writer.append(
                    "Cinema:"
            );

            writer.append(
                    escapeCsv(
                            selectedCinemaName
                    )
            );

            writer.append("\n");


            writer.append(
                    "Report:"
            );

            writer.append(
                    escapeCsv(
                            reportType
                    )
            );

            writer.append("\n");


            // -------------------------------------------------
            // DATE FILTER
            // -------------------------------------------------

            writer.append(
                    "From Date:"
            );

            writer.append(
                    dpFromDate.getValue() == null
                            ? "All"
                            : dpFromDate
                                    .getValue()
                                    .toString()
            );

            writer.append("\n");


            writer.append(
                    "To Date:"
            );

            writer.append(
                    dpToDate.getValue() == null
                            ? "All"
                            : dpToDate
                                    .getValue()
                                    .toString()
            );

            writer.append("\n\n");


            // -------------------------------------------------
            // REPORT TABLE
            // -------------------------------------------------

            writer.append(
                    "Name,Shows,Tickets Sold,Revenue,Occupancy\n"
            );


            for (
                    ReportRow row :
                    currentReportRows
            ) {

                writer.append(
                        escapeCsv(
                                row.getName()
                        )
                );

                writer.append(",");

                writer.append(
                        String.valueOf(
                                row.getShows()
                        )
                );

                writer.append(",");

                writer.append(
                        String.valueOf(
                                row.getTickets()
                        )
                );

                writer.append(",");

                writer.append(
                        escapeCsv(
                                row.getRevenue()
                        )
                );

                writer.append(",");

                writer.append(
                        escapeCsv(
                                row.getOccupancy()
                        )
                );

                writer.append("\n");
            }


            // -------------------------------------------------
            // DAILY REVENUE TREND
            // -------------------------------------------------

            writer.append("\n");

            writer.append(
                    "Daily Revenue Trend\n"
            );

            writer.append(
                    "Date,Revenue\n"
            );


            for (
                    TrendRow row :
                    currentTrendRows
            ) {

                writer.append(
                        row.getDate().toString()
                );

                writer.append(",");

                writer.append(
                        escapeCsv(
                                formatMoney(
                                        row.getRevenue()
                                )
                        )
                );

                writer.append("\n");
            }


            // -------------------------------------------------
            // TOTALS
            // -------------------------------------------------

            writer.append("\n");

            writer.append(
                    "Total Revenue,"
            );

            writer.append(
                    escapeCsv(
                            lblRevenue.getText()
                    )
            );

            writer.append("\n");


            writer.append(
                    "Total Tickets,"
            );

            writer.append(
                    lblTickets.getText()
            );

            writer.append("\n");


            writer.append(
                    "Total Shows,"
            );

            writer.append(
                    lblShows.getText()
            );

            writer.append("\n");


            writer.append(
                    "Overall Occupancy,"
            );

            writer.append(
                    lblOccupancy.getText()
            );

            writer.append("\n");


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Export Report",
                    "Report exported successfully."
            );


        } catch (IOException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Export Report",
                    "Could not export the report.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // CSV ESCAPE
    // =========================================================

    private String escapeCsv(
            String value) {

        if (value == null) {
            return "";
        }


        if (
                value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
        ) {

            return "\""
                    + value.replace(
                            "\"",
                            "\"\""
                    )
                    + "\"";
        }


        return value;
    }


    // =========================================================
    // BACK
    // =========================================================

    @FXML
    private void handleBack() {

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
                    employeeName
            );


            Stage stage =
                    (Stage) btnBack
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

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Navigation Error",
                    "Could not return to dashboard.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // GET CURRENT ROLE
    // =========================================================

    private String getCurrentRole() {

        String role =
                Session.getRole();

        if (role == null) {
            role = userRole;
        }

        if (role == null) {
            role = "";
        }

        return role
                .trim()
                .toLowerCase();
    }


    // =========================================================
    // REPORT ROW
    // =========================================================

    public static class ReportRow {

        private final String name;

        private final int shows;

        private final int tickets;

        private final double revenueValue;

        private final double occupancyValue;


        public ReportRow(
                String name,
                int shows,
                int tickets,
                double revenue,
                double occupancy) {

            this.name = name;

            this.shows = shows;

            this.tickets = tickets;

            this.revenueValue = revenue;

            this.occupancyValue = occupancy;
        }


        public String getName() {
            return name;
        }


        public int getShows() {
            return shows;
        }


        public int getTickets() {
            return tickets;
        }


        public String getRevenue() {

            return formatMoney(
                    revenueValue
            );
        }


        public String getOccupancy() {

            return formatPercent(
                    occupancyValue
            );
        }


        public double getRevenueValue() {

            return revenueValue;
        }


        public double getOccupancyValue() {

            return occupancyValue;
        }


        private static String formatMoney(
                double amount) {

            return String.format(
                    Locale.US,
                    "%.2f $",
                    amount
            );
        }


        private static String formatPercent(
                double value) {

            return String.format(
                    Locale.US,
                    "%.1f%%",
                    value
            );
        }
    }


    // =========================================================
    // TREND ROW
    // =========================================================

    public static class TrendRow {

        private final LocalDate date;

        private final double revenue;


        public TrendRow(
                LocalDate date,
                double revenue) {

            this.date = date;

            this.revenue = revenue;
        }


        public LocalDate getDate() {
            return date;
        }


        public double getRevenue() {
            return revenue;
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
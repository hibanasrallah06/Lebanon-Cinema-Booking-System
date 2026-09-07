package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Customer;
import cinemasystem.model.Show;
import cinemasystem.model.Snack;
import cinemasystem.util.EmailService;
import cinemasystem.util.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.Pos;

import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;


public class BookingController {

    private String employeeName;

    // =====================================================
    // EDIT MODE
    // =====================================================

    private Integer editBookingId = null;

    private boolean editMode = false;

    // Keeps the seat buttons so we can select the old
    // booking seats automatically when Edit is opened.
    private final Map<Integer, Button> seatButtons =
            new HashMap<>();


    // =====================================================
    // VIP PRICE
    // =====================================================

    private static final double VIP_EXTRA_PRICE = 5.00;


    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }


    // =====================================================
    // SET BOOKING ID FOR EDIT
    // =====================================================

    public void setEditBookingId(Integer bookingId) {

        if (bookingId == null) {
            return;
        }

        this.editBookingId = bookingId;
        this.editMode = true;

        loadBookingForEdit();
    }


    // =====================================================
    // FXML
    // =====================================================

    @FXML
    private Button btnBack;

    @FXML
    private TextField txtCustomerSearch;

    @FXML
    private Button btnRefresh;

    @FXML
    private Label lblSelectedCustomer;

    @FXML
    private TextField txtShowSearch;

    @FXML
    private Label lblSelectedShow;

    @FXML
    private GridPane seatPane;

    @FXML
    private Label lblSelectedSeats;

    @FXML
    private Label lblInvoiceCustomer;

    @FXML
    private Label lblInvoiceEmployee;

    @FXML
    private Label lblInvoiceMovie;

    @FXML
    private Label lblInvoiceCinema;

    @FXML
    private Label lblInvoiceHall;

    @FXML
    private Label lblInvoiceShow;

    @FXML
    private Label lblInvoiceDate;

    @FXML
    private Label lblInvoiceSeats;

    @FXML
    private Label lblInvoiceTickets;

    @FXML
    private Label lblInvoiceTotal;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnPrint;

    @FXML
    private Button btnEmail;

    @FXML
    private VBox customerResultsPane;

    @FXML
    private VBox showResultsPane;


    // =====================================================
    // SNACKS
    // =====================================================

    private int popcornQty = 0;
    private int pepsiQty = 0;
    private int hotDogQty = 0;
    private int chocolateQty = 0;

    private final double POPCORN_PRICE = 5.00;
    private final double PEPSI_PRICE = 3.00;
    private final double HOTDOG_PRICE = 6.00;
    private final double CHOCOLATE_PRICE = 3.00;

    private double selectedTicketPrice = 0.0;


    // =====================================================
    // SNACK DATA
    // =====================================================

    @FXML
    private VBox snacksPane;

    private final ObservableList<Snack> snacks =
            FXCollections.observableArrayList();

    private final Map<Integer, Integer> selectedSnacks =
            new HashMap<>();


    // =====================================================
    // CUSTOMER DATA
    // =====================================================

    private final ObservableList<Customer> customers =
            FXCollections.observableArrayList();

    private Customer selectedCustomer;


    // =====================================================
    // SHOW DATA
    // =====================================================

    private final ObservableList<Show> shows =
            FXCollections.observableArrayList();

    private Show selectedShow;


    // =====================================================
    // SEAT DATA
    // =====================================================

    private final ObservableList<Integer> selectedSeatIds =
            FXCollections.observableArrayList();

    private final ObservableList<String> selectedSeatNumbers =
            FXCollections.observableArrayList();


    // =====================================================
    // BOOKING STATE
    // =====================================================

    private Integer savedBookingId = null;


    // =====================================================
    // INITIALIZE
    // =====================================================

    @FXML
    private void initialize() {

        loadCustomers();

        setupCustomerSearch();

        loadShows();

        setupShowSearch();

        loadSnacks();

        loadEmployeeInformation();

        clearInvoice();

        lblSelectedCustomer.setText(
                "No customer selected"
        );

        lblSelectedShow.setText(
                "No show selected"
        );

        lblSelectedSeats.setText(
                "Selected seats: None"
        );

        btnPrint.setDisable(true);

        btnEmail.setDisable(true);
    }


    // =====================================================
    // LOAD CUSTOMERS
    // =====================================================

    private void loadCustomers() {

        customers.clear();

        String sql =
                "SELECT customer_id, customer_name, " +
                "customer_phone, customer_email " +
                "FROM customer " +
                "ORDER BY customer_name";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                Customer customer =
                        new Customer(
                                result.getInt("customer_id"),
                                result.getString("customer_name"),
                                result.getString("customer_phone"),
                                result.getString("customer_email")
                        );

                customers.add(customer);
            }

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load customers."
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // CUSTOMER SEARCH
    // =====================================================

    private void setupCustomerSearch() {

        txtCustomerSearch.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    filterCustomers(newValue);
                }
        );
    }


    private void filterCustomers(String searchText) {

        String search =
                searchText
                        .trim()
                        .toLowerCase();

        if (search.isEmpty()) {

            removeCustomerResults();

            return;
        }

        ObservableList<Customer> filtered =
                FXCollections.observableArrayList();

        for (Customer customer : customers) {

            String name =
                    customer.getCustomerName() == null
                    ? ""
                    : customer.getCustomerName().toLowerCase();

            String phone =
                    customer.getCustomerPhone() == null
                    ? ""
                    : customer.getCustomerPhone().toLowerCase();

            String email =
                    customer.getCustomerEmail() == null
                    ? ""
                    : customer.getCustomerEmail().toLowerCase();

            if (
                    name.contains(search)
                    || phone.contains(search)
                    || email.contains(search)
            ) {

                filtered.add(customer);
            }
        }

        showCustomerResults(filtered);
    }


    // =====================================================
    // CUSTOMER RESULTS
    // =====================================================

    private void showCustomerResults(
            ObservableList<Customer> filtered) {

        customerResultsPane
                .getChildren()
                .clear();

        for (Customer customer : filtered) {

            Button button =
                    new Button();

            String phone =
                    customer.getCustomerPhone();

            if (phone == null || phone.isEmpty()) {

                phone = "No phone";
            }

            button.setText(
                    customer.getCustomerName()
                    + "   |   "
                    + phone
            );

            button.setMaxWidth(
                    Double.MAX_VALUE
            );

            button.setPrefHeight(40);

            button.setStyle(
                    "-fx-background-color:white;" +
                    "-fx-border-color:#DDDDDD;" +
                    "-fx-text-fill:#222222;" +
                    "-fx-alignment:CENTER_LEFT;" +
                    "-fx-padding:8;" +
                    "-fx-cursor:hand;"
            );

            button.setOnAction(event -> {

                selectCustomer(customer);

            });

            customerResultsPane
                    .getChildren()
                    .add(button);
        }
    }


    private void removeCustomerResults() {

        customerResultsPane
                .getChildren()
                .clear();
    }


    // =====================================================
    // SELECT CUSTOMER
    // =====================================================

    private void selectCustomer(Customer customer) {

        selectedCustomer = customer;

        lblSelectedCustomer.setText(
                "Selected: "
                + customer.getCustomerName()
        );

        lblInvoiceCustomer.setText(
                customer.getCustomerName()
        );

        txtCustomerSearch.setText(
                customer.getCustomerName()
        );

        customerResultsPane
                .getChildren()
                .clear();
    }


    // =====================================================
    // LOAD SHOWS
    // =====================================================

    private void loadShows() {

        shows.clear();

        String loggedEmployee =
                Session.getFullName();

        String role =
                Session.getRole();

        if (
                loggedEmployee == null ||
                loggedEmployee.trim().isEmpty()
        ) {

            loggedEmployee = this.employeeName;
        }

        if (role == null) {

            role = "";
        }

        role =
                role.trim()
                .toLowerCase();

        String sql;

        boolean isAdmin =
                role.equals("admin");

        if (isAdmin) {

            sql =
                    "SELECT ms.show_id, " +
                    "       m.title AS movie_title, " +
                    "       h.hall_name, " +
                    "       c.cinema_name, " +
                    "       ms.show_date, " +
                    "       ms.start_time, " +
                    "       ms.end_time, " +
                    "       ms.ticket_price " +
                    "FROM movie_show ms " +
                    "JOIN movie m " +
                    "     ON ms.movie_id = m.movie_id " +
                    "JOIN hall h " +
                    "     ON ms.hall_id = h.hall_id " +
                    "JOIN cinema c " +
                    "     ON h.cinema_id = c.cinema_id " +
                    "WHERE TIMESTAMP(ms.show_date, ms.end_time) > NOW() " +
                    "ORDER BY ms.show_date, ms.start_time";

        } else {

            if (
                    loggedEmployee == null ||
                    loggedEmployee.trim().isEmpty()
            ) {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Employee Error",
                        "Could not identify the logged-in employee."
                );

                return;
            }

            sql =
                    "SELECT ms.show_id, " +
                    "       m.title AS movie_title, " +
                    "       h.hall_name, " +
                    "       c.cinema_name, " +
                    "       ms.show_date, " +
                    "       ms.start_time, " +
                    "       ms.end_time, " +
                    "       ms.ticket_price " +
                    "FROM movie_show ms " +
                    "JOIN movie m " +
                    "     ON ms.movie_id = m.movie_id " +
                    "JOIN hall h " +
                    "     ON ms.hall_id = h.hall_id " +
                    "JOIN cinema c " +
                    "     ON h.cinema_id = c.cinema_id " +
                    "JOIN employee e " +
                    "     ON e.cinema_id = c.cinema_id " +
                    "WHERE e.full_name = ? " +
                    "AND TIMESTAMP(ms.show_date, ms.end_time) > NOW() " +
                    "ORDER BY ms.show_date, ms.start_time";
        }

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            if (!isAdmin) {

                statement.setString(
                        1,
                        loggedEmployee
                );
            }

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    Show show =
                            new Show(
                                    result.getInt("show_id"),
                                    result.getString("movie_title"),
                                    result.getString("hall_name"),
                                    result.getString("cinema_name"),
                                    result.getString("show_date"),
                                    result.getString("start_time"),
                                    result.getString("end_time"),
                                    result.getDouble("ticket_price")
                            );

                    shows.add(show);
                }
            }

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load shows."
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // SHOW SEARCH
    // =====================================================

    private void setupShowSearch() {

        txtShowSearch.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    filterShows(newValue);
                }
        );
    }


    private void filterShows(String searchText) {

        String search =
                searchText
                        .trim()
                        .toLowerCase();

        if (search.isEmpty()) {

            removeShowResults();

            return;
        }

        ObservableList<Show> filtered =
                FXCollections.observableArrayList();

        for (Show show : shows) {

            String movie =
                    show.getMovie() == null
                    ? ""
                    : show.getMovie().toLowerCase();

            String hall =
                    show.getHall() == null
                    ? ""
                    : show.getHall().toLowerCase();

            String cinema =
                    show.getCinemaName() == null
                    ? ""
                    : show.getCinemaName().toLowerCase();

            String date =
                    show.getShowDate() == null
                    ? ""
                    : show.getShowDate().toLowerCase();

            if (
                    movie.contains(search)
                    || hall.contains(search)
                    || cinema.contains(search)
                    || date.contains(search)
            ) {

                filtered.add(show);
            }
        }

        showShowResults(filtered);
    }


    // =====================================================
    // SHOW RESULTS
    // =====================================================

    private void showShowResults(
            ObservableList<Show> filtered) {

        showResultsPane
                .getChildren()
                .clear();

        for (Show show : filtered) {

            Button button =
                    new Button();

            button.setText(
                    show.getMovie()
                    + "   |   "
                    + show.getCinemaName()
                    + "   |   "
                    + show.getHall()
                    + "   |   "
                    + show.getShowDate()
                    + "   "
                    + show.getStartTime()
            );

            button.setMaxWidth(
                    Double.MAX_VALUE
            );

            button.setPrefHeight(45);

            button.setStyle(
                    "-fx-background-color:white;" +
                    "-fx-border-color:#DDDDDD;" +
                    "-fx-text-fill:#222222;" +
                    "-fx-alignment:CENTER_LEFT;" +
                    "-fx-padding:8;" +
                    "-fx-cursor:hand;"
            );

            button.setOnAction(event -> {

                selectShow(show);

            });

            showResultsPane
                    .getChildren()
                    .add(button);
        }
    }


    private void removeShowResults() {

        showResultsPane
                .getChildren()
                .clear();
    }


    // =====================================================
    // SELECT SHOW
    // =====================================================

    private void selectShow(Show show) {

        selectedShow = show;

        if (!editMode) {
            savedBookingId = null;
        }

        selectedSeatIds.clear();

        selectedSeatNumbers.clear();

        selectedSnacks.clear();

        displaySnacks();

        seatButtons.clear();

        lblSelectedSeats.setText(
                "Selected seats: None"
        );

        lblInvoiceSeats.setText("-");

        lblInvoiceTickets.setText("0");

        lblInvoiceTotal.setText(
                "0.00 $"
        );

        lblSelectedShow.setText(
                "Selected: "
                + show.getMovie()
                + " | "
                + show.getCinemaName()
                + " | "
                + show.getShowDate()
                + " "
                + show.getStartTime()
        );

        txtShowSearch.setText(
                show.getMovie()
        );

        lblInvoiceMovie.setText(
                show.getMovie()
        );

        lblInvoiceCinema.setText(
                show.getCinemaName()
        );

        lblInvoiceHall.setText(
                show.getHall()
        );

        lblInvoiceShow.setText(
                show.getStartTime()
                + " - "
                + show.getEndTime()
        );

        lblInvoiceDate.setText(
                show.getShowDate()
        );

        showResultsPane
                .getChildren()
                .clear();

        selectedTicketPrice =
                getTicketPrice(
                        show.getShowId()
                );

        loadSeatsForSelectedShow();

        if (!editMode) {

            btnPrint.setDisable(true);

            btnEmail.setDisable(true);
        }
    }


    // =====================================================
    // GET TICKET PRICE
    // =====================================================

    private double getTicketPrice(int showId) {

        String sql =
                "SELECT ticket_price " +
                "FROM movie_show " +
                "WHERE show_id = ?";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    showId
            );

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getDouble(
                            "ticket_price"
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return 0.0;
    }


    // =====================================================
    // GET HALL ID
    // =====================================================

    private int getHallId(int showId) {

        String sql =
                "SELECT hall_id " +
                "FROM movie_show " +
                "WHERE show_id = ?";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    showId
            );

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getInt(
                            "hall_id"
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return -1;
    }


    // =====================================================
    // GET SEAT PRICE
    // =====================================================

    private double getSeatPrice(String seatType) {

        if (
                seatType == null ||
                !seatType.trim()
                        .equalsIgnoreCase("VIP")
        ) {

            return selectedTicketPrice;
        }

        return selectedTicketPrice + VIP_EXTRA_PRICE;
    }


    // =====================================================
    // GET SEAT TYPE
    // =====================================================

    private String getSeatType(
            Connection connection,
            int seatId) throws Exception {

        String sql =
                "SELECT seat_type " +
                "FROM seat " +
                "WHERE seat_id = ?";

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    seatId
            );

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    String type =
                            result.getString(
                                    "seat_type"
                            );

                    if (
                            type == null ||
                            type.trim().isEmpty()
                    ) {

                        return "Standard";
                    }

                    return type;
                }
            }
        }

        return "Standard";
    }


    // =====================================================
    // GET SELECTED SEATS TOTAL
    // =====================================================

    private double getSelectedSeatsTotal() {

        if (selectedSeatIds.isEmpty()) {

            return 0.0;
        }

        double total = 0.0;

        String placeholders =
                createPlaceholders(
                        selectedSeatIds.size()
                );

        String sql =
                "SELECT seat_id, seat_type " +
                "FROM seat " +
                "WHERE seat_id IN (" +
                placeholders +
                ")";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            int index = 1;

            for (Integer seatId :
                    selectedSeatIds) {

                statement.setInt(
                        index++,
                        seatId
                );
            }

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    String seatType =
                            result.getString(
                                    "seat_type"
                            );

                    total +=
                            getSeatPrice(seatType);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return total;
    }


    // =====================================================
    // LOAD SNACKS
    // =====================================================

    private void loadSnacks() {

        snacks.clear();

        selectedSnacks.clear();

        String sql =
                "SELECT snack_id, snack_name, price, active " +
                "FROM snack " +
                "WHERE active = TRUE " +
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

            displaySnacks();

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Snack Error",
                    "Could not load snacks.\n\n"
                    + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // LOAD SEATS FOR SELECTED SHOW
    // =====================================================

    private void loadSeatsForSelectedShow() {

        if (selectedShow == null) {

            return;
        }

        int hallId =
                getHallId(
                        selectedShow.getShowId()
                );

        if (hallId == -1) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Seat Error",
                    "Could not find the hall for this show."
            );

            return;
        }

        loadSeats(
                hallId,
                selectedShow.getShowId()
        );
    }


    // =====================================================
    // LOAD SEATS
    // =====================================================

    private void loadSeats(
            int hallId,
            int showId) {

        seatPane.getChildren().clear();

        seatButtons.clear();

        seatPane.setHgap(8);

        seatPane.setVgap(10);

        seatPane.setAlignment(
                Pos.CENTER
        );

        int capacity = 0;

        String capacitySql =
                "SELECT capacity " +
                "FROM hall " +
                "WHERE hall_id = ?";

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                capacitySql
                        )
        ) {

            statement.setInt(
                    1,
                    hallId
            );

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    capacity =
                            result.getInt(
                                    "capacity"
                            );
                }
            }

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Seat Error",
                    "Could not get hall capacity."
            );

            e.printStackTrace();

            return;
        }

        if (capacity <= 0) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Seat Error",
                    "Invalid hall capacity."
            );

            return;
        }


        // =================================================
        // CHECK CURRENT BOOKING ID
        // =================================================

        int currentBookingId =
                editBookingId == null
                ? 0
                : editBookingId;


        // =================================================
        // GET SEATS
        // =================================================

        String sql =
                "SELECT s.seat_id, " +
                "       s.seat_number, " +
                "       s.seat_row, " +
                "       s.seat_type, " +

                "       CASE " +
                "         WHEN EXISTS (" +
                "           SELECT 1 " +
                "           FROM booking_seat bs " +
                "           JOIN booking b " +
                "             ON bs.booking_id = b.booking_id " +
                "           WHERE bs.seat_id = s.seat_id " +
                "             AND b.show_id = ? " +
                "             AND b.status = 'Confirmed' " +
                "             AND b.booking_id <> ?" +
                "         ) THEN 1 " +
                "         ELSE 0 " +
                "       END AS is_booked " +

                "FROM seat s " +

                "WHERE s.hall_id = ? " +

                "ORDER BY " +
                "  s.seat_row, " +
                "  CAST(s.seat_number AS UNSIGNED) " +

                "LIMIT ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            int index = 1;

            statement.setInt(
                    index++,
                    showId
            );

            statement.setInt(
                    index++,
                    currentBookingId
            );

            statement.setInt(
                    index++,
                    hallId
            );

            statement.setInt(
                    index,
                    capacity
            );


            Map<String, Integer> rowIndexes =
                    new HashMap<>();

            int nextRowIndex = 0;


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    int seatId =
                            result.getInt(
                                    "seat_id"
                            );

                    String seatNumber =
                            result.getString(
                                    "seat_number"
                            );

                    String seatRow =
                            result.getString(
                                    "seat_row"
                            );

                    String dbSeatType =
                            result.getString(
                                    "seat_type"
                            );


                    final String seatType;

                    if (
                            dbSeatType == null ||
                            dbSeatType.trim().isEmpty()
                    ) {

                        seatType = "Standard";

                    } else {

                        seatType =
                                dbSeatType.trim();
                    }


                    boolean booked =
                            result.getInt(
                                    "is_booked"
                            ) == 1;


                    if (!rowIndexes.containsKey(seatRow)) {

                        rowIndexes.put(
                                seatRow,
                                nextRowIndex
                        );

                        nextRowIndex++;
                    }


                    int gridRow =
                            rowIndexes.get(
                                    seatRow
                            );


                    int gridColumn;

                    try {

                        gridColumn =
                                Integer.parseInt(
                                        seatNumber
                                ) - 1;

                    } catch (NumberFormatException e) {

                        String number =
                                seatNumber.replaceAll(
                                        "\\D+",
                                        ""
                                );

                        gridColumn =
                                Integer.parseInt(
                                        number
                                ) - 1;
                    }


                    String displayNumber =
                            seatRow + seatNumber;


                    Button seatButton =
                            new Button(
                                    displayNumber
                            );


                    seatButton.setPrefWidth(48);
                    seatButton.setMinWidth(48);
                    seatButton.setMaxWidth(48);

                    seatButton.setPrefHeight(42);
                    seatButton.setMinHeight(42);
                    seatButton.setMaxHeight(42);


                    // =================================================
                    // SAVE BUTTON REFERENCE
                    // =================================================

                    seatButtons.put(
                            seatId,
                            seatButton
                    );
                    
                    boolean currentlySelected =
        selectedSeatIds.contains(seatId);


                    // =================================================
                    // BOOKED BY ANOTHER BOOKING
                    // =================================================

                   if (booked) {

    seatButton.setDisable(true);

    seatButton.setStyle(
            "-fx-background-color:#555555;" +
            "-fx-text-fill:#AAAAAA;" +
            "-fx-font-weight:bold;" +
            "-fx-background-radius:12 12 6 6;" +
            "-fx-border-radius:12 12 6 6;" +
            "-fx-border-color:#666666;" +
            "-fx-border-width:1;" +
            "-fx-opacity:0.75;"
    );

} else if (currentlySelected) {

    applySelectedSeatStyle(
            seatButton
    );

} else {

    applySeatStyle(
            seatButton,
            seatType
    );

    seatButton.setOnAction(event -> {

        toggleSeat(
                seatButton,
                seatId,
                displayNumber,
                seatType
        );
    });
}


                    double seatPrice =
                            getSeatPrice(
                                    seatType
                            );


                    Tooltip tooltip =
                            new Tooltip(
                                    seatType
                                    + " Seat\n"
                                    + "Price: "
                                    + String.format(
                                            Locale.US,
                                            "%.2f $",
                                            seatPrice
                                    )
                            );


                    Tooltip.install(
                            seatButton,
                            tooltip
                    );


                    seatPane.add(
                            seatButton,
                            gridColumn,
                            gridRow
                    );
                }
            }

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load seats.\n\n"
                    + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =====================================================
// TOGGLE SEAT
// =====================================================

private void toggleSeat(
        Button seatButton,
        int seatId,
        String seatNumber,
        String seatType) {

    // =================================================
    // UNSELECT
    // =================================================

    if (selectedSeatIds.contains(seatId)) {

        selectedSeatIds.remove(
                Integer.valueOf(seatId)
        );

        selectedSeatNumbers.remove(
                seatNumber
        );

        applySeatStyle(
                seatButton,
                seatType
        );

    }

    // =================================================
    // SELECT
    // =================================================

    else {

        selectedSeatIds.add(
                seatId
        );

        selectedSeatNumbers.add(
                seatNumber
        );

        applySelectedSeatStyle(
                seatButton
        );
    }

    updateInvoiceTotal();
}


// =====================================================
// APPLY NORMAL SEAT STYLE
// =====================================================

private void applySeatStyle(
        Button seatButton,
        String seatType) {

    if (
            seatType != null &&
            seatType.trim()
                    .equalsIgnoreCase("VIP")
    ) {

        seatButton.setStyle(
                "-fx-background-color:linear-gradient(to bottom,#B12BC7,#7B1FA2);" +
                "-fx-text-fill:white;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:12 12 6 6;" +
                "-fx-border-radius:12 12 6 6;" +
                "-fx-border-color:#D96BE8;" +
                "-fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.45),7,0,0,3);" +
                "-fx-cursor:hand;"
        );

    } else {

        seatButton.setStyle(
                "-fx-background-color:linear-gradient(to bottom,#4A4A4A,#292929);" +
                "-fx-text-fill:white;" +
                "-fx-font-weight:bold;" +
                "-fx-border-color:#777777;" +
                "-fx-border-width:1;" +
                "-fx-border-radius:12 12 6 6;" +
                "-fx-background-radius:12 12 6 6;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),7,0,0,3);" +
                "-fx-cursor:hand;"
        );
    }
}


// =====================================================
// APPLY SELECTED SEAT STYLE
// =====================================================

private void applySelectedSeatStyle(
        Button seatButton) {

    seatButton.setStyle(
            "-fx-background-color:#4CAF50;" +
            "-fx-text-fill:white;" +
            "-fx-font-weight:bold;" +
            "-fx-background-radius:12 12 6 6;" +
            "-fx-border-radius:12 12 6 6;" +
            "-fx-border-color:#81C784;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.45),7,0,0,3);" +
            "-fx-cursor:hand;"
    );
}


    // =====================================================
    // DISPLAY SNACKS
    // =====================================================

    private void displaySnacks() {

        snacksPane.getChildren().clear();

        for (Snack snack : snacks) {

            HBox row =
                    createSnackRow(snack);

            snacksPane
                    .getChildren()
                    .add(row);
        }
    }


    // =====================================================
    // CREATE SNACK ROW
    // =====================================================

    private HBox createSnackRow(
            Snack snack) {

        HBox row =
                new HBox(8);

        row.setAlignment(
                Pos.CENTER_LEFT
        );


        Label nameLabel =
                new Label(
                        snack.getSnackName()
                        + "  "
                        + String.format(
                                Locale.US,
                                "%.2f $",
                                snack.getPrice()
                        )
                );


        nameLabel.setPrefWidth(150);


        nameLabel.setStyle(
                "-fx-font-size:13px;" +
                "-fx-text-fill:#333333;"
        );


        Button minusButton =
                new Button("−");


        Button plusButton =
                new Button("+");


        Label quantityLabel =
                new Label(
                        String.valueOf(
                                selectedSnacks.getOrDefault(
                                        snack.getSnackId(),
                                        0
                                )
                        )
                );


        quantityLabel.setPrefWidth(30);

        quantityLabel.setAlignment(
                Pos.CENTER
        );


        minusButton.setPrefWidth(30);

        plusButton.setPrefWidth(30);


        minusButton.setStyle(
                "-fx-background-color:#EEEEEE;" +
                "-fx-font-weight:bold;" +
                "-fx-cursor:hand;"
        );


        plusButton.setStyle(
                "-fx-background-color:#2E7D32;" +
                "-fx-text-fill:white;" +
                "-fx-font-weight:bold;" +
                "-fx-cursor:hand;"
        );


        // =================================================
        // MINUS
        // =================================================

        minusButton.setOnAction(event -> {

            int quantity =
                    selectedSnacks.getOrDefault(
                            snack.getSnackId(),
                            0
                    );


            if (quantity > 0) {

                quantity--;


                if (quantity == 0) {

                    selectedSnacks.remove(
                            snack.getSnackId()
                    );

                } else {

                    selectedSnacks.put(
                            snack.getSnackId(),
                            quantity
                    );
                }


                quantityLabel.setText(
                        String.valueOf(
                                quantity
                        )
                );


                updateInvoiceTotal();
            }
        });


        // =================================================
        // PLUS
        // =================================================

        plusButton.setOnAction(event -> {

            int quantity =
                    selectedSnacks.getOrDefault(
                            snack.getSnackId(),
                            0
                    );


            quantity++;


            selectedSnacks.put(
                    snack.getSnackId(),
                    quantity
            );


            quantityLabel.setText(
                    String.valueOf(
                            quantity
                    )
            );


            updateInvoiceTotal();
        });


        row.getChildren().addAll(
                nameLabel,
                minusButton,
                quantityLabel,
                plusButton
        );


        return row;
    }


    // =====================================================
    // GET SNACK TOTAL
    // =====================================================

    private double getSnackTotal() {

        double total = 0.0;

        for (Snack snack : snacks) {

            int quantity =
                    selectedSnacks.getOrDefault(
                            snack.getSnackId(),
                            0
                    );

            total +=
                    quantity
                    * snack.getPrice();
        }

        return total;
    }


    // =====================================================
    // UPDATE INVOICE
    // =====================================================

    private void updateInvoiceTotal() {

        int tickets =
                selectedSeatIds.size();

        double ticketTotal =
                getSelectedSeatsTotal();

        double snackTotal =
                getSnackTotal();

        double total =
                ticketTotal + snackTotal;


        if (tickets == 0) {

            lblSelectedSeats.setText(
                    "Selected seats: None"
            );

            lblInvoiceSeats.setText("-");

        } else {

            lblSelectedSeats.setText(
                    "Selected seats: "
                    + String.join(
                            ", ",
                            selectedSeatNumbers
                    )
            );

            lblInvoiceSeats.setText(
                    String.join(
                            ", ",
                            selectedSeatNumbers
                    )
            );
        }


        lblInvoiceTickets.setText(
                String.valueOf(tickets)
        );


        lblInvoiceTotal.setText(
                String.format(
                        Locale.US,
                        "%.2f $",
                        total
                )
        );
    }


    // =====================================================
    // EMPLOYEE
    // =====================================================

    private void loadEmployeeInformation() {

        String name =
                Session.getFullName();

        if (
                name == null ||
                name.isEmpty()
        ) {

            name = employeeName;
        }

        if (
                name == null ||
                name.isEmpty()
        ) {

            name = "Unknown";
        }

        lblInvoiceEmployee.setText(
                name
        );
    }


    // =====================================================
    // GET EMPLOYEE ID
    // =====================================================

    private int getEmployeeId(
            Connection connection) throws Exception {

        String name =
                Session.getFullName();

        if (
                name == null ||
                name.isEmpty()
        ) {

            name = employeeName;
        }

        if (
                name == null ||
                name.isEmpty()
        ) {

            throw new Exception(
                    "No logged-in employee found."
            );
        }


        String sql =
                "SELECT employee_id " +
                "FROM employee " +
                "WHERE full_name = ?";


        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    name
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getInt(
                            "employee_id"
                    );
                }
            }
        }


        throw new Exception(
                "Employee not found: "
                + name
        );
    }


    // =====================================================
    // LOAD BOOKING FOR EDIT
    // =====================================================

    private void loadBookingForEdit() {

        if (editBookingId == null) {
            return;
        }


        String sql =
                "SELECT b.booking_id, " +
                "       b.customer_id, " +
                "       b.show_id, " +
                "       b.total_amount, " +

                "       c.customer_name, " +
                "       c.customer_phone, " +
                "       c.customer_email, " +

                "       m.title AS movie_title, " +
                "       h.hall_name, " +
                "       ci.cinema_name, " +
                "       ms.show_date, " +
                "       ms.start_time, " +
                "       ms.end_time, " +
                "       ms.ticket_price " +

                "FROM booking b " +

                "JOIN customer c " +
                "     ON b.customer_id = c.customer_id " +

                "JOIN movie_show ms " +
                "     ON b.show_id = ms.show_id " +

                "JOIN movie m " +
                "     ON ms.movie_id = m.movie_id " +

                "JOIN hall h " +
                "     ON ms.hall_id = h.hall_id " +

                "JOIN cinema ci " +
                "     ON h.cinema_id = ci.cinema_id " +

                "WHERE b.booking_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    editBookingId
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (!result.next()) {

                    showAlert(
                            Alert.AlertType.ERROR,
                            "Booking Error",
                            "Booking #" +
                            editBookingId +
                            " was not found."
                    );

                    return;
                }


                int customerId =
                        result.getInt(
                                "customer_id"
                        );

                int showId =
                        result.getInt(
                                "show_id"
                        );


                // =================================================
                // CUSTOMER
                // =================================================

                selectedCustomer =
                        findCustomerById(
                                customerId
                        );


                if (selectedCustomer == null) {

                    selectedCustomer =
                            new Customer(
                                    customerId,
                                    result.getString(
                                            "customer_name"
                                    ),
                                    result.getString(
                                            "customer_phone"
                                    ),
                                    result.getString(
                                            "customer_email"
                                    )
                            );
                    customers.add(
                            selectedCustomer
                    );
                }


                selectCustomer(
                        selectedCustomer
                );


                // =================================================
                // SHOW
                // =================================================

                Show bookingShow =
                        findShowById(
                                showId
                        );


                if (bookingShow == null) {

                    bookingShow =
                            new Show(
                                    showId,
                                    result.getString(
                                            "movie_title"
                                    ),
                                    result.getString(
                                            "hall_name"
                                    ),
                                    result.getString(
                                            "cinema_name"
                                    ),
                                    result.getString(
                                            "show_date"
                                    ),
                                    result.getString(
                                            "start_time"
                                    ),
                                    result.getString(
                                            "end_time"
                                    ),
                                    result.getDouble(
                                            "ticket_price"
                                    )
                            );

                    shows.add(
                            bookingShow
                    );
                }


                // =================================================
                // SELECT SHOW
                // =================================================

                selectedShow =
                        bookingShow;

                selectedTicketPrice =
                        result.getDouble(
                                "ticket_price"
                        );


                lblSelectedShow.setText(
                        "Selected: "
                        + bookingShow.getMovie()
                        + " | "
                        + bookingShow.getCinemaName()
                        + " | "
                        + bookingShow.getShowDate()
                        + " "
                        + bookingShow.getStartTime()
                );


                txtShowSearch.setText(
                        bookingShow.getMovie()
                );


                lblInvoiceMovie.setText(
                        bookingShow.getMovie()
                );

                lblInvoiceCinema.setText(
                        bookingShow.getCinemaName()
                );

                lblInvoiceHall.setText(
                        bookingShow.getHall()
                );

                lblInvoiceShow.setText(
                        bookingShow.getStartTime()
                        + " - "
                        + bookingShow.getEndTime()
                );

                lblInvoiceDate.setText(
                        bookingShow.getShowDate()
                );


                // =================================================
                // LOAD SEATS
                // =================================================

                selectedSeatIds.clear();

                selectedSeatNumbers.clear();

                loadSeatsForSelectedShow();


                // =================================================
                // LOAD OLD BOOKING SEATS
                // =================================================

                loadBookingSeatsForEdit();


                // =================================================
                // LOAD OLD SNACKS
                // =================================================

                loadBookingSnacksForEdit();


                // =================================================
                // UPDATE INVOICE
                // =================================================

                updateInvoiceTotal();


                savedBookingId =
                        editBookingId;


                btnPrint.setDisable(false);

                btnEmail.setDisable(false);


                // =================================================
                // CHANGE SAVE BUTTON TEXT
                // =================================================

                btnSave.setText(
                        "Update Booking"
                );
            }

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Edit Booking Error",
                    "Could not load booking for editing.\n\n"
                    + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // FIND CUSTOMER
    // =====================================================

    private Customer findCustomerById(
            int customerId) {

        for (Customer customer : customers) {

            if (
                    customer.getCustomerId()
                    == customerId
            ) {

                return customer;
            }
        }

        return null;
    }


    // =====================================================
    // FIND SHOW
    // =====================================================

    private Show findShowById(
            int showId) {

        for (Show show : shows) {

            if (
                    show.getShowId()
                    == showId
            ) {

                return show;
            }
        }

        return null;
    }


    // =====================================================
    // LOAD BOOKING SEATS FOR EDIT
    // =====================================================

    private void loadBookingSeatsForEdit() {

        if (editBookingId == null) {
            return;
        }


        String sql =
                "SELECT bs.seat_id, " +
                "       s.seat_number, " +
                "       s.seat_row, " +
                "       s.seat_type " +

                "FROM booking_seat bs " +

                "JOIN seat s " +
                "     ON bs.seat_id = s.seat_id " +

                "WHERE bs.booking_id = ? " +

                "ORDER BY " +
                "s.seat_row, " +
                "CAST(s.seat_number AS UNSIGNED)";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    editBookingId
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    int seatId =
                            result.getInt(
                                    "seat_id"
                            );

                    String seatNumber =
                            result.getString(
                                    "seat_number"
                            );

                    String seatRow =
                            result.getString(
                                    "seat_row"
                            );


                    String displayNumber =
                            seatRow + seatNumber;


                    Button seatButton =
                            seatButtons.get(
                                    seatId
                            );


                    if (seatButton != null) {

                        String seatType =
                                result.getString(
                                        "seat_type"
                                );

                        if (
                                seatType == null ||
                                seatType.trim().isEmpty()
                        ) {

                            seatType = "Standard";
                        }


                        selectedSeatIds.add(
                                seatId
                        );

                        selectedSeatNumbers.add(
                                displayNumber
                        );


                        applySelectedSeatStyle(
        seatButton
);
                    }
                }
            }

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Seat Error",
                    "Could not load booking seats."
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // LOAD BOOKING SNACKS FOR EDIT
    // =====================================================

    private void loadBookingSnacksForEdit() {

        if (editBookingId == null) {
            return;
        }


        selectedSnacks.clear();


        String sql =
                "SELECT snack_id, quantity " +
                "FROM booking_snack " +
                "WHERE booking_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    editBookingId
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    int snackId =
                            result.getInt(
                                    "snack_id"
                            );

                    int quantity =
                            result.getInt(
                                    "quantity"
                            );


                    if (quantity > 0) {

                        selectedSnacks.put(
                                snackId,
                                quantity
                        );
                    }
                }
            }


            displaySnacks();

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Snack Error",
                    "Could not load booking snacks."
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // RESET BOOKING FORM
    // =====================================================

    private void resetBookingForm() {

        savedBookingId = null;

        editBookingId = null;

        editMode = false;


        selectedCustomer = null;

        txtCustomerSearch.clear();

        lblSelectedCustomer.setText(
                "No customer selected"
        );

        removeCustomerResults();


        selectedShow = null;

        selectedTicketPrice = 0.0;

        txtShowSearch.clear();

        lblSelectedShow.setText(
                "No show selected"
        );

        removeShowResults();


        selectedSeatIds.clear();

        selectedSeatNumbers.clear();

        seatButtons.clear();

        seatPane.getChildren().clear();

        lblSelectedSeats.setText(
                "Selected seats: None"
        );


        selectedSnacks.clear();

        popcornQty = 0;

        pepsiQty = 0;

        hotDogQty = 0;

        chocolateQty = 0;

        displaySnacks();


        clearInvoice();


        btnSave.setText(
                "Save Booking"
        );

        btnPrint.setDisable(true);

        btnEmail.setDisable(true);
    }


    // =====================================================
    // NEW BOOKING
    // =====================================================

    @FXML
    private void handleNewBooking(ActionEvent event) {

        resetBookingForm();

        txtCustomerSearch.requestFocus();
    }


    // =====================================================
    // SAVE / UPDATE BOOKING
    // =====================================================

    @FXML
    private void handleSave(
            ActionEvent event) {

        if (selectedCustomer == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Customer",
                    "Please select a customer first."
            );

            return;
        }


        if (selectedShow == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Show",
                    "Please select a show first."
            );

            return;
        }


        if (selectedSeatIds.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Seats",
                    "Please select at least one seat."
            );

            return;
        }


        // =================================================
        // TOTAL
        // =================================================

        double ticketTotal =
                getSelectedSeatsTotal();

        double snackTotal =
                getSnackTotal();

        double total =
                ticketTotal
                + snackTotal;


        Connection connection = null;


        try {

            connection =
                    DBConnection.getConnection();

            connection.setAutoCommit(false);


            // =================================================
            // EMPLOYEE
            // =================================================

            int employeeId =
                    getEmployeeId(
                            connection
                    );


            // =================================================
            // HALL
            // =================================================

            int hallId =
                    getHallId(
                            selectedShow.getShowId()
                    );


            if (hallId == -1) {

                throw new Exception(
                        "Could not find hall."
                );
            }


            // =================================================
            // CHECK SEATS
            // =================================================

            String checkSql =
                    "SELECT s.seat_id " +
                    "FROM seat s " +
                    "WHERE s.hall_id = ? " +
                    "AND s.seat_id IN (" +
                    createPlaceholders(
                            selectedSeatIds.size()
                    ) +
                    ") " +

                    "AND NOT EXISTS (" +
                    "   SELECT 1 " +
                    "   FROM booking_seat bs " +
                    "   JOIN booking b " +
                    "     ON bs.booking_id = b.booking_id " +
                    "   WHERE bs.seat_id = s.seat_id " +
                    "     AND b.show_id = ? " +
                    "     AND b.status = 'Confirmed' " +
                    "     AND b.booking_id <> ?" +
                    ") " +

                    "FOR UPDATE";


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    checkSql
                            )
            ) {

                int index = 1;


                statement.setInt(
                        index++,
                        hallId
                );


                for (
                        Integer seatId :
                        selectedSeatIds
                ) {

                    statement.setInt(
                            index++,
                            seatId
                    );
                }


                statement.setInt(
                        index++,
                        selectedShow.getShowId()
                );


                statement.setInt(
                        index,
                        editBookingId == null
                        ? 0
                        : editBookingId
                );


                List<Integer> availableSeats =
                        new ArrayList<>();


                try (
                        ResultSet result =
                                statement.executeQuery()
                ) {

                    while (result.next()) {

                        availableSeats.add(
                                result.getInt(
                                        "seat_id"
                                )
                        );
                    }
                }


                if (
                        availableSeats.size()
                        != selectedSeatIds.size()
                ) {

                    connection.rollback();


                    showAlert(
                            Alert.AlertType.WARNING,
                            "Seat Already Booked",
                            "One or more selected seats " +
                            "have already been booked. " +
                            "Please select different seats."
                    );


                    loadSeatsForSelectedShow();


                    selectedSeatIds.clear();

                    selectedSeatNumbers.clear();


                    if (editMode) {

                        loadBookingSeatsForEdit();
                    }


                    updateInvoiceTotal();


                    return;
                }
            }


            // =================================================
            // EDIT EXISTING BOOKING
            // =================================================

            if (editMode && editBookingId != null) {

                String updateSql =
                        "UPDATE booking " +
                        "SET employee_id = ?, " +
                        "    show_id = ?, " +
                        "    customer_id = ?, " +
                        "    status = ?, " +
                        "    total_amount = ? " +
                        "WHERE booking_id = ?";


                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        updateSql
                                )
                ) {

                    statement.setInt(
                            1,
                            employeeId
                    );

                    statement.setInt(
                            2,
                            selectedShow.getShowId()
                    );

                    statement.setInt(
                            3,
                            selectedCustomer.getCustomerId()
                    );

                    statement.setString(
                            4,
                            "Confirmed"
                    );

                    statement.setDouble(
                            5,
                            total
                    );

                    statement.setInt(
                            6,
                            editBookingId
                    );


                    int updated =
                            statement.executeUpdate();


                    if (updated == 0) {

                        throw new Exception(
                                "Booking was not found."
                        );
                    }
                }


                // =================================================
                // DELETE OLD SEATS
                // =================================================

                String deleteSeatsSql =
                        "DELETE FROM booking_seat " +
                        "WHERE booking_id = ?";


                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        deleteSeatsSql
                                )
                ) {

                    statement.setInt(
                            1,
                            editBookingId
                    );

                    statement.executeUpdate();
                }


                // =================================================
                // DELETE OLD SNACKS
                // =================================================

                String deleteSnacksSql =
                        "DELETE FROM booking_snack " +
                        "WHERE booking_id = ?";


                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        deleteSnacksSql
                                )
                ) {

                    statement.setInt(
                            1,
                            editBookingId
                    );

                    statement.executeUpdate();
                }


                savedBookingId =
                        editBookingId;
            }


            // =================================================
            // NEW BOOKING
            // =================================================

            else {

                String bookingSql =
                        "INSERT INTO booking " +
                        "(employee_id, show_id, customer_id, " +
                        "status, total_amount) " +
                        "VALUES (?, ?, ?, ?, ?)";


                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        bookingSql,
                                        java.sql.Statement.RETURN_GENERATED_KEYS
                                )
                ) {

                    statement.setInt(
                            1,
                            employeeId
                    );

                    statement.setInt(
                            2,
                            selectedShow.getShowId()
                    );

                    statement.setInt(
                            3,
                            selectedCustomer.getCustomerId()
                    );

                    statement.setString(
                            4,
                            "Confirmed"
                    );

                    statement.setDouble(
                            5,
                            total
                    );


                    statement.executeUpdate();


                    try (
                            ResultSet keys =
                                    statement.getGeneratedKeys()
                    ) {

                        if (!keys.next()) {

                            throw new Exception(
                                    "Could not get booking ID."
                            );
                        }


                        savedBookingId =
                                keys.getInt(1);
                    }
                }
            }


            // =================================================
            // INSERT BOOKING SEATS
            // =================================================

            String seatSql =
                    "INSERT INTO booking_seat " +
                    "(booking_id, seat_id, price) " +
                    "VALUES (?, ?, ?)";


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    seatSql
                            )
            ) {

                for (
                        Integer seatId :
                        selectedSeatIds
                ) {

                    String seatType =
                            getSeatType(
                                    connection,
                                    seatId
                            );


                    double seatPrice =
                            getSeatPrice(
                                    seatType
                            );


                    statement.setInt(
                            1,
                            savedBookingId
                    );

                    statement.setInt(
                            2,
                            seatId
                    );

                    statement.setDouble(
                            3,
                            seatPrice
                    );


                    statement.addBatch();
                }


                statement.executeBatch();
            }


            // =================================================
            // INSERT BOOKING SNACKS
            // =================================================

            String snackSql =
                    "INSERT INTO booking_snack " +
                    "(booking_id, snack_id, quantity, unit_price) " +
                    "VALUES (?, ?, ?, ?)";


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    snackSql
                            )
            ) {

                for (Snack snack : snacks) {

                    int quantity =
                            selectedSnacks.getOrDefault(
                                    snack.getSnackId(),
                                    0
                            );


                    if (quantity <= 0) {
                        continue;
                    }


                    statement.setInt(
                            1,
                            savedBookingId
                    );

                    statement.setInt(
                            2,
                            snack.getSnackId()
                    );

                    statement.setInt(
                            3,
                            quantity
                    );

                    statement.setDouble(
                            4,
                            snack.getPrice()
                    );


                    statement.addBatch();
                }


                statement.executeBatch();
            }


            // =================================================
            // COMMIT
            // =================================================

            connection.commit();


            btnPrint.setDisable(false);

            btnEmail.setDisable(false);


            String message;

            if (editMode) {

                message =
                        "Booking #"
                        + savedBookingId
                        + " was updated successfully.";

            } else {

                message =
                        "Booking #"
                        + savedBookingId
                        + " was saved successfully.";
            }


            showAlert(
                    Alert.AlertType.INFORMATION,
                    editMode
                    ? "Booking Updated"
                    : "Booking Saved",
                    message
            );


            // =================================================
            // REFRESH SEATS
            // =================================================

            loadSeatsForSelectedShow();

restoreSelectedSeatStyles();

updateInvoiceTotal();


            lblSelectedSeats.setText(
                    "Selected seats: "
                    + String.join(
                            ", ",
                            selectedSeatNumbers
                    )
            );


            // =================================================
            // KEEP UPDATE MODE
            // =================================================

            if (editMode) {

                btnSave.setText(
                        "Update Booking"
                );
            }


        } catch (Exception e) {

            try {

                if (connection != null) {

                    connection.rollback();
                }

            } catch (Exception rollbackException) {

                rollbackException.printStackTrace();
            }


            savedBookingId = null;


            showAlert(
                    Alert.AlertType.ERROR,
                    "Booking Error",
                    "Could not save/update the booking.\n\n"
                    + e.getMessage()
            );


            e.printStackTrace();


        } finally {

            try {

                if (connection != null) {

                    connection.setAutoCommit(true);

                    connection.close();
                }

            } catch (Exception closeException) {

                closeException.printStackTrace();
            }
        }
    }
    
    // =====================================================
// RESTORE SELECTED SEAT STYLES
// =====================================================

private void restoreSelectedSeatStyles() {

    if (selectedSeatIds.isEmpty()) {
        return;
    }

    for (Integer seatId : selectedSeatIds) {

        Button seatButton =
                seatButtons.get(seatId);

        if (seatButton != null) {

            applySelectedSeatStyle(
                    seatButton
            );
        }
    }
}


    // =====================================================
    // SQL PLACEHOLDERS
    // =====================================================

    private String createPlaceholders(
            int count) {

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0; i < count; i++) {

            if (i > 0) {

                builder.append(",");
            }

            builder.append("?");
        }

        return builder.toString();
    }


    // =====================================================
    // PRINT
    // =====================================================

    @FXML
    private void handlePrint(
            ActionEvent event) {

        if (savedBookingId == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Not Saved",
                    "Please save the booking before printing."
            );

            return;
        }


        try {

            VBox invoice =
                    createPrintableInvoice();


            PrinterJob printerJob =
                    PrinterJob.createPrinterJob();


            if (printerJob == null) {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Print Error",
                        "No printer is available."
                );

                return;
            }


            Stage stage =
                    (Stage) btnPrint
                            .getScene()
                            .getWindow();


            boolean proceed =
                    printerJob.showPrintDialog(
                            stage
                    );


            if (!proceed) {

                return;
            }


            PageLayout pageLayout =
                    printerJob
                            .getPrinter()
                            .createPageLayout(
                                    Paper.A4,
                                    PageOrientation.PORTRAIT,
                                    Printer.MarginType.DEFAULT
                            );


            boolean success =
                    printerJob.printPage(
                            pageLayout,
                            invoice
                    );


            if (success) {

                printerJob.endJob();


                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Print",
                        "Invoice printed successfully."
                );


                resetBookingForm();

            } else {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Print Error",
                        "Could not print the invoice."
                );
            }


        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Print Error",
                    "Could not print the invoice."
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // SELECTED SNACKS TEXT
    // =====================================================

    private String getSelectedSnacksText() {

        StringBuilder text =
                new StringBuilder();

        boolean hasSnacks = false;


        for (Snack snack : snacks) {

            int quantity =
                    selectedSnacks.getOrDefault(
                            snack.getSnackId(),
                            0
                    );


            if (quantity > 0) {

                hasSnacks = true;


                double subtotal =
                        quantity
                        * snack.getPrice();


                text.append(
                        snack.getSnackName()
                );

                text.append(" x");

                text.append(quantity);

                text.append(" = ");

                text.append(
                        String.format(
                                Locale.US,
                                "%.2f $",
                                subtotal
                        )
                );

                text.append("\n");
            }
        }


        if (!hasSnacks) {

            return "None";
        }


        return text
                .toString()
                .trim();
    }


    // =====================================================
    // CREATE PRINTABLE INVOICE
    // =====================================================

    private VBox createPrintableInvoice() {

        VBox invoice =
                new VBox(10);


        invoice.setStyle(
                "-fx-background-color:white;" +
                "-fx-padding:30;"
        );


        Label title =
                new Label(
                        "CINEMA BOOKING INVOICE"
                );


        title.setStyle(
                "-fx-font-size:24px;" +
                "-fx-font-weight:bold;"
        );


        Label booking =
                new Label(
                        "Booking #: "
                        + savedBookingId
                );


        booking.setStyle(
                "-fx-font-size:14px;" +
                "-fx-font-weight:bold;"
        );


        Label customer =
                new Label(
                        "Customer: "
                        + lblInvoiceCustomer.getText()
                );


        Label employee =
                new Label(
                        "Employee: "
                        + lblInvoiceEmployee.getText()
                );


        Label movie =
                new Label(
                        "Movie: "
                        + lblInvoiceMovie.getText()
                );


        Label cinema =
                new Label(
                        "Cinema: "
                        + lblInvoiceCinema.getText()
                );


        Label hall =
                new Label(
                        "Hall: "
                        + lblInvoiceHall.getText()
                );


        Label show =
                new Label(
                        "Show: "
                        + lblInvoiceShow.getText()
                );


        Label date =
                new Label(
                        "Date: "
                        + lblInvoiceDate.getText()
                );


        Label seats =
                new Label(
                        "Seats: "
                        + lblInvoiceSeats.getText()
                );


        Label tickets =
                new Label(
                        "Tickets: "
                        + lblInvoiceTickets.getText()
                );


        Label snacksLabel =
                new Label(
                        "Snacks:\n"
                        + getSelectedSnacksText()
                );


        snacksLabel.setStyle(
                "-fx-font-size:14px;"
        );


        Label total =
                new Label(
                        "TOTAL: "
                        + lblInvoiceTotal.getText()
                );


        total.setStyle(
                "-fx-font-size:20px;" +
                "-fx-font-weight:bold;"
        );


        invoice.getChildren().addAll(
                title,
                booking,
                customer,
                employee,
                movie,
                cinema,
                hall,
                show,
                date,
                seats,
                tickets,
                snacksLabel,
                total
        );


        return invoice;
    }


    // =====================================================
    // EMAIL
    // =====================================================

    @FXML
    private void handleEmail(
            ActionEvent event) {

        if (savedBookingId == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Not Saved",
                    "Please save the booking before sending the email."
            );

            return;
        }


        if (selectedCustomer == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Customer",
                    "Please select a customer."
            );

            return;
        }


        String email =
                selectedCustomer.getCustomerEmail();


        if (
                email == null ||
                email.trim().isEmpty()
        ) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Email",
                    "This customer does not have an email address."
            );

            return;
        }


        try {

            String subject =
                    "Cinema Booking Invoice #"
                    + savedBookingId;


            String body =
                    "Cinema Booking Invoice\n"
                    + "========================\n\n"

                    + "Booking #: "
                    + savedBookingId
                    + "\n"

                    + "Customer: "
                    + lblInvoiceCustomer.getText()
                    + "\n"

                    + "Employee: "
                    + lblInvoiceEmployee.getText()
                    + "\n"

                    + "Movie: "
                    + lblInvoiceMovie.getText()
                    + "\n"

                    + "Cinema: "
                    + lblInvoiceCinema.getText()
                    + "\n"

                    + "Hall: "
                    + lblInvoiceHall.getText()
                    + "\n"

                    + "Show: "
                    + lblInvoiceShow.getText()
                    + "\n"

                    + "Date: "
                    + lblInvoiceDate.getText()
                    + "\n"

                    + "Seats: "
                    + lblInvoiceSeats.getText()
                    + "\n"

                    + "Tickets: "
                    + lblInvoiceTickets.getText()
                    + "\n"

                    + "Snacks:\n"
                    + getSelectedSnacksText()
                    + "\n"

                    + "Total: "
                    + lblInvoiceTotal.getText()
                    + "\n\n"

                    + "Thank you for booking with us.";


            EmailService.sendEmail(
                    email,
                    subject,
                    body
            );


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Email Sent",
                    "Invoice sent successfully to:\n"
                    + email
            );


            resetBookingForm();


        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Email Error",
                    "Could not send the email.\n\n"
                    + e.getMessage()
            );


            e.printStackTrace();
        }
    }


    // =====================================================
    // NEW CUSTOMER
    // =====================================================

    @FXML
    private void handleNewCustomer(
            ActionEvent event) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/NewCustomer.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            NewCustomerController controller =
                    loader.getController();


            controller.setBookingController(
                    this
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Add Customer"
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
                    "Could not open Add Customer window."
            );
        }
    }


    // =====================================================
    // RELOAD CUSTOMERS
    // =====================================================

    public void reloadCustomers() {

        loadCustomers();
    }


    // =====================================================
    // BACK
    // =====================================================

    @FXML
    private void handleBack(
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


            controller.setEmployeeName(
                    Session.getFullName()
            );


            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();


            stage.setTitle(
                    "Cinema Dashboard"
            );


            stage.setScene(
                    new Scene(root)
            );


            stage.show();


        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Navigation Error",
                    "Could not return to dashboard."
            );


            e.printStackTrace();
        }
    }


    // =====================================================
    // CLEAR INVOICE
    // =====================================================

    private void clearInvoice() {

        lblInvoiceCustomer.setText("-");


        lblInvoiceEmployee.setText(
                Session.getFullName() != null
                ? Session.getFullName()
                : "-"
        );


        lblInvoiceMovie.setText("-");

        lblInvoiceCinema.setText("-");

        lblInvoiceHall.setText("-");

        lblInvoiceShow.setText("-");

        lblInvoiceDate.setText("-");

        lblInvoiceSeats.setText("-");

        lblInvoiceTickets.setText("0");

        lblInvoiceTotal.setText(
                "0.00 $"
        );
    }


    // =====================================================
    // ALERT
    // =====================================================

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
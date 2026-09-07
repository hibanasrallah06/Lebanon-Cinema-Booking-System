package cinemasystem.controller;

import cinemasystem.database.DBConnection;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Optional;

public class ManageBookingController {

    // =========================================================
    // SEARCH
    // =========================================================

    @FXML
    private TextField txtSearch;


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private Button btnBack;

    @FXML
    private Button btnEditBooking;

    @FXML
    private Button btnCancelBooking;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<BookingRow> tblBookings;

    @FXML
    private TableColumn<BookingRow, Integer> colBookingId;

    @FXML
    private TableColumn<BookingRow, String> colCustomer;

    @FXML
    private TableColumn<BookingRow, String> colPhone;

    @FXML
    private TableColumn<BookingRow, String> colMovie;

    @FXML
    private TableColumn<BookingRow, String> colShowDate;

    @FXML
    private TableColumn<BookingRow, String> colStartTime;

    @FXML
    private TableColumn<BookingRow, String> colSeats;
    
    @FXML
    private TableColumn<BookingRow, String> colSnacks;

    @FXML
    private TableColumn<BookingRow, String> colTotal;

    @FXML
    private TableColumn<BookingRow, String> colStatus;

    @FXML
    private TableColumn<BookingRow, String> colBookingDate;


    // =========================================================
    // SELECTED BOOKING
    // =========================================================

    @FXML
    private Label lblSelectedBookingId;

    @FXML
    private Label lblSelectedCustomer;

    @FXML
    private Label lblSelectedMovie;

    @FXML
    private Label lblSelectedShow;

    @FXML
    private Label lblSelectedSeats;

    @FXML
    private Label lblSelectedTotal;

    @FXML
    private Label lblSelectedStatus;

    @FXML
    private Label lblBookingCount;


    // =========================================================
    // SNACK DETAILS
    // =========================================================

    @FXML
    private Label lblSelectedSnacks;


    // =========================================================
    // USER
    // =========================================================

    private String employeeName;

    private String userRole;

    private int cinemaId;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        setupTable();

        setupTableSelection();

        setupLiveSearch();

        clearSelectedBooking();

        loadBookings();
    }


    // =========================================================
    // EMPLOYEE
    // =========================================================

    public void setEmployeeName(String fullName) {

        this.employeeName = fullName;
    }


    // =========================================================
    // ROLE
    // =========================================================

    public void setUserRole(String role) {

        this.userRole = role;
    }


    // =========================================================
    // CINEMA
    // =========================================================

    public void setCinemaId(int cinemaId) {

        this.cinemaId = cinemaId;

        if (tblBookings != null) {

            loadBookings();
        }
    }


    // =========================================================
    // TABLE SETUP
    // =========================================================

    private void setupTable() {

        tblBookings.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );


        colBookingId.setCellValueFactory(
                new PropertyValueFactory<>("bookingId")
        );

        colCustomer.setCellValueFactory(
                new PropertyValueFactory<>("customer")
        );

        colPhone.setCellValueFactory(
                new PropertyValueFactory<>("phone")
        );

        colMovie.setCellValueFactory(
                new PropertyValueFactory<>("movie")
        );

        colShowDate.setCellValueFactory(
                new PropertyValueFactory<>("showDate")
        );

        colStartTime.setCellValueFactory(
                new PropertyValueFactory<>("startTime")
        );

        colSeats.setCellValueFactory(
                new PropertyValueFactory<>("seats")
        );
        
        colSnacks.setCellValueFactory(
                new PropertyValueFactory<>("snacks")
        );

        colTotal.setCellValueFactory(
                new PropertyValueFactory<>("total")
        );

        colStatus.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        colBookingDate.setCellValueFactory(
                new PropertyValueFactory<>("bookingDate")
        );
    }


    // =========================================================
    // LIVE SEARCH
    // =========================================================

    private void setupLiveSearch() {

        txtSearch.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    loadBookings();
                }
        );
    }


    // =========================================================
    // TABLE SELECTION
    // =========================================================

    private void setupTableSelection() {

        tblBookings.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {

                            if (newValue != null) {

                                showSelectedBooking(newValue);

                            } else {

                                clearSelectedBooking();
                            }
                        }
                );
    }


    // =========================================================
// LOAD UPCOMING BOOKINGS ONLY
// =========================================================

private void loadBookings() {

    ObservableList<BookingRow> bookingList =
            FXCollections.observableArrayList();

    String search =
            txtSearch != null
                    ? txtSearch.getText().trim()
                    : "";

    StringBuilder sql =
            new StringBuilder();

    sql.append(
            "SELECT "
            + "b.booking_id, "
            + "c.customer_name, "
            + "c.customer_phone, "
            + "m.title AS movie_title, "
            + "ms.show_date, "
            + "ms.start_time, "
            + "h.hall_name, "
            + "b.total_amount, "
            + "b.status, "
            + "b.booking_date, "

            // =================================================
            // SEATS
            // =================================================

            + "GROUP_CONCAT("
            + "DISTINCT CONCAT(s.seat_row, s.seat_number) "
            + "ORDER BY s.seat_row, s.seat_number "
            + "SEPARATOR ', '"
            + ") AS seats, "

            // =================================================
            // SNACKS
            // =================================================

            + "("
            + "SELECT GROUP_CONCAT("
            + "CONCAT(sn2.snack_name, ' × ', bs2.quantity) "
            + "ORDER BY sn2.snack_name "
            + "SEPARATOR ', '"
            + ") "
            + "FROM booking_snack bs2 "
            + "JOIN snack sn2 "
            + "ON bs2.snack_id = sn2.snack_id "
            + "WHERE bs2.booking_id = b.booking_id"
            + ") AS snacks "

            // =================================================
            // FROM
            // =================================================

            + "FROM booking b "

            + "JOIN customer c "
            + "ON b.customer_id = c.customer_id "

            + "JOIN movie_show ms "
            + "ON b.show_id = ms.show_id "

            + "JOIN movie m "
            + "ON ms.movie_id = m.movie_id "

            + "JOIN hall h "
            + "ON ms.hall_id = h.hall_id "

            + "LEFT JOIN booking_seat bs "
            + "ON b.booking_id = bs.booking_id "

            + "LEFT JOIN seat s "
            + "ON bs.seat_id = s.seat_id "

            // =================================================
            // ONLY UPCOMING SHOWS
            // =================================================

            + "WHERE TIMESTAMP(ms.show_date, ms.start_time) >= NOW() "
    );

    // =========================================================
    // CINEMA RESTRICTION
    // =========================================================

    boolean restrictCinema = !isAdmin();

    if (restrictCinema) {

        sql.append(
                "AND h.cinema_id = ? "
        );
    }

    // =========================================================
    // SEARCH
    // =========================================================

    if (!search.isEmpty()) {

        sql.append(
                "AND ("
                + "CAST(b.booking_id AS CHAR) LIKE ? "
                + "OR LOWER(c.customer_name) LIKE ? "
                + "OR c.customer_phone LIKE ? "
                + "OR LOWER(c.customer_email) LIKE ? "
                + "OR LOWER(m.title) LIKE ? "
                + "OR LOWER(h.hall_name) LIKE ? "
                + "OR CAST(ms.show_date AS CHAR) LIKE ? "
                + ") "
        );
    }

    // =========================================================
    // GROUP BY
    // =========================================================

    sql.append(
            "GROUP BY "
            + "b.booking_id, "
            + "c.customer_name, "
            + "c.customer_phone, "
            + "m.title, "
            + "ms.show_date, "
            + "ms.start_time, "
            + "h.hall_name, "
            + "b.total_amount, "
            + "b.status, "
            + "b.booking_date "
    );

    // =========================================================
    // ORDER BY UPCOMING SHOWS
    // =========================================================

    sql.append(
            "ORDER BY "
            + "ms.show_date ASC, "
            + "ms.start_time ASC"
    );

    // =========================================================
    // DATABASE
    // =========================================================

    try (
            Connection connection =
                    DBConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(
                            sql.toString()
                    )
    ) {

        int parameterIndex = 1;

        // =====================================================
        // CINEMA
        // =====================================================

        if (restrictCinema) {

            statement.setInt(
                    parameterIndex++,
                    Session.getCinemaId()
            );
        }

        // =====================================================
        // SEARCH PARAMETERS
        // =====================================================

        if (!search.isEmpty()) {

            String value =
                    "%" + search.toLowerCase() + "%";

            // Booking ID
            statement.setString(
                    parameterIndex++,
                    value
            );

            // Customer name
            statement.setString(
                    parameterIndex++,
                    value
            );

            // Phone
            statement.setString(
                    parameterIndex++,
                    "%" + search + "%"
            );

            // Email
            statement.setString(
                    parameterIndex++,
                    value
            );

            // Movie
            statement.setString(
                    parameterIndex++,
                    value
            );

            // Hall
            statement.setString(
                    parameterIndex++,
                    value
            );

            // Date
            statement.setString(
                    parameterIndex++,
                    value
            );
        }

        // =====================================================
        // EXECUTE QUERY
        // =====================================================

        try (
                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                // =================================================
                // BOOKING ID
                // =================================================

                int bookingId =
                        result.getInt(
                                "booking_id"
                        );

                // =================================================
                // CUSTOMER
                // =================================================

                String customerName =
                        result.getString(
                                "customer_name"
                        );

                String phone =
                        result.getString(
                                "customer_phone"
                        );

                // =================================================
                // MOVIE
                // =================================================

                String movieTitle =
                        result.getString(
                                "movie_title"
                        );

                // =================================================
                // SHOW DATE
                // =================================================

                String showDate =
                        result.getDate(
                                "show_date"
                        ) != null
                                ? result.getDate(
                                        "show_date"
                                ).toString()
                                : "";

                // =================================================
                // START TIME
                // =================================================

                String startTime =
                        result.getTime(
                                "start_time"
                        ) != null
                                ? result.getTime(
                                        "start_time"
                                ).toString()
                                : "";

                // =================================================
                // SEATS
                // =================================================

                String seats =
                        result.getString(
                                "seats"
                        );

                seats =
                        formatSeats(seats);

                // =================================================
                // SNACKS
                // =================================================

                String snacks =
                        result.getString(
                                "snacks"
                        );

                if (snacks == null
                        || snacks.trim().isEmpty()) {

                    snacks = "-";
                }

                // =================================================
                // TOTAL
                // =================================================

                String total =
                        result.getBigDecimal(
                                "total_amount"
                        ) != null
                                ? result.getBigDecimal(
                                        "total_amount"
                                ).toPlainString()
                                : "0.00";

                // =================================================
                // STATUS
                // =================================================

                String bookingStatus =
                        result.getString(
                                "status"
                        );

                // =================================================
                // BOOKING DATE
                // =================================================

                String bookingDate =
                        formatBookingDate(
                                result.getTimestamp(
                                        "booking_date"
                                )
                        );

                // =================================================
                // CREATE ROW
                // =================================================

                BookingRow row =
                        new BookingRow(
                                bookingId,
                                customerName,
                                phone,
                                movieTitle,
                                showDate,
                                startTime,
                                seats,
                                snacks,
                                total,
                                bookingStatus,
                                bookingDate
                        );

                bookingList.add(row);
            }
        }

        // =====================================================
        // SET TABLE
        // =====================================================

        tblBookings.setItems(
                bookingList
        );

        // =====================================================
        // BOOKING COUNT
        // =====================================================

        lblBookingCount.setText(
                bookingList.size()
                +
                (
                        bookingList.size() == 1
                                ? " booking"
                                : " bookings"
                )
        );

    } catch (SQLException e) {

        e.printStackTrace();

        showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Could not load upcoming bookings.\n\n"
                        + e.getMessage()
        );
    }
}
    // =========================================================
    // FORMAT SEATS
    // =========================================================

    private String formatSeats(String seats) {

        if (seats == null ||
                seats.trim().isEmpty()) {

            return "-";
        }


        /*
         * The database already returns:
         *
         * A1, A2, A3
         *
         * because the query uses:
         *
         * CONCAT(seat_row, seat_number)
         *
         * So we simply return it.
         */

        return seats;
    }
    
    private boolean isShowWithin24Hours(int bookingId) {

    String sql =
            "SELECT ms.show_date, ms.start_time " +
            "FROM booking b " +
            "JOIN movie_show ms ON b.show_id = ms.show_id " +
            "WHERE b.booking_id = ?";

    try (
            Connection connection = DBConnection.getConnection();
            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        statement.setInt(1, bookingId);

        try (ResultSet result = statement.executeQuery()) {

            if (result.next()) {

                java.sql.Date showDate =
                        result.getDate("show_date");

                java.sql.Time startTime =
                        result.getTime("start_time");

                if (showDate == null || startTime == null) {
                    return false;
                }

                LocalDateTime showDateTime =
                        LocalDateTime.of(
                                showDate.toLocalDate(),
                                startTime.toLocalTime()
                        );

                LocalDateTime cancellationDeadline =
                        showDateTime.minusHours(24);

                return LocalDateTime.now()
                        .isAfter(cancellationDeadline);
            }
        }

    } catch (SQLException e) {

        e.printStackTrace();
    }

    return false;
}


    // =========================================================
    // SHOW SELECTED BOOKING
    // =========================================================

    private void showSelectedBooking(
            BookingRow booking) {

        lblSelectedBookingId.setText(
                String.valueOf(
                        booking.getBookingId()
                )
        );


        lblSelectedCustomer.setText(
                safeText(
                        booking.getCustomer()
                )
        );


        lblSelectedMovie.setText(
                safeText(
                        booking.getMovie()
                )
        );


        lblSelectedShow.setText(
                booking.getShowDate()
                + "  "
                + booking.getStartTime()
        );


        lblSelectedSeats.setText(
                safeText(
                        booking.getSeats()
                )
        );


        lblSelectedTotal.setText(
                booking.getTotal()
                + " $"
        );


        lblSelectedStatus.setText(
                safeText(
                        booking.getStatus()
                )
        );


        // =====================================================
        // LOAD SNACKS
        // =====================================================

        loadSelectedSnacks(
                booking.getBookingId()
        );


        boolean cancelled =
        booking.getStatus() != null
        &&
        booking.getStatus()
                .equalsIgnoreCase("Cancelled");

boolean within24Hours =
        isShowWithin24Hours(
                booking.getBookingId()
        );

boolean cannotModify =
        cancelled || within24Hours;

btnCancelBooking.setDisable(
        cannotModify
);

btnEditBooking.setDisable(
        cannotModify
);
    }


    // =========================================================
    // LOAD SNACK DETAILS
    // =========================================================

    private void loadSelectedSnacks(
            int bookingId) {

        if (lblSelectedSnacks == null) {
            return;
        }


        String sql =
                "SELECT "
                + "s.snack_name, "
                + "bs.quantity, "
                + "bs.unit_price "
                + "FROM booking_snack bs "
                + "JOIN snack s "
                + "ON bs.snack_id = s.snack_id "
                + "WHERE bs.booking_id = ? "
                + "ORDER BY s.snack_name";


        StringBuilder snacks =
                new StringBuilder();


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setInt(
                    1,
                    bookingId
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    String snackName =
                            result.getString(
                                    "snack_name"
                            );


                    int quantity =
                            result.getInt(
                                    "quantity"
                            );


                    String unitPrice =
                            result.getBigDecimal(
                                    "unit_price"
                            ) != null
                            ? result.getBigDecimal(
                                    "unit_price"
                            ).toPlainString()
                            : "0.00";


                    if (snacks.length() > 0) {

                        snacks.append("\n");
                    }


                    snacks.append(
                            "🍿 "
                    );

                    snacks.append(
                            snackName
                    );

                    snacks.append(
                            " × "
                    );

                    snacks.append(
                            quantity
                    );

                    snacks.append(
                            "  ("
                    );

                    snacks.append(
                            unitPrice
                    );

                    snacks.append(
                            " $)"
                    );
                }
            }


            if (snacks.length() == 0) {

                lblSelectedSnacks.setText(
                        "No snacks"
                );

            } else {

                lblSelectedSnacks.setText(
                        snacks.toString()
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            lblSelectedSnacks.setText(
                    "Could not load snacks"
            );
        }
    }


    // =========================================================
    // CLEAR SELECTED BOOKING
    // =========================================================

    private void clearSelectedBooking() {

        if (lblSelectedBookingId != null)
            lblSelectedBookingId.setText("-");

        if (lblSelectedCustomer != null)
            lblSelectedCustomer.setText("-");

        if (lblSelectedMovie != null)
            lblSelectedMovie.setText("-");

        if (lblSelectedShow != null)
            lblSelectedShow.setText("-");

        if (lblSelectedSeats != null)
            lblSelectedSeats.setText("-");

        if (lblSelectedTotal != null)
            lblSelectedTotal.setText("-");

        if (lblSelectedStatus != null)
            lblSelectedStatus.setText("-");

        if (lblSelectedSnacks != null)
            lblSelectedSnacks.setText("-");


        if (btnCancelBooking != null) {

            btnCancelBooking.setDisable(true);
        }


        if (btnEditBooking != null) {

            btnEditBooking.setDisable(true);
        }
    }


    // =========================================================
    // EDIT BOOKING
    // =========================================================

    @FXML
private void handleEditBooking(ActionEvent event) {

    // Get the selected booking from the table
    BookingRow selectedBooking =
            tblBookings.getSelectionModel()
                    .getSelectedItem();

    // No booking selected
    if (selectedBooking == null) {

        showAlert(
                Alert.AlertType.WARNING,
                "No Booking Selected",
                "Please select a booking first."
        );

        return;
    }

    // Don't allow editing cancelled bookings
    if (selectedBooking.getStatus() != null
            &&
            selectedBooking.getStatus()
                    .equalsIgnoreCase("Cancelled")) {

        showAlert(
                Alert.AlertType.WARNING,
                "Cannot Edit Booking",
                "Cancelled bookings cannot be edited."
        );

        return;
    }
    
    if (isShowWithin24Hours(
        selectedBooking.getBookingId())) {

    showAlert(
            Alert.AlertType.WARNING,
            "Cannot Edit Booking",
            "This booking cannot be edited because more than 24 hours have passed since it was created."
    );

    return;
}

    try {

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/cinemasystem/view/Booking.fxml"
                        )
                );

        Parent root = loader.load();

        BookingController controller =
                loader.getController();

        // Send the selected booking ID to BookingController
        controller.setEditBookingId(
                selectedBooking.getBookingId()
        );

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setTitle("Edit Booking");

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
                "Edit Error",
                "Could not open booking for editing.\n\n"
                        + e.getMessage()
        );
    }
}


    // =========================================================
    // REFRESH
    // =========================================================

    /*
     * Refresh is kept because it is useful,
     * but Clear is completely removed.
     */

    @FXML
    private void handleRefresh(
            ActionEvent event) {

        loadBookings();

        tblBookings.getSelectionModel()
                .clearSelection();
    }


    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    @FXML
    private void handleCancelBooking(
            ActionEvent event) {

        BookingRow selectedBooking =
                tblBookings
                        .getSelectionModel()
                        .getSelectedItem();


        if (selectedBooking == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Booking Selected",
                    "Please select a booking first."
            );

            return;
        }


        if (selectedBooking.getStatus() != null
                &&
                selectedBooking.getStatus()
                        .equalsIgnoreCase(
                                "Cancelled"
                        )) {

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Already Cancelled",
                    "This booking is already cancelled."
            );

            return;
        }
        
        if (isShowWithin24Hours(
        selectedBooking.getBookingId())) {

    showAlert(
            Alert.AlertType.WARNING,
            "Cannot Cancel Booking",
            "This booking cannot be cancelled because less than 24 hours remain before the show."
    );

    return;
}


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Cancel Booking"
        );


        confirmation.setHeaderText(
                "Cancel Booking #"
                + selectedBooking.getBookingId()
        );


        confirmation.setContentText(
                "Are you sure you want to cancel this booking?\n\n"
                + "Customer: "
                + selectedBooking.getCustomer()
                + "\nMovie: "
                + selectedBooking.getMovie()
                + "\nSeats: "
                + selectedBooking.getSeats()
                + "\n\n"
                + "The booking will remain in the database "
                + "with status 'Cancelled'."
        );


        Optional<javafx.scene.control.ButtonType>
                result =
                confirmation.showAndWait();


        if (!result.isPresent()
                ||
                result.get()
                        != javafx.scene.control.ButtonType.OK) {

            return;
        }


        cancelBooking(
                selectedBooking.getBookingId()
        );
    }


    // =========================================================
    // DATABASE CANCELLATION
    // =========================================================

    private void cancelBooking(
            int bookingId) {

        String sql =
                "UPDATE booking "
                + "SET status = ? "
                + "WHERE booking_id = ? "
                + "AND status <> ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setString(
                    1,
                    "Cancelled"
            );


            statement.setInt(
                    2,
                    bookingId
            );


            statement.setString(
                    3,
                    "Cancelled"
            );


            int updated =
                    statement.executeUpdate();


            if (updated > 0) {

                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Booking Cancelled",
                        "Booking #"
                        + bookingId
                        + " has been cancelled successfully."
                );


                loadBookings();

                clearSelectedBooking();


            } else {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Cancellation Failed",
                        "The booking could not be cancelled. "
                        + "It may already be cancelled."
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not cancel the booking.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // BACK
    // =========================================================

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
                    "Dashboard"
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
    // ADMIN CHECK
    // =========================================================

    private boolean isAdmin() {

        String role =
                Session.getRole();


        return role != null
                &&
                role.trim()
                        .equalsIgnoreCase(
                                "admin"
                        );
    }


    // =========================================================
    // DATE FORMAT
    // =========================================================

    private String formatBookingDate(
            java.sql.Timestamp timestamp) {

        if (timestamp == null) {

            return "";
        }


        LocalDateTime dateTime =
                timestamp.toLocalDateTime();


        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm"
                );


        return dateTime.format(
                formatter
        );
    }


    // =========================================================
    // SAFE TEXT
    // =========================================================

    private String safeText(
            String value) {

        if (value == null
                ||
                value.trim().isEmpty()) {

            return "-";
        }


        return value;
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


    // =========================================================
    // BOOKING ROW
    // =========================================================

    public static class BookingRow {

        private final int bookingId;

        private final String customer;

        private final String phone;

        private final String movie;

        private final String showDate;

        private final String startTime;

        private final String seats;
        
        private final String snacks;

        private final String total;

        private final String status;

        private final String bookingDate;


        public BookingRow(
                int bookingId,
                String customer,
                String phone,
                String movie,
                String showDate,
                String startTime,
                String seats,
                String snacks,
                String total,
                String status,
                String bookingDate) {

            this.bookingId =
                    bookingId;

            this.customer =
                    customer;

            this.phone =
                    phone;

            this.movie =
                    movie;

            this.showDate =
                    showDate;

            this.startTime =
                    startTime;

            this.seats =
                    seats;
            
            this.snacks =
                     snacks;

            this.total =
                    total;

            this.status =
                    status;

            this.bookingDate =
                    bookingDate;
        }


        public int getBookingId() {
            return bookingId;
        }


        public String getCustomer() {
            return customer;
        }


        public String getPhone() {
            return phone;
        }


        public String getMovie() {
            return movie;
        }


        public String getShowDate() {
            return showDate;
        }


        public String getStartTime() {
            return startTime;
        }


        public String getSeats() {
            return seats;
        }
        
        public String getSnacks() {
            return snacks;
        }


        public String getTotal() {
            return total;
        }


        public String getStatus() {
            return status;
        }


        public String getBookingDate() {
            return bookingDate;
        }
    }
}
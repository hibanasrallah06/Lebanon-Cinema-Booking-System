package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Movie;
import cinemasystem.model.Show;
import cinemasystem.util.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public class EditShowController {

    // =========================================================
    // SHOW CONTROLLER
    // =========================================================

    private ShowController showController;


    public void setShowController(
            ShowController showController) {

        this.showController = showController;
    }


    // =========================================================
    // MOVIE
    // =========================================================

    @FXML
    private ComboBox<Movie> cmbMovie;


    // =========================================================
    // HALL
    // =========================================================

    @FXML
    private ComboBox<HallItem> cmbHall;


    // =========================================================
    // DATE
    // =========================================================

    @FXML
    private DatePicker dateShow;


    // =========================================================
    // TIME
    // =========================================================

    @FXML
    private ComboBox<String> cmbStartTime;


    @FXML
    private ComboBox<String> cmbEndTime;


    // =========================================================
    // PRICE
    // =========================================================

    @FXML
    private TextField txtTicketPrice;


    // =========================================================
    // SHOW ID
    // =========================================================

    private int showId;


    // =========================================================
    // LISTS
    // =========================================================

    private ObservableList<Movie> movieList =
            FXCollections.observableArrayList();


    private ObservableList<HallItem> hallList =
            FXCollections.observableArrayList();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadMovies();

        loadHalls();


        cmbMovie.setItems(movieList);

        cmbHall.setItems(hallList);


        // -----------------------------------------------------
        // START TIMES
        // -----------------------------------------------------

        cmbStartTime.getItems().addAll(
                "10:00",
                "10:30",
                "11:00",
                "11:30",
                "12:00",
                "12:30",
                "13:00",
                "13:30",
                "14:00",
                "14:30",
                "15:00",
                "15:30",
                "16:00",
                "16:30",
                "17:00",
                "17:30",
                "18:00",
                "18:30",
                "19:00",
                "19:30",
                "20:00",
                "20:30",
                "21:00",
                "21:30",
                "22:00"
        );


        // -----------------------------------------------------
        // END TIME IS AUTOMATIC
        // -----------------------------------------------------

        cmbEndTime.setDisable(true);


        // -----------------------------------------------------
        // AUTOMATIC END TIME
        // -----------------------------------------------------

        cmbMovie.valueProperty().addListener(
                (observable, oldValue, newValue) -> {

                    updateEndTime();
                }
        );


        cmbStartTime.valueProperty().addListener(
                (observable, oldValue, newValue) -> {

                    updateEndTime();
                }
        );
    }


    // =========================================================
    // RECEIVE SELECTED SHOW
    // =========================================================

    public void setShow(
            Show show,
            int movieId,
            int hallId
    ) {

        showId =
                show.getShowId();


        // -----------------------------------------------------
        // DATE
        // -----------------------------------------------------

        dateShow.setValue(
                LocalDate.parse(
                        show.getShowDate()
                )
        );


        // -----------------------------------------------------
        // MOVIE
        // -----------------------------------------------------

        for (Movie movie : movieList) {

            if (movie.getMovieId() == movieId) {

                cmbMovie.setValue(movie);

                break;
            }
        }


        // -----------------------------------------------------
        // HALL
        // -----------------------------------------------------

        for (HallItem hall : hallList) {

            if (hall.getHallId() == hallId) {

                cmbHall.setValue(hall);

                break;
            }
        }


        // -----------------------------------------------------
        // START TIME
        // -----------------------------------------------------

        String startTime =
                show.getStartTime();


        if (startTime != null &&
            startTime.length() >= 5) {

            startTime =
                    startTime.substring(0, 5);
        }


        cmbStartTime.setValue(
                startTime
        );


        // -----------------------------------------------------
        // END TIME
        // -----------------------------------------------------
        //
        // IMPORTANT:
        // We DO NOT use the old end_time.
        //
        // The system recalculates it automatically
        // according to movie duration + 30 minutes.
        //
        // -----------------------------------------------------

        updateEndTime();


        // -----------------------------------------------------
        // PRICE
        // -----------------------------------------------------

        txtTicketPrice.setText(
                String.valueOf(
                        show.getTicketPrice()
                )
        );
    }


    // =========================================================
    // AUTOMATIC END TIME
    // =========================================================
    //
    // End Time =
    // Start Time + Movie Duration + 30 minutes
    //
    // =========================================================

    private void updateEndTime() {

        Movie selectedMovie =
                cmbMovie.getValue();

        String startTime =
                cmbStartTime.getValue();


        if (selectedMovie == null ||
            startTime == null) {

            cmbEndTime.setValue(null);

            return;
        }


        String sql =
                "SELECT duration " +
                "FROM movie " +
                "WHERE movie_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    selectedMovie.getMovieId()
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    int duration =
                            result.getInt(
                                    "duration"
                            );


                    if (result.wasNull() ||
                        duration <= 0) {

                        cmbEndTime.setValue(null);

                        showAlert(
                                Alert.AlertType.WARNING,
                                "Movie Duration",
                                "This movie does not have a valid duration."
                        );

                        return;
                    }


                    // -------------------------------------------------
                    // MOVIE + 30 MINUTES CLEANING BUFFER
                    // -------------------------------------------------

                    int totalMinutes =
                            duration + 30;


                    LocalTime start =
                            LocalTime.parse(
                                    startTime
                            );


                    LocalDateTime startDateTime =
                            LocalDateTime.of(
                                    LocalDate.now(),
                                    start
                            );


                    LocalDateTime endDateTime =
                            startDateTime.plusMinutes(
                                    totalMinutes
                            );


                    LocalTime end =
                            endDateTime.toLocalTime();


                    String endTime =
                            String.format(
                                    "%02d:%02d",
                                    end.getHour(),
                                    end.getMinute()
                            );


                    cmbEndTime.setValue(
                            endTime
                    );
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not calculate show end time.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // LOAD MOVIES
    // =========================================================

    private void loadMovies() {

        String sql =
                "SELECT movie_id, title " +
                "FROM movie " +
                "ORDER BY title";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                Movie movie =
                        new Movie(
                                result.getInt("movie_id"),
                                result.getString("title"),
                                null,
                                null,
                                null,
                                0,
                                null
                        );


                movieList.add(movie);
            }

        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load movies.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // LOAD HALLS
    // =========================================================

    private void loadHalls() {

        hallList.clear();


        String role =
                Session.getRole();


        if (role == null) {

            role = "";
        }


        role =
                role.trim().toLowerCase();


        // =====================================================
        // ADMIN → ALL HALLS
        // =====================================================

        if (role.equals("admin")) {

            String sql =
                    "SELECT hall_id, hall_name " +
                    "FROM hall " +
                    "ORDER BY hall_name";


            try (
                    Connection connection =
                            DBConnection.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    hallList.add(
                            new HallItem(
                                    result.getInt("hall_id"),
                                    result.getString("hall_name")
                            )
                    );
                }

            } catch (SQLException e) {

                e.printStackTrace();

                showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Could not load halls.\n\n"
                        + e.getMessage()
                );
            }
        }


        // =====================================================
        // MANAGER / EMPLOYEE
        // =====================================================

        else if (role.equals("manager")
                || role.equals("employee")) {

            int cinemaId =
                    Session.getCinemaId();


            String sql =
                    "SELECT h.hall_id, h.hall_name " +
                    "FROM hall h " +
                    "WHERE h.cinema_id = ? " +
                    "ORDER BY h.hall_name";


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

                    while (result.next()) {

                        hallList.add(
                                new HallItem(
                                        result.getInt("hall_id"),
                                        result.getString("hall_name")
                                )
                        );
                    }
                }

            } catch (SQLException e) {

                e.printStackTrace();

                showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Could not load your cinema halls.\n\n"
                        + e.getMessage()
                );
            }
        }


        // =====================================================
        // UNKNOWN ROLE
        // =====================================================

        else {

            hallList.clear();
        }
    }


    // =========================================================
    // UPDATE SHOW
    // =========================================================

    @FXML
    private void handleUpdate() {

        // -----------------------------------------------------
        // CHECK EMPTY FIELDS
        // -----------------------------------------------------

        if (cmbMovie.getValue() == null ||
            cmbHall.getValue() == null ||
            dateShow.getValue() == null ||
            cmbStartTime.getValue() == null ||
            cmbEndTime.getValue() == null ||
            txtTicketPrice.getText()
                    .trim()
                    .isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Show",
                    "Please fill in all fields."
            );

            return;
        }


        // -----------------------------------------------------
        // GET VALUES
        // -----------------------------------------------------

        String startTime =
                cmbStartTime.getValue();

        String endTime =
                cmbEndTime.getValue();

        LocalDate showDate =
                dateShow.getValue();


        // -----------------------------------------------------
        // CHECK TIME
        // -----------------------------------------------------

        LocalTime start;
        LocalTime end;


        try {

            start =
                    LocalTime.parse(startTime);

            end =
                    LocalTime.parse(endTime);

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Time",
                    "Please select valid start and end times."
            );

            return;
        }


        // -----------------------------------------------------
        // GET MOVIE DURATION
        // -----------------------------------------------------

        int movieDuration;


        try {

            movieDuration =
                    getMovieDuration(
                            cmbMovie
                                    .getValue()
                                    .getMovieId()
                    );

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not read movie duration.\n\n"
                    + e.getMessage()
            );

            return;
        }


        if (movieDuration <= 0) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Movie Duration",
                    "This movie does not have a valid duration."
            );

            return;
        }


        // -----------------------------------------------------
        // VERIFY AUTOMATIC END TIME
        // -----------------------------------------------------

        LocalDateTime expectedEnd =
                LocalDateTime.of(
                        showDate,
                        start
                ).plusMinutes(
                        movieDuration + 30
                );


        LocalDateTime selectedEnd =
                getDateTime(
                        showDate,
                        endTime,
                        true
                );


        if (!expectedEnd.equals(selectedEnd)) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Show Duration",
                    "The end time does not match the movie duration.\n\n"
                    + "The system automatically requires "
                    + movieDuration
                    + " minutes for the movie plus 30 minutes for cleaning."
            );

            updateEndTime();

            return;
        }


        // -----------------------------------------------------
        // SHOW MUST NOT START IN THE PAST
        // -----------------------------------------------------

        LocalDateTime showStart =
                LocalDateTime.of(
                        showDate,
                        start
                );


        if (!showStart.isAfter(LocalDateTime.now())) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Show Date",
                    "You cannot set a show in the past.\n\n"
                    + "Please select a future date and time."
            );

            return;
        }


        // -----------------------------------------------------
        // CHECK PRICE
        // -----------------------------------------------------

        double price;


        try {

            price =
                    Double.parseDouble(
                            txtTicketPrice
                                    .getText()
                                    .trim()
                    );


            if (price < 0) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Invalid Price",
                        "Ticket price cannot be negative."
                );

                return;
            }

        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Price",
                    "Please enter a valid ticket price."
            );

            return;
        }


        // -----------------------------------------------------
        // GET HALL ID
        // -----------------------------------------------------

        int hallId =
                cmbHall
                        .getValue()
                        .getHallId();


        int movieId =
                cmbMovie
                        .getValue()
                        .getMovieId();


        // -----------------------------------------------------
        // UPDATE SQL
        // -----------------------------------------------------

        String sql =
                "UPDATE movie_show " +
                "SET movie_id = ?, " +
                "hall_id = ?, " +
                "show_date = ?, " +
                "start_time = ?, " +
                "end_time = ?, " +
                "ticket_price = ? " +
                "WHERE show_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection()
        ) {

            // -------------------------------------------------
            // CHECK CONFLICT
            // -------------------------------------------------

            if (hasTimeConflict(
                    connection,
                    hallId,
                    showDate,
                    startTime,
                    endTime,
                    showId
            )) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Show Conflict",
                        "This hall already has a show "
                        + "during the selected time.\n\n"
                        + "Please choose another time "
                        + "or another hall."
                );

                return;
            }


            // -------------------------------------------------
            // UPDATE
            // -------------------------------------------------

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {

                statement.setInt(
                        1,
                        movieId
                );


                statement.setInt(
                        2,
                        hallId
                );


                statement.setDate(
                        3,
                        java.sql.Date.valueOf(
                                showDate
                        )
                );


                statement.setString(
                        4,
                        startTime
                );


                statement.setString(
                        5,
                        endTime
                );


                statement.setDouble(
                        6,
                        price
                );


                statement.setInt(
                        7,
                        showId
                );


                statement.executeUpdate();
            }


            // -------------------------------------------------
            // REFRESH TABLE
            // -------------------------------------------------

            if (showController != null) {

                showController.loadShows();
            }


            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Show updated successfully."
            );


            closeWindow();


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not update the show.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // GET MOVIE DURATION
    // =========================================================

    private int getMovieDuration(
            int movieId) throws SQLException {

        String sql =
                "SELECT duration " +
                "FROM movie " +
                "WHERE movie_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    movieId
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    int duration =
                            result.getInt("duration");


                    if (result.wasNull()) {

                        return 0;
                    }


                    return duration;
                }
            }
        }


        return 0;
    }


    // =========================================================
    // CHECK TIME CONFLICT
    // =========================================================

    private boolean hasTimeConflict(
            Connection connection,
            int hallId,
            LocalDate showDate,
            String newStartTime,
            String newEndTime,
            Integer excludeShowId
    ) throws SQLException {


        LocalDateTime newStart =
                getDateTime(
                        showDate,
                        newStartTime,
                        false
                );


        LocalDateTime newEnd =
                getDateTime(
                        showDate,
                        newEndTime,
                        true
                );


        String sql =
                "SELECT show_id, show_date, "
                + "start_time, end_time "
                + "FROM movie_show "
                + "WHERE hall_id = ? "
                + "AND show_date BETWEEN ? AND ?";


        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    hallId
            );


            statement.setDate(
                    2,
                    java.sql.Date.valueOf(
                            showDate.minusDays(1)
                    )
            );


            statement.setDate(
                    3,
                    java.sql.Date.valueOf(
                            showDate.plusDays(1)
                    )
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    int existingShowId =
                            result.getInt(
                                    "show_id"
                            );


                    if (excludeShowId != null &&
                        existingShowId == excludeShowId) {

                        continue;
                    }


                    LocalDate existingDate =
                            result
                                    .getDate(
                                            "show_date"
                                    )
                                    .toLocalDate();


                    String existingStart =
                            result.getString(
                                    "start_time"
                            );


                    String existingEnd =
                            result.getString(
                                    "end_time"
                            );


                    LocalDateTime existingStartDateTime =
                            getDateTime(
                                    existingDate,
                                    existingStart,
                                    false
                            );


                    LocalDateTime existingEndDateTime =
                            getDateTime(
                                    existingDate,
                                    existingEnd,
                                    true
                            );


                    boolean overlap =
                            newStart.isBefore(
                                    existingEndDateTime
                            )
                            &&
                            newEnd.isAfter(
                                    existingStartDateTime
                            );


                    if (overlap) {

                        return true;
                    }
                }
            }
        }


        return false;
    }


    // =========================================================
    // CREATE DATE + TIME
    // =========================================================

    private LocalDateTime getDateTime(
            LocalDate date,
            String timeString,
            boolean isEndTime
    ) {

        LocalTime time =
                LocalTime.parse(
                        timeString.substring(0, 5)
                );


        if (isEndTime &&
            time.equals(LocalTime.MIDNIGHT)) {

            return date
                    .plusDays(1)
                    .atStartOfDay();
        }


        return date.atTime(time);
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
                (Stage) cmbMovie
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


    // =========================================================
    // HALL ITEM
    // =========================================================

    public static class HallItem {

        private int hallId;

        private String hallName;


        public HallItem(
                int hallId,
                String hallName
        ) {

            this.hallId = hallId;

            this.hallName = hallName;
        }


        public int getHallId() {

            return hallId;
        }


        public String getHallName() {

            return hallName;
        }


        @Override
        public String toString() {

            return hallName;
        }
    }
}
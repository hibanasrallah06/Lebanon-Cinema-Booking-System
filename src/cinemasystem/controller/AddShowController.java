package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Movie;
import cinemasystem.util.Session;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public class AddShowController {

    // =========================================================
    // MOVIE
    // =========================================================

    @FXML
    private ComboBox<Movie> cmbMovie;

    private ObservableList<Movie> allMovies =
            FXCollections.observableArrayList();


    // =========================================================
    // HALL
    // =========================================================

    @FXML
    private ComboBox<HallItem> cmbHall;

    private ObservableList<HallItem> hallList =
            FXCollections.observableArrayList();


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
    // SHOW CONTROLLER
    // =========================================================

    private ShowController showController;


    public void setShowController(
            ShowController showController) {

        this.showController = showController;
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

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
        // LOAD DATA
        // -----------------------------------------------------

        loadMovies();
        loadHalls();


        cmbMovie.setItems(allMovies);
        cmbHall.setItems(hallList);


        // -----------------------------------------------------
        // MOVIE CONVERTER
        // -----------------------------------------------------

        cmbMovie.setConverter(
                new StringConverter<Movie>() {

                    @Override
                    public String toString(Movie movie) {

                        return movie == null
                                ? ""
                                : movie.getTitle();
                    }


                    @Override
                    public Movie fromString(
                            String string) {

                        if (string == null ||
                            string.trim().isEmpty()) {

                            return null;
                        }


                        for (Movie movie : allMovies) {

                            if (movie.getTitle()
                                    .equalsIgnoreCase(
                                            string.trim()
                                    )) {

                                return movie;
                            }
                        }

                        return null;
                    }
                }
        );


        // -----------------------------------------------------
        // MOVIE SEARCH
        // -----------------------------------------------------

        setupMovieSearch();


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
    // AUTOMATIC END TIME
    // =========================================================
    //
    // End Time =
    // Start Time + Movie Duration + 30 minutes
    //
    // Example:
    // Movie duration = 180 minutes
    // Start = 15:00
    // End = 18:30
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
                            result.getInt("duration");


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
                    // MOVIE DURATION + 30 MINUTES CLEANING BUFFER
                    // -------------------------------------------------

                    int totalMinutes =
                            duration + 30;


                    LocalTime start =
                            LocalTime.parse(startTime);


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


                    // -------------------------------------------------
                    // AUTOMATIC END TIME
                    // -------------------------------------------------

                    cmbEndTime.setValue(endTime);
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
    // MOVIE SEARCH
    // =========================================================

    private void setupMovieSearch() {

        TextField editor =
                cmbMovie.getEditor();


        editor.focusedProperty().addListener(
                (observable, oldValue, focused) -> {

                    if (focused) {

                        Platform.runLater(() -> {

                            cmbMovie.show();
                        });
                    }
                }
        );


        editor.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    String searchText =
                            newValue == null
                                    ? ""
                                    : newValue
                                            .trim()
                                            .toLowerCase();


                    ObservableList<Movie>
                            filteredMovies =
                            FXCollections
                                    .observableArrayList();


                    if (searchText.isEmpty()) {

                        filteredMovies.addAll(
                                allMovies
                        );

                    } else {

                        for (Movie movie :
                                allMovies) {

                            if (movie.getTitle() != null &&
                                movie.getTitle()
                                        .toLowerCase()
                                        .contains(searchText)) {

                                filteredMovies.add(
                                        movie
                                );
                            }
                        }
                    }


                    cmbMovie.setItems(
                            filteredMovies
                    );


                    Platform.runLater(() -> {

                        if (!filteredMovies.isEmpty()) {

                            cmbMovie.show();
                        }
                    });
                }
        );


        cmbMovie.setOnAction(event -> {

            Movie selected =
                    cmbMovie
                            .getSelectionModel()
                            .getSelectedItem();


            if (selected != null) {

                cmbMovie.setValue(selected);

                cmbMovie.getEditor()
                        .setText(
                                selected.getTitle()
                        );

                updateEndTime();
            }
        });
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


                allMovies.add(movie);
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
    // SAVE SHOW
    // =========================================================

    @FXML
    private void handleSave() throws SQLException {

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
                    "Add Show",
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
        // VERIFY AUTOMATIC DURATION
        // -----------------------------------------------------

        int movieDuration =
                getMovieDuration(
                        cmbMovie
                                .getValue()
                                .getMovieId()
                );


        if (movieDuration <= 0) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Movie Duration",
                    "This movie does not have a valid duration."
            );

            return;
        }


        // -----------------------------------------------------
        // EXPECTED END TIME
        // Movie Duration + 30 min buffer
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
                    "You cannot add a show in the past.\n\n"
                    + "Please select a future date and time."
            );

            return;
        }


        // -----------------------------------------------------
        // CHECK PRICE
        // -----------------------------------------------------

        double ticketPrice;


        try {

            ticketPrice =
                    Double.parseDouble(
                            txtTicketPrice
                                    .getText()
                                    .trim()
                    );


            if (ticketPrice < 0) {

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
                    "Please enter a valid price."
            );

            return;
        }


        // -----------------------------------------------------
        // GET IDs
        // -----------------------------------------------------

        int movieId =
                cmbMovie
                        .getValue()
                        .getMovieId();


        int hallId =
                cmbHall
                        .getValue()
                        .getHallId();


        // -----------------------------------------------------
        // SQL
        // -----------------------------------------------------

        String sql =
                "INSERT INTO movie_show " +
                "(hall_id, movie_id, show_date, " +
                "start_time, end_time, ticket_price) " +
                "VALUES (?, ?, ?, ?, ?, ?)";


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
                    null
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
            // INSERT
            // -------------------------------------------------

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {

                statement.setInt(
                        1,
                        hallId
                );

                statement.setInt(
                        2,
                        movieId
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
                        ticketPrice
                );


                statement.executeUpdate();
            }


            // -------------------------------------------------
            // REFRESH
            // -------------------------------------------------

            if (showController != null) {

                showController.loadShows();
            }


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Show added successfully."
            );


            closeWindow();


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not add the show.\n\n"
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
                                    .getDate("show_date")
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
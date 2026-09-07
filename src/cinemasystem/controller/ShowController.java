package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Show;
import cinemasystem.util.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;


public class ShowController {

    // =========================================================
    // USER / ROLE
    // =========================================================

    private String employeeName;

    private String userRole;


    public void setEmployeeName(String employeeName) {

        this.employeeName = employeeName;
    }


    public void setUserRole(String userRole) {

        this.userRole = userRole;

        applyPermissions();

        loadCinemas();

        loadShows();
    }


    // =========================================================
    // PERMISSIONS
    // =========================================================

    private boolean isAdmin() {

        return userRole != null
                && userRole.trim().equalsIgnoreCase("admin");
    }


    private boolean isManager() {

        return userRole != null
                && userRole.trim().equalsIgnoreCase("manager");
    }


    private boolean isEmployee() {

        return userRole != null
                && userRole.trim().equalsIgnoreCase("employee");
    }


    private boolean canManageShows() {

        return isAdmin() || isManager();
    }


    private boolean canSeeReport() {

        return isAdmin() || isManager();
    }


    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnEdit;

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnReport;


    @FXML
    private TextField txtSearch;


    @FXML
    private ComboBox<CinemaItem> cmbReportCinema;


    @FXML
    private Label lblCurrentDate;


    @FXML
    private ScrollPane scheduleScroll;


    @FXML
    private VBox scheduleContainer;


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<Show> showList =
            FXCollections.observableArrayList();


    private FilteredList<Show> filteredShows;


    private final ObservableList<CinemaItem> cinemaList =
            FXCollections.observableArrayList();


    private LocalDate selectedDate =
            LocalDate.now();


    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");


    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        updateDateLabel();

        setupSearch();
    }


    // =========================================================
    // APPLY PERMISSIONS
    // =========================================================

    private void applyPermissions() {

        boolean canManage =
                canManageShows();

        boolean canReport =
                canSeeReport();


        if (btnAdd != null) {

            btnAdd.setVisible(canManage);
            btnAdd.setManaged(canManage);
            btnAdd.setDisable(!canManage);
        }


        if (btnEdit != null) {

            btnEdit.setVisible(canManage);
            btnEdit.setManaged(canManage);
            btnEdit.setDisable(!canManage);
        }


        if (btnDelete != null) {

            btnDelete.setVisible(canManage);
            btnDelete.setManaged(canManage);
            btnDelete.setDisable(!canManage);
        }


        if (btnReport != null) {

            btnReport.setVisible(canReport);
            btnReport.setManaged(canReport);
            btnReport.setDisable(!canReport);
        }


        if (cmbReportCinema != null) {

            cmbReportCinema.setVisible(canReport);
            cmbReportCinema.setManaged(canReport);
            cmbReportCinema.setDisable(!canReport);
        }
    }


    // =========================================================
    // LOAD SHOWS
    // =========================================================

    public void loadShows() {

        showList.clear();


        String role =
                userRole == null
                        ? ""
                        : userRole.trim().toLowerCase();


        String sql;


        // =====================================================
        // ADMIN
        // =====================================================

        if (role.equals("admin")) {

            sql =
                    "SELECT ms.show_id, " +
                    "m.title AS movie, " +
                    "h.hall_name AS hall, " +
                    "c.cinema_name AS cinemaName, " +
                    "ms.show_date, " +
                    "ms.start_time, " +
                    "ms.end_time, " +
                    "ms.ticket_price " +
                    "FROM movie_show ms " +
                    "JOIN movie m ON ms.movie_id = m.movie_id " +
                    "JOIN hall h ON ms.hall_id = h.hall_id " +
                    "JOIN cinema c ON h.cinema_id = c.cinema_id " +
                    "WHERE ms.show_date = ? " +
                    "ORDER BY h.hall_name, ms.start_time";
        }


        // =====================================================
        // MANAGER / EMPLOYEE
        // =====================================================

        else if (role.equals("manager")
                || role.equals("employee")) {

            sql =
                    "SELECT ms.show_id, " +
                    "m.title AS movie, " +
                    "h.hall_name AS hall, " +
                    "c.cinema_name AS cinemaName, " +
                    "ms.show_date, " +
                    "ms.start_time, " +
                    "ms.end_time, " +
                    "ms.ticket_price " +
                    "FROM movie_show ms " +
                    "JOIN movie m ON ms.movie_id = m.movie_id " +
                    "JOIN hall h ON ms.hall_id = h.hall_id " +
                    "JOIN cinema c ON h.cinema_id = c.cinema_id " +
                    "WHERE h.cinema_id = ? " +
                    "AND ms.show_date = ? " +
                    "ORDER BY h.hall_name, ms.start_time";
        }


        else {

            filteredShows =
                    new FilteredList<>(
                            showList,
                            show -> true
                    );

            buildSchedule();

            return;
        }


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {


            int index = 1;


            if (role.equals("manager")
                    || role.equals("employee")) {

                statement.setInt(
                        index++,
                        Session.getCinemaId()
                );
            }


            statement.setString(
                    index,
                    selectedDate.toString()
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    Show show =
                            new Show(
                                    result.getInt("show_id"),
                                    result.getString("movie"),
                                    result.getString("hall"),
                                    result.getString("cinemaName"),
                                    result.getString("show_date"),
                                    result.getString("start_time"),
                                    result.getString("end_time"),
                                    result.getDouble("ticket_price")
                            );


                    showList.add(show);
                }
            }


            filteredShows =
                    new FilteredList<>(
                            showList,
                            show -> true
                    );


            buildSchedule();


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load shows.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // BUILD SCHEDULE
    // =========================================================

    private void buildSchedule() {

        if (scheduleContainer == null) {
            return;
        }


        scheduleContainer.getChildren().clear();


        List<String> halls =
                new ArrayList<>();


        for (Show show : showList) {

            if (!halls.contains(show.getHall())) {

                halls.add(show.getHall());
            }
        }


        // =====================================================
        // EMPTY
        // =====================================================

        if (halls.isEmpty()) {

            VBox emptyBox =
                    new VBox(10);

            emptyBox.setAlignment(
                    javafx.geometry.Pos.CENTER
            );


            Label empty =
                    new Label(
                            "No shows scheduled for this day."
                    );


            empty.setStyle(
                    "-fx-font-size:18px;" +
                    "-fx-text-fill:#777777;" +
                    "-fx-font-weight:bold;"
            );


            emptyBox.getChildren().add(empty);


            scheduleContainer
                    .getChildren()
                    .add(emptyBox);


            return;
        }


        // =====================================================
        // TIME HEADER
        // =====================================================

        scheduleContainer
                .getChildren()
                .add(
                        createTimeHeader()
                );


        // =====================================================
        // HALL ROWS
        // =====================================================

        for (String hall : halls) {

            scheduleContainer
                    .getChildren()
                    .add(
                            createHallRow(hall)
                    );
        }
    }


    // =========================================================
    // TIME HEADER
    // =========================================================

    private HBox createTimeHeader() {

        HBox header =
                new HBox();


        header.setPrefHeight(45);


        header.setStyle(
                "-fx-background-color:#111111;" +
                "-fx-background-radius:8;"
        );


        Label hallLabel =
                new Label("HALL");


        hallLabel.setPrefWidth(120);


        hallLabel.setAlignment(
                javafx.geometry.Pos.CENTER
        );


        hallLabel.setStyle(
                "-fx-text-fill:white;" +
                "-fx-font-size:13px;" +
                "-fx-font-weight:bold;"
        );


        header.getChildren().add(
                hallLabel
        );


        for (int hour = 10; hour <= 23; hour++) {

            Label time =
                    new Label(
                            String.format(
                                    "%02d:00",
                                    hour
                            )
                    );


            time.setPrefWidth(100);


            time.setAlignment(
                    javafx.geometry.Pos.CENTER
            );


            time.setStyle(
                    "-fx-text-fill:#BDBDBD;" +
                    "-fx-font-size:12px;" +
                    "-fx-font-weight:bold;" +
                    "-fx-border-color:#333333;" +
                    "-fx-border-width:0 1 0 0;"
            );


            header.getChildren().add(time);
        }


        return header;
    }


    // =========================================================
    // HALL ROW
    // =========================================================

    private HBox createHallRow(String hallName) {

        HBox row =
                new HBox();


        row.setPrefHeight(125);


        row.setMinHeight(125);


        row.setStyle(
                "-fx-background-color:white;" +
                "-fx-background-radius:8;" +
                "-fx-border-color:#DDDDDD;" +
                "-fx-border-radius:8;"
        );


        Label hall =
                new Label(
                        hallName
                );


        hall.setPrefWidth(120);


        hall.setAlignment(
                javafx.geometry.Pos.CENTER
        );


        hall.setStyle(
                "-fx-font-size:14px;" +
                "-fx-font-weight:bold;" +
                "-fx-text-fill:#333333;" +
                "-fx-background-color:#F5F5F5;" +
                "-fx-border-color:#DDDDDD;" +
                "-fx-border-width:0 1 0 0;"
        );


        row.getChildren().add(hall);


        HBox timeline =
                new HBox();


        timeline.setPrefHeight(125);


        timeline.setMinWidth(1400);


        timeline.setStyle(
                "-fx-background-color:white;"
        );


        // -----------------------------------------------------
        // HOURS BACKGROUND
        // -----------------------------------------------------

        for (int hour = 10; hour <= 23; hour++) {

            Region hourBox =
                    new Region();


            hourBox.setPrefWidth(100);


            hourBox.setMinWidth(100);


            hourBox.setPrefHeight(125);


            hourBox.setStyle(
                    "-fx-border-color:#EEEEEE;" +
                    "-fx-border-width:0 1 0 0;"
            );


            timeline.getChildren().add(
                    hourBox
            );
        }


        // -----------------------------------------------------
        // SHOW CARDS
        // -----------------------------------------------------

        for (Show show : showList) {

            if (!safe(show.getHall())
                    .equalsIgnoreCase(
                            safe(hallName)
                    )) {

                continue;
            }


            VBox card =
                    createShowCard(show);


            int startMinutes =
                    convertTimeToMinutes(
                            show.getStartTime()
                    );


            int endMinutes =
                    convertTimeToMinutes(
                            show.getEndTime()
                    );


            int startFrom10 =
                    startMinutes - (10 * 60);


            int duration =
                    endMinutes - startMinutes;


            if (startFrom10 < 0) {
                startFrom10 = 0;
            }


            if (duration <= 0) {
                duration = 60;
            }


            double x =
                    startFrom10
                    / 60.0
                    * 100;


            double width =
                    duration
                    / 60.0
                    * 100;


            card.setPrefWidth(
                    Math.max(width - 6, 90)
            );


            card.setMinWidth(
                    Math.max(width - 6, 90)
            );


            // -------------------------------------------------
            // Position using margin
            // -------------------------------------------------

            HBox.setMargin(
                    card,
                    new javafx.geometry.Insets(
                            10,
                            0,
                            10,
                            x
                    )
            );


            timeline.getChildren().add(
                    card
            );
        }


        row.getChildren().add(
                timeline
        );


        return row;
    }


    // =========================================================
    // SHOW CARD
    // =========================================================

    private VBox createShowCard(Show show) {

        VBox card =
                new VBox(5);


        card.setPrefHeight(105);


        card.setMaxHeight(105);


        card.setStyle(
                "-fx-background-color:#2E7D32;" +
                "-fx-background-radius:8;" +
                "-fx-padding:10;" +
                "-fx-cursor:hand;"
        );


        Label movie =
                new Label(
                        "🎬  "
                        + safe(show.getMovie())
                );


        movie.setWrapText(true);


        movie.setStyle(
                "-fx-text-fill:white;" +
                "-fx-font-size:14px;" +
                "-fx-font-weight:bold;"
        );


        Label time =
                new Label(
                        safe(show.getStartTime())
                        + " → "
                        + safe(show.getEndTime())
                );


        time.setStyle(
                "-fx-text-fill:#E8F5E9;" +
                "-fx-font-size:12px;" +
                "-fx-font-weight:bold;"
        );


        Label cinema =
                new Label(
                        safe(show.getCinemaName())
                );


        cinema.setStyle(
                "-fx-text-fill:#C8E6C9;" +
                "-fx-font-size:11px;"
        );


        Label price =
                new Label(
                        String.format(
                                "$%.2f",
                                show.getTicketPrice()
                        )
                );


        price.setStyle(
                "-fx-text-fill:white;" +
                "-fx-font-size:12px;" +
                "-fx-font-weight:bold;"
        );


        card.getChildren().addAll(
                movie,
                time,
                cinema,
                price
        );


        // =====================================================
        // CLICK SHOW → SELECT
        // =====================================================

        card.setOnMouseClicked(
                event -> {

                    if (event.getClickCount() == 1) {

                        card.setStyle(
                                "-fx-background-color:#1B5E20;" +
                                "-fx-background-radius:8;" +
                                "-fx-padding:10;" +
                                "-fx-cursor:hand;" +
                                "-fx-border-color:#81C784;" +
                                "-fx-border-width:2;" +
                                "-fx-border-radius:8;"
                        );
                    }


                    if (event.getClickCount() == 2) {

                        if (canManageShows()) {

                            openEditShow(show);
                        }
                    }
                }
        );


        return card;
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

                    if (filteredShows == null) {
                        return;
                    }


                    String searchText =
                            newValue == null
                                    ? ""
                                    : newValue.trim()
                                            .toLowerCase();


                    filteredShows.setPredicate(
                            show -> {

                                if (searchText.isEmpty()) {

                                    return true;
                                }


                                return safe(
                                        show.getMovie()
                                ).contains(searchText)

                                || safe(
                                        show.getHall()
                                ).contains(searchText)

                                || safe(
                                        show.getCinemaName()
                                ).contains(searchText)

                                || safe(
                                        show.getShowDate()
                                ).contains(searchText)

                                || safe(
                                        show.getStartTime()
                                ).contains(searchText)

                                || safe(
                                        show.getEndTime()
                                ).contains(searchText)

                                || String.valueOf(
                                        show.getShowId()
                                ).contains(searchText);
                            }
                    );


                    buildFilteredSchedule();
                }
        );
    }


    // =========================================================
    // FILTERED SCHEDULE
    // =========================================================

    private void buildFilteredSchedule() {

        if (filteredShows == null) {
            return;
        }


        ObservableList<Show> original =
                FXCollections.observableArrayList(
                        showList
                );


        showList.clear();


        showList.addAll(
                filteredShows
        );


        buildSchedule();


        showList.clear();


        showList.addAll(
                original
        );


        filteredShows.setPredicate(
                show -> true
        );
    }


    // =========================================================
    // DATE NAVIGATION
    // =========================================================

    @FXML
    private void handlePreviousDay() {

        selectedDate =
                selectedDate.minusDays(1);


        updateDateLabel();

        loadShows();
    }


    @FXML
    private void handleNextDay() {

        selectedDate =
                selectedDate.plusDays(1);


        updateDateLabel();

        loadShows();
    }


    @FXML
    private void handleToday() {

        selectedDate =
                LocalDate.now();


        updateDateLabel();

        loadShows();
    }


    private void updateDateLabel() {

        if (lblCurrentDate == null) {
            return;
        }


        if (selectedDate.equals(
                LocalDate.now()
        )) {

            lblCurrentDate.setText(
                    "Today • "
                    + selectedDate.format(
                            DateTimeFormatter.ofPattern(
                                    "MMM dd"
                            )
                    )
            );

        } else {

            lblCurrentDate.setText(
                    selectedDate.format(
                            DateTimeFormatter.ofPattern(
                                    "EEE, MMM dd"
                            )
                    )
            );
        }
    }


    // =========================================================
    // ADD SHOW
    // =========================================================

    @FXML
    private void handleAdd() {

        if (!canManageShows()) {

            showAccessDenied();

            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/AddShow.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            AddShowController controller =
                    loader.getController();


            controller.setShowController(this);


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Add Show"
            );


            stage.setScene(
                    new Scene(root)
            );


            setIcon(stage);


            stage.show();


        } catch (Exception e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Add Show",
                    "Could not open Add Show window.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // EDIT SHOW
    // =========================================================

    @FXML
    private void handleEdit() {

        if (!canManageShows()) {

            showAccessDenied();

            return;
        }


        Show selected =
                getSelectedShow();


        if (selected == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Show",
                    "Please select a show first."
            );

            return;
        }


        openEditShow(selected);
    }


    private void openEditShow(Show selectedShow) {

        try {

            int movieId = 0;

            int hallId = 0;


            // -------------------------------------------------
            // MOVIE ID
            // -------------------------------------------------

            String movieSql =
                    "SELECT movie_id " +
                    "FROM movie " +
                    "WHERE title = ?";


            try (
                    Connection connection =
                            DBConnection.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    movieSql
                            )
            ) {

                statement.setString(
                        1,
                        selectedShow.getMovie()
                );


                try (
                        ResultSet result =
                                statement.executeQuery()
                ) {

                    if (result.next()) {

                        movieId =
                                result.getInt(
                                        "movie_id"
                                );
                    }
                }
            }


            // -------------------------------------------------
            // HALL ID
            // -------------------------------------------------

            String hallSql =
                    "SELECT hall_id " +
                    "FROM hall " +
                    "WHERE hall_name = ?";


            try (
                    Connection connection =
                            DBConnection.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    hallSql
                            )
            ) {

                statement.setString(
                        1,
                        selectedShow.getHall()
                );


                try (
                        ResultSet result =
                                statement.executeQuery()
                ) {

                    if (result.next()) {

                        hallId =
                                result.getInt(
                                        "hall_id"
                                );
                    }
                }
            }


            if (movieId == 0
                    || hallId == 0) {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Edit Show",
                        "Could not find movie or hall."
                );

                return;
            }


            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/EditShow.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            EditShowController controller =
                    loader.getController();


            controller.setShowController(
                    this
            );


            controller.setShow(
                    selectedShow,
                    movieId,
                    hallId
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Edit Show"
            );


            stage.setScene(
                    new Scene(root)
            );


            setIcon(stage);


            stage.show();


        } catch (Exception e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Edit Show",
                    "Could not open Edit Show window.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    @FXML
    private void handleDelete() {

        if (!canManageShows()) {

            showAccessDenied();

            return;
        }


        Show selected =
                getSelectedShow();


        if (selected == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Delete Show",
                    "Please select a show first."
            );

            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Delete Show"
        );


        confirmation.setHeaderText(null);


        confirmation.setContentText(
                "Are you sure you want to delete:\n\n"
                + selected.getMovie()
                + "?"
        );


        if (
                confirmation.showAndWait()
                        .orElse(null)
                        != ButtonType.OK
        ) {

            return;
        }


        String sql =
                "DELETE FROM movie_show " +
                "WHERE show_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    selected.getShowId()
            );


            statement.executeUpdate();


            loadShows();


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Delete Show",
                    "Show deleted successfully."
            );


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Delete Show",
                    "Could not delete the show.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // GET SELECTED SHOW
    // =========================================================

    private Show getSelectedShow() {

        /*
         * Since the timeline uses cards instead
         * of TableView, we keep a selectedShow.
         */

        return selectedShow;
    }


    private Show selectedShow;


    // =========================================================
    // REPORT
    // =========================================================

    @FXML
    private void handleReport() {

        if (!canSeeReport()) {

            showAccessDenied();

            return;
        }


        CinemaItem selectedCinema =
                cmbReportCinema.getValue();


        if (selectedCinema == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Shows Report",
                    "Please select a cinema first."
            );

            return;
        }


        String sql =
                "SELECT " +
                "COUNT(*) AS total_shows, " +
                "COUNT(DISTINCT ms.movie_id) AS total_movies, " +
                "COUNT(DISTINCT ms.hall_id) AS total_halls, " +
                "AVG(ms.ticket_price) AS average_price, " +
                "MIN(ms.ticket_price) AS minimum_price, " +
                "MAX(ms.ticket_price) AS maximum_price " +
                "FROM movie_show ms " +
                "JOIN hall h " +
                "ON ms.hall_id = h.hall_id " +
                "WHERE h.cinema_id = ? " +
                "AND ms.show_date = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    selectedCinema.getCinemaId()
            );


            statement.setString(
                    2,
                    selectedDate.toString()
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    String report =
                            "SHOWS REPORT\n"
                            + "==============================\n\n"

                            + "Cinema: "
                            + selectedCinema.getCinemaName()
                            + "\n\n"

                            + "Date: "
                            + selectedDate
                            + "\n\n"

                            + "Total Shows: "
                            + result.getInt(
                                    "total_shows"
                            )
                            + "\n\n"

                            + "Different Movies: "
                            + result.getInt(
                                    "total_movies"
                            )
                            + "\n\n"

                            + "Halls Used: "
                            + result.getInt(
                                    "total_halls"
                            )
                            + "\n\n"

                            + "Average Ticket Price: "
                            + String.format(
                                    "%.2f",
                                    result.getDouble(
                                            "average_price"
                                    )
                            )
                            + "\n\n"

                            + "Minimum Ticket Price: "
                            + String.format(
                                    "%.2f",
                                    result.getDouble(
                                            "minimum_price"
                                    )
                            )
                            + "\n\n"

                            + "Maximum Ticket Price: "
                            + String.format(
                                    "%.2f",
                                    result.getDouble(
                                            "maximum_price"
                                    )
                            );


                    showAlert(
                            Alert.AlertType.INFORMATION,
                            "Shows Report",
                            report
                    );
                }
            }


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Report Error",
                    "Could not generate report.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // LOAD CINEMAS
    // =========================================================

    private void loadCinemas() {

        if (cmbReportCinema == null) {
            return;
        }


        cinemaList.clear();


        String role =
                userRole == null
                        ? ""
                        : userRole.trim().toLowerCase();


        String sql;


        if (role.equals("admin")) {

            sql =
                    "SELECT cinema_id, cinema_name " +
                    "FROM cinema " +
                    "ORDER BY cinema_name";
        }


        else if (role.equals("manager")) {

            sql =
                    "SELECT cinema_id, cinema_name " +
                    "FROM cinema " +
                    "WHERE cinema_id = ?";
        }


        else {

            cmbReportCinema.setItems(
                    cinemaList
            );

            return;
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


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    cinemaList.add(
                            new CinemaItem(
                                    result.getInt(
                                            "cinema_id"
                                    ),
                                    result.getString(
                                            "cinema_name"
                                    )
                            )
                    );
                }
            }


            cmbReportCinema.setItems(
                    cinemaList
            );


            if (!cinemaList.isEmpty()) {

                cmbReportCinema
                        .getSelectionModel()
                        .selectFirst();
            }


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
    // DUPLICATE CHECK
    // =========================================================

    public boolean showAlreadyExists(
            int hallId,
            String showDate,
            String startTime,
            Integer excludeShowId) {

        String sql =
                "SELECT COUNT(*) " +
                "FROM movie_show " +
                "WHERE hall_id = ? " +
                "AND show_date = ? " +
                "AND start_time = ? ";


        if (excludeShowId != null) {

            sql +=
                    "AND show_id <> ?";
        }


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            int index = 1;


            statement.setInt(
                    index++,
                    hallId
            );


            statement.setString(
                    index++,
                    showDate
            );


            statement.setString(
                    index++,
                    startTime
            );


            if (excludeShowId != null) {

                statement.setInt(
                        index,
                        excludeShowId
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getInt(1) > 0;
                }
            }


        } catch (SQLException e) {

            e.printStackTrace();
        }


        return false;
    }


    public boolean showAlreadyExists(
            int hallId,
            String showDate,
            String startTime) {

        return showAlreadyExists(
                hallId,
                showDate,
                startTime,
                null
        );
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
                    (Stage)
                            scheduleContainer
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
                    "Could not return to dashboard."
            );
        }
    }


    // =========================================================
    // TIME CONVERSION
    // =========================================================

    private int convertTimeToMinutes(
            String time) {

        if (time == null
                || time.isBlank()) {

            return 0;
        }


        try {

            String clean =
                    time.length() >= 5
                            ? time.substring(0, 5)
                            : time;


            LocalTime localTime =
                    LocalTime.parse(
                            clean,
                            TIME_FORMAT
                    );


            return localTime.getHour() * 60
                    + localTime.getMinute();


        } catch (Exception e) {

            return 0;
        }
    }


    // =========================================================
    // SAFE
    // =========================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }


    // =========================================================
    // ICON
    // =========================================================

    private void setIcon(Stage stage) {

        try {

            stage.getIcons().add(
                    new javafx.scene.image.Image(
                            getClass()
                                    .getResourceAsStream(
                                            "/cinemasystem/images/icon.png"
                                    )
                    )
            );

        } catch (Exception ignored) {
        }
    }


    // =========================================================
    // ACCESS DENIED
    // =========================================================

    private void showAccessDenied() {

        showAlert(
                Alert.AlertType.WARNING,
                "Access Denied",
                "You do not have permission to perform this action."
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


    // =========================================================
    // CINEMA ITEM
    // =========================================================

    public static class CinemaItem {

        private final int cinemaId;

        private final String cinemaName;


        public CinemaItem(
                int cinemaId,
                String cinemaName) {

            this.cinemaId =
                    cinemaId;

            this.cinemaName =
                    cinemaName;
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
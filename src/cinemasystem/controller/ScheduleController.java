package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Show;
import cinemasystem.util.Session;

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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

import javafx.scene.input.MouseButton;

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


public class ScheduleController {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM dd");


    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<CinemaItem> cmbCinema;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnEdit;

    @FXML
    private Button btnDelete;


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<Show> showList =
            FXCollections.observableArrayList();

    private final ObservableList<CinemaItem> cinemaList =
            FXCollections.observableArrayList();

    private Show selectedShow;

    private String employeeName;

    private String userRole;


    // =========================================================
    // SETTERS
    // =========================================================

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }


    public void setUserRole(String userRole) {

        this.userRole = userRole;

        applyPermissions();
        loadCinemas();
        refreshSchedule();
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        // -----------------------------------------------------
        // SEARCH
        // -----------------------------------------------------

        if (txtSearch != null) {

            txtSearch.textProperty().addListener(
                    (observable, oldValue, newValue) -> {

                        renderShows();
                    }
            );
        }


        // -----------------------------------------------------
        // CINEMA
        // -----------------------------------------------------

        if (cmbCinema != null) {

            cmbCinema.valueProperty().addListener(
                    (observable, oldValue, newValue) -> {

                        selectedShow = null;

                        hideActionButtons();

                        if (newValue != null) {
                            refreshSchedule();
                        }
                    }
            );
        }


        // -----------------------------------------------------
        // SESSION
        // -----------------------------------------------------

        String sessionRole = Session.getRole();

        if (sessionRole != null) {

            userRole = sessionRole.trim();

            applyPermissions();

            loadCinemas();

            refreshSchedule();

        } else {

            hideActionButtons();
        }
    }


    // =========================================================
    // PERMISSIONS
    // =========================================================

    private boolean isAdmin() {

        return userRole != null
                && userRole.trim()
                .equalsIgnoreCase("admin");
    }


    private boolean isManager() {

        return userRole != null
                && userRole.trim()
                .equalsIgnoreCase("manager");
    }


    private boolean isEmployee() {

        return userRole != null
                && userRole.trim()
                .equalsIgnoreCase("employee");
    }


    private boolean canManageShows() {

        return isAdmin()
                || isManager();
    }


    private void applyPermissions() {

        boolean canManage =
                canManageShows();

        // -----------------------------------------------------
        // ADD
        // -----------------------------------------------------

        if (btnAdd != null) {

            btnAdd.setVisible(canManage);

            btnAdd.setManaged(canManage);

            btnAdd.setDisable(!canManage);
        }


        hideActionButtons();
    }


    private void hideActionButtons() {

        if (btnEdit != null) {

            btnEdit.setVisible(false);

            btnEdit.setManaged(false);

            btnEdit.setDisable(true);
        }


        if (btnDelete != null) {

            btnDelete.setVisible(false);

            btnDelete.setManaged(false);

            btnDelete.setDisable(true);
        }
    }


    private void showActionButtons(Show show) {

        selectedShow = show;

        boolean canManage =
                canManageShows();


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
    }


    // =========================================================
    // CINEMAS
    // =========================================================

    private void loadCinemas() {

        if (cmbCinema == null) {
            return;
        }


        cinemaList.clear();


        String role =
                userRole == null
                        ? ""
                        : userRole.trim().toLowerCase();


        // =====================================================
        // ADMIN
        // =====================================================

        if (role.equals("admin")) {

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


        // =====================================================
        // MANAGER / EMPLOYEE
        // =====================================================

        else if (
                role.equals("manager")
                        || role.equals("employee")
        ) {

            int cinemaId =
                    Session.getCinemaId();


            String sql =
                    "SELECT cinema_id, cinema_name "
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

            } catch (SQLException e) {

                e.printStackTrace();

                showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Could not load cinema.\n\n"
                                + e.getMessage()
                );
            }
        }


        cmbCinema.setItems(
                cinemaList
        );


        if (!cinemaList.isEmpty()) {

            cmbCinema
                    .getSelectionModel()
                    .selectFirst();
        }
    }


    // =========================================================
    // REFRESH
    // =========================================================

    private void refreshSchedule() {

        if (cmbCinema == null) {
            return;
        }


        CinemaItem cinema =
                cmbCinema.getValue();


        if (cinema == null) {

            showList.clear();

            selectedShow = null;

            hideActionButtons();

            renderShows();

            return;
        }


        loadAllShowsForCinema(
                cinema.getCinemaId()
        );


        selectedShow = null;

        hideActionButtons();

        renderShows();
    }


    // =========================================================
    // LOAD ALL UPCOMING SHOWS
    // =========================================================

    private void loadAllShowsForCinema(
            int cinemaId
    ) {

        showList.clear();


        /*
         * Only today and future shows.
         *
         * Old shows are not displayed.
         */

        String sql =
                "SELECT ms.show_id, "
                        + "m.title AS movie, "
                        + "h.hall_name AS hall, "
                        + "c.cinema_name AS cinemaName, "
                        + "ms.show_date, "
                        + "ms.start_time, "
                        + "ms.end_time, "
                        + "ms.ticket_price "
                        + "FROM movie_show ms "
                        + "JOIN movie m "
                        + "ON ms.movie_id = m.movie_id "
                        + "JOIN hall h "
                        + "ON ms.hall_id = h.hall_id "
                        + "JOIN cinema c "
                        + "ON h.cinema_id = c.cinema_id "
                        + "WHERE h.cinema_id = ? "
                        + "AND ms.show_date >= ? "
                        + "ORDER BY ms.show_date, "
                        + "h.hall_id, "
                        + "ms.start_time";


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

            statement.setString(
                    2,
                    LocalDate.now().toString()
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    Show show =
                            createShowFromResult(
                                    result
                            );

                    showList.add(show);
                }
            }

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
    // CREATE SHOW
    // =========================================================

    private Show createShowFromResult(
            ResultSet result
    ) throws SQLException {

        return new Show(
                result.getInt("show_id"),
                result.getString("movie"),
                result.getString("hall"),
                result.getString("cinemaName"),
                result.getString("show_date"),
                result.getString("start_time"),
                result.getString("end_time"),
                result.getDouble("ticket_price")
        );
    }


    // =========================================================
    // RENDER SHOWS
    // =========================================================

    private void renderShows() {

        if (txtSearch == null) {
            return;
        }


        VBox container =
                new VBox(12);

        container.setFillWidth(true);

        container.setPadding(
                new Insets(5)
        );

        container.setStyle(
                "-fx-background-color:#ECECEC;"
        );


        String search =
                txtSearch.getText()
                        .trim()
                        .toLowerCase();


        boolean found = false;

        String lastDate = "";


        for (Show show : showList) {

            // -------------------------------------------------
            // SAFETY CHECK
            // -------------------------------------------------

            LocalDate showDate =
                    parseDate(
                            show.getShowDate()
                    );


            if (
                    showDate != null
                            && showDate.isBefore(
                            LocalDate.now()
                    )
            ) {
                continue;
            }


            // -------------------------------------------------
            // SEARCH
            // -------------------------------------------------

            if (
                    !matchesSearch(
                            show,
                            search
                    )
            ) {
                continue;
            }


            found = true;


            String date =
                    safe(
                            show.getShowDate()
                    );


            // -------------------------------------------------
            // DATE HEADER
            // -------------------------------------------------

            if (!date.equals(lastDate)) {

                Label dateLabel =
                        new Label(
                                formatShowDate(date)
                        );


                dateLabel.setStyle(
                        "-fx-text-fill:#222222;"
                                + "-fx-font-size:18px;"
                                + "-fx-font-weight:bold;"
                                + "-fx-padding:12 5 5 5;"
                );


                container
                        .getChildren()
                        .add(dateLabel);


                lastDate = date;
            }


            // -------------------------------------------------
            // SHOW CARD
            // -------------------------------------------------

            HBox card =
                    createShowCard(show);


            container
                    .getChildren()
                    .add(card);
        }


        // =====================================================
        // EMPTY
        // =====================================================

        if (!found) {

            Label empty =
                    new Label(
                            showList.isEmpty()
                                    ? "No upcoming shows found for this cinema."
                                    : "No upcoming shows match your search."
                    );


            empty.setStyle(
                    "-fx-text-fill:#777777;"
                            + "-fx-font-size:16px;"
                            + "-fx-font-weight:bold;"
                            + "-fx-padding:25;"
            );


            container
                    .getChildren()
                    .add(empty);
        }


        // =====================================================
        // BODY
        // =====================================================

        ScrollPane scrollPane =
                findBodyScroll();


        if (scrollPane != null) {

            scrollPane.setContent(
                    container
            );

            scrollPane.setFitToWidth(true);

            scrollPane.setHbarPolicy(
                    ScrollPane.ScrollBarPolicy.NEVER
            );
        }
    }


    // =========================================================
    // BODY SCROLL
    // =========================================================

    private ScrollPane findBodyScroll() {

        if (txtSearch == null) {
            return null;
        }


        Node parent =
                txtSearch;


        while (
                parent != null
                        && parent.getParent() != null
        ) {

            parent = parent.getParent();


            if (parent instanceof ScrollPane) {

                ScrollPane scroll =
                        (ScrollPane) parent;

                /*
                 * The first ScrollPane under the
                 * page body is the schedule body.
                 */
            }
        }


        /*
         * We don't use this method because the
         * ScrollPane is injected through the FXML.
         *
         * The actual body is stored in bodyScroll.
         */

        return bodyScroll;
    }


    // =========================================================
    // BODY SCROLL FXML
    // =========================================================

    @FXML
    private ScrollPane bodyScroll;


    // =========================================================
    // SHOW CARD
    // =========================================================

    private HBox createShowCard(
            Show show
    ) {

        HBox card =
                new HBox(18);


        card.setAlignment(
                Pos.CENTER_LEFT
        );


        card.setPadding(
                new Insets(
                        14,
                        18,
                        14,
                        18
                )
        );


        card.setMaxWidth(
                Double.MAX_VALUE
        );


        card.setPrefHeight(82);


        setShowNormalStyle(card);


        // =====================================================
        // MOVIE
        // =====================================================

        VBox movieBox =
                new VBox(3);


        movieBox.setPrefWidth(280);

        movieBox.setMinWidth(280);


        Label movie =
                new Label(
                        safe(
                                show.getMovie()
                        )
                );


        movie.setStyle(
                "-fx-text-fill:#222222;"
                        + "-fx-font-size:16px;"
                        + "-fx-font-weight:bold;"
        );


        movie.setWrapText(true);


        Label cinema =
                new Label(
                        safe(
                                show.getCinemaName()
                        )
                );


        cinema.setStyle(
                "-fx-text-fill:#888888;"
                        + "-fx-font-size:12px;"
        );


        movieBox
                .getChildren()
                .addAll(
                        movie,
                        cinema
                );


        // =====================================================
        // DATE
        // =====================================================

        VBox dateBox =
                new VBox(3);


        dateBox.setPrefWidth(130);

        dateBox.setMinWidth(130);


        Label dateTitle =
                new Label("DATE");


        dateTitle.setStyle(
                "-fx-text-fill:#999999;"
                        + "-fx-font-size:10px;"
                        + "-fx-font-weight:bold;"
        );


        Label date =
                new Label(
                        formatShowDate(
                                show.getShowDate()
                        )
                );


        date.setStyle(
                "-fx-text-fill:#333333;"
                        + "-fx-font-size:13px;"
                        + "-fx-font-weight:bold;"
        );


        dateBox
                .getChildren()
                .addAll(
                        dateTitle,
                        date
                );


        // =====================================================
        // HALL
        // =====================================================

        VBox hallBox =
                new VBox(3);


        hallBox.setPrefWidth(130);

        hallBox.setMinWidth(130);


        Label hallTitle =
                new Label("HALL");


        hallTitle.setStyle(
                "-fx-text-fill:#999999;"
                        + "-fx-font-size:10px;"
                        + "-fx-font-weight:bold;"
        );


        Label hall =
                new Label(
                        safe(
                                show.getHall()
                        )
                );


        hall.setStyle(
                "-fx-text-fill:#333333;"
                        + "-fx-font-size:13px;"
                        + "-fx-font-weight:bold;"
        );


        hallBox
                .getChildren()
                .addAll(
                        hallTitle,
                        hall
                );


        // =====================================================
        // TIME
        // =====================================================

        VBox timeBox =
                new VBox(3);


        timeBox.setPrefWidth(150);

        timeBox.setMinWidth(150);


        Label timeTitle =
                new Label("TIME");


        timeTitle.setStyle(
                "-fx-text-fill:#999999;"
                        + "-fx-font-size:10px;"
                        + "-fx-font-weight:bold;"
        );


        Label time =
                new Label(
                        formatTime(
                                show.getStartTime()
                        )
                                + " - "
                                + formatTime(
                                show.getEndTime()
                        )
                );


        time.setStyle(
                "-fx-text-fill:#2E7D32;"
                        + "-fx-font-size:14px;"
                        + "-fx-font-weight:bold;"
        );


        timeBox
                .getChildren()
                .addAll(
                        timeTitle,
                        time
                );


        // =====================================================
        // PRICE
        // =====================================================

        VBox priceBox =
                new VBox(3);


        priceBox.setPrefWidth(100);

        priceBox.setMinWidth(100);


        Label priceTitle =
                new Label("TICKET");


        priceTitle.setStyle(
                "-fx-text-fill:#999999;"
                        + "-fx-font-size:10px;"
                        + "-fx-font-weight:bold;"
        );


        Label price =
                new Label(
                        String.format(
                                "%.2f",
                                show.getTicketPrice()
                        )
                );


        price.setStyle(
                "-fx-text-fill:#222222;"
                        + "-fx-font-size:14px;"
                        + "-fx-font-weight:bold;"
        );


        priceBox
                .getChildren()
                .addAll(
                        priceTitle,
                        price
                );


        // =====================================================
        // ADD TO CARD
        // =====================================================

        card.getChildren()
                .addAll(
                        movieBox,
                        dateBox,
                        hallBox,
                        timeBox,
                        priceBox
                );


        // =====================================================
        // CLICK
        // =====================================================

        card.setOnMouseClicked(event -> {

            if (
                    event.getButton()
                            != MouseButton.PRIMARY
            ) {
                return;
            }


            selectedShow = show;


            showActionButtons(
                    show
            );


            setShowSelectedStyle(card);


            event.consume();
        });


        // =====================================================
        // HOVER
        // =====================================================

        card.setOnMouseEntered(event -> {

            if (selectedShow != show) {

                setShowHoverStyle(card);
            }
        });


        card.setOnMouseExited(event -> {

            if (selectedShow != show) {

                setShowNormalStyle(card);
            }
        });


        return card;
    }


    // =========================================================
    // CARD STYLES
    // =========================================================

    private void setShowNormalStyle(
            HBox card
    ) {

        card.setStyle(
                "-fx-background-color:white;"
                        + "-fx-background-radius:10;"
                        + "-fx-border-color:#DDDDDD;"
                        + "-fx-border-radius:10;"
                        + "-fx-cursor:hand;"
        );
    }


    private void setShowHoverStyle(
            HBox card
    ) {

        card.setStyle(
                "-fx-background-color:#F7F7F7;"
                        + "-fx-background-radius:10;"
                        + "-fx-border-color:#CCCCCC;"
                        + "-fx-border-radius:10;"
                        + "-fx-cursor:hand;"
        );
    }


    private void setShowSelectedStyle(
            HBox card
    ) {

        card.setStyle(
                "-fx-background-color:#F1F8E9;"
                        + "-fx-background-radius:10;"
                        + "-fx-border-color:#2E7D32;"
                        + "-fx-border-width:2;"
                        + "-fx-border-radius:10;"
                        + "-fx-cursor:hand;"
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private boolean matchesSearch(
            Show show,
            String search
    ) {

        if (
                search == null
                        || search.isEmpty()
        ) {

            return true;
        }


        return safe(
                show.getMovie()
        )
                .contains(search)

                || safe(
                show.getHall()
        )
                .contains(search)

                || safe(
                show.getStartTime()
        )
                .contains(search)

                || safe(
                show.getEndTime()
        )
                .contains(search)

                || safe(
                show.getShowDate()
        )
                .contains(search)

                || safe(
                show.getCinemaName()
        )
                .contains(search);
    }


    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.toLowerCase();
    }


    // =========================================================
    // DATE
    // =========================================================

    private LocalDate parseDate(
            String date
    ) {

        if (
                date == null
                        || date.isBlank()
        ) {

            return null;
        }


        try {

            return LocalDate.parse(
                    date
            );

        } catch (Exception e) {

            return null;
        }
    }


    private String formatShowDate(
            String date
    ) {

        if (
                date == null
                        || date.isBlank()
        ) {

            return "";
        }


        try {

            LocalDate parsed =
                    LocalDate.parse(
                            date
                    );


            return parsed.format(
                    DATE_FORMAT
            );

        } catch (Exception e) {

            return date;
        }
    }


    // =========================================================
    // TIME
    // =========================================================

    private String formatTime(
            String time
    ) {

        if (
                time == null
                        || time.isBlank()
        ) {

            return "";
        }


        try {

            String value =
                    time.length() >= 8
                            ? time.substring(
                            0,
                            8
                    )
                            : time;


            return LocalTime
                    .parse(value)
                    .format(
                            TIME_FORMAT
                    );

        } catch (Exception e) {

            return time;
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


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Add Show"
            );


            stage.setScene(
                    new Scene(root)
            );
            
            stage.getIcons().add(
    new javafx.scene.image.Image(
        getClass().getResourceAsStream("/cinemasystem/images/icon.png")
    )
);


            stage.showAndWait();


            refreshSchedule();

        } catch (Exception e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Add Show",
                    "Could not open Add Show.\n\n"
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // EDIT
    // =========================================================

    @FXML
    private void handleEditSelected() {

        if (selectedShow == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Show",
                    "Please select a show first."
            );

            return;
        }


        if (!canManageShows()) {

            showAccessDenied();

            return;
        }


        editShow(
                selectedShow
        );
    }


    private void editShow(
            Show selectedShow
    ) {

        try {

            int movieId =
                    getMovieId(
                            selectedShow.getMovie()
                    );


            int hallId =
                    getHallId(
                            selectedShow.getHall()
                    );


            if (
                    movieId == 0
                            || hallId == 0
            ) {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Edit Show",
                        "Could not find movie or hall."
                );

                return;
            }


            if (isManager()) {

                int cinemaId =
                        getHallCinemaId(
                                hallId
                        );


                if (
                        cinemaId
                                != Session.getCinemaId()
                ) {

                    showAccessDenied();

                    return;
                }
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
            
            stage.getIcons().add(
    new javafx.scene.image.Image(
        getClass().getResourceAsStream("/cinemasystem/images/icon.png")
    )
);


            stage.showAndWait();


            refreshSchedule();

        } catch (Exception e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Edit Show",
                    "Could not open Edit Show.\n\n"
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    @FXML
    private void handleDeleteSelected() {

        if (selectedShow == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Delete Show",
                    "Please select a show first."
            );

            return;
        }


        if (!canManageShows()) {

            showAccessDenied();

            return;
        }


        deleteShow(
                selectedShow
        );


        selectedShow = null;

        hideActionButtons();
    }


    private void deleteShow(
            Show selectedShow
    ) {

        if (!canManageShows()) {

            showAccessDenied();

            return;
        }


        if (isManager()) {

            int hallId =
                    getHallId(
                            selectedShow.getHall()
                    );


            int cinemaId =
                    getHallCinemaId(
                            hallId
                    );


            if (
                    cinemaId
                            != Session.getCinemaId()
            ) {

                showAccessDenied();

                return;
            }
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
                "Delete this show?\n\n"
                        + selectedShow.getMovie()
                        + "\n"
                        + selectedShow.getHall()
                        + "\n"
                        + formatShowDate(
                        selectedShow.getShowDate()
                )
                        + "\n"
                        + formatTime(
                        selectedShow.getStartTime()
                )
                        + " - "
                        + formatTime(
                        selectedShow.getEndTime()
                )
        );


        if (
                confirmation
                        .showAndWait()
                        .orElse(null)
                        != ButtonType.OK
        ) {

            return;
        }


        String sql =
                "DELETE FROM movie_show "
                        + "WHERE show_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    selectedShow.getShowId()
            );


            statement.executeUpdate();


            this.selectedShow = null;

            hideActionButtons();

            refreshSchedule();


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
    // GET MOVIE ID
    // =========================================================

    private int getMovieId(
            String movieTitle
    ) {

        String sql =
                "SELECT movie_id "
                        + "FROM movie "
                        + "WHERE title = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    movieTitle
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    return result.getInt(
                            "movie_id"
                    );
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }


        return 0;
    }


    // =========================================================
    // GET HALL ID
    // =========================================================

    private int getHallId(
            String hallName
    ) {

        String sql =
                "SELECT hall_id "
                        + "FROM hall "
                        + "WHERE hall_name = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    hallName
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

        } catch (SQLException e) {

            e.printStackTrace();
        }


        return 0;
    }


    // =========================================================
    // GET HALL CINEMA
    // =========================================================

    private int getHallCinemaId(
            int hallId
    ) {

        String sql =
                "SELECT cinema_id "
                        + "FROM hall "
                        + "WHERE hall_id = ?";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
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

                    return result.getInt(
                            "cinema_id"
                    );
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }


        return 0;
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


            if (employeeName != null) {

                controller.setEmployeeName(
                        employeeName
                );
            }


            Stage stage =
                    (Stage)
                            (
                                    (Node)
                                            btnAdd
                            )
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
    // CINEMA ITEM
    // =========================================================

    public static class CinemaItem {

        private final int cinemaId;

        private final String cinemaName;


        public CinemaItem(
                int cinemaId,
                String cinemaName
        ) {

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
            String message
    ) {

        Alert alert =
                new Alert(type);


        alert.setTitle(
                title
        );


        alert.setHeaderText(null);


        alert.setContentText(
                message
        );


        alert.showAndWait();
    }
}
package cinemasystem.controller;

import cinemasystem.database.DBConnection;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.HBox;

public class AddMovieController {

    @FXML
    private TextField txtTitle;

    @FXML
    private TextArea txtDescription;

    @FXML
    private VBox genreContainer;

    @FXML
    private ComboBox<String> cmbLanguage;

    @FXML
    private TextField txtDuration;

    @FXML
    private ComboBox<String> cmbAgeRating;


    // =========================================================
    // GENRES
    // =========================================================

    private final List<String> genreList = List.of(
            "Action",
            "Adventure",
            "Animation",
            "Comedy",
            "Crime",
            "Drama",
            "Fantasy",
            "Horror",
            "Romance",
            "Science Fiction",
            "Thriller"
    );


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        cmbLanguage.getItems().addAll(
                "Arabic",
                "English",
                "French",
                "Korean",
                "Turkish"
        );

        cmbAgeRating.getItems().addAll(
                "G",
                "PG",
                "PG-13",
                "R",
                "18+"
        );


        /*
         * The first ComboBox already exists
         * inside the FXML.
         *
         * We just initialize its items.
         */

        if (!genreContainer.getChildren().isEmpty()) {

            HBox firstRow =
                    (HBox) genreContainer
                            .getChildren()
                            .get(0);

            ComboBox<String> firstCombo =
                    (ComboBox<String>)
                            firstRow.getChildren().get(0);

            firstCombo.getItems().addAll(
                    genreList
            );
        }
    }


    // =========================================================
    // ADD NEW GENRE
    // =========================================================

    @FXML
    private void handleAddGenre() {

        HBox row =
                createGenreRow();

        genreContainer
                .getChildren()
                .add(row);
    }


    // =========================================================
    // CREATE GENRE ROW
    // =========================================================

    private HBox createGenreRow() {

        HBox row =
                new HBox(8);

        row.setMaxWidth(
                Double.MAX_VALUE
        );


        ComboBox<String> combo =
                new ComboBox<>();

        combo.getItems().addAll(
                genreList
        );

        combo.setPromptText(
                "Select genre"
        );

        combo.setPrefHeight(
                48
        );

        combo.setMaxWidth(
                Double.MAX_VALUE
        );

        combo.setStyle(
                "-fx-background-color:white;" +
                "-fx-background-radius:8;" +
                "-fx-border-color:#CCCCCC;" +
                "-fx-border-radius:8;" +
                "-fx-font-size:14px;"
        );


        HBox.setHgrow(
                combo,
                javafx.scene.layout.Priority.ALWAYS
        );


        Button removeButton =
                new Button("−");

        removeButton.setPrefWidth(
                48
        );

        removeButton.setPrefHeight(
                48
        );

        removeButton.setStyle(
                "-fx-background-color:#D32F2F;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:22px;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:8;" +
                "-fx-cursor:hand;"
        );


        removeButton.setOnAction(
                event ->
                        genreContainer
                                .getChildren()
                                .remove(row)
        );


        row.getChildren().addAll(
                combo,
                removeButton
        );


        return row;
    }


    // =========================================================
    // GET SELECTED GENRES
    // =========================================================

    private String getSelectedGenres() {

        List<String> selectedGenres =
                new ArrayList<>();


        for (
                javafx.scene.Node node :
                genreContainer.getChildren()
        ) {

            if (!(node instanceof HBox)) {
                continue;
            }


            HBox row =
                    (HBox) node;


            if (row.getChildren().isEmpty()) {
                continue;
            }


            if (!(row.getChildren().get(0)
                    instanceof ComboBox)) {

                continue;
            }


            ComboBox<?> combo =
                    (ComboBox<?>)
                            row.getChildren().get(0);


            Object value =
                    combo.getValue();


            if (value != null
                    && !value.toString()
                    .trim()
                    .isEmpty()) {

                String genre =
                        value.toString().trim();


                /*
                 * Prevent duplicate genres.
                 */

                if (!selectedGenres
                        .contains(genre)) {

                    selectedGenres.add(
                            genre
                    );
                }
            }
        }


        return String.join(
                ", ",
                selectedGenres
        );
    }


    // =========================================================
    // SAVE
    // =========================================================

    @FXML
    private void handleSave() {

        String title =
                txtTitle.getText()
                        .trim();

        String description =
                txtDescription.getText()
                        .trim();

        String genre =
                getSelectedGenres();

        String language =
                cmbLanguage.getValue();

        String durationText =
                txtDuration.getText()
                        .trim();

        String ageRating =
                cmbAgeRating.getValue();


        // =====================================================
        // REQUIRED FIELDS
        // =====================================================

        if (title.isEmpty()
                || genre.isEmpty()
                || language == null
                || durationText.isEmpty()
                || ageRating == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Data",
                    "Please fill in all required fields."
            );

            return;
        }


        // =====================================================
        // DURATION
        // =====================================================

        int duration;


        try {

            duration =
                    Integer.parseInt(
                            durationText
                    );


            if (duration <= 0) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Invalid Duration",
                        "Duration must be greater than 0."
                );

                return;
            }


        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Invalid Duration",
                    "Duration must be a number."
            );

            return;
        }


        // =====================================================
        // CHECK DUPLICATE MOVIE
        // =====================================================

        String checkSql =
                "SELECT COUNT(*) " +
                "FROM movie " +
                "WHERE LOWER(title) = LOWER(?)";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement checkStatement =
                        connection.prepareStatement(
                                checkSql
                        )
        ) {

            checkStatement.setString(
                    1,
                    title
            );


            try (
                    ResultSet resultSet =
                            checkStatement
                                    .executeQuery()
            ) {

                if (resultSet.next()) {

                    int count =
                            resultSet.getInt(1);


                    if (count > 0) {

                        showAlert(
                                Alert.AlertType.WARNING,
                                "Movie Already Exists",
                                "A movie with the title \""
                                        + title
                                        + "\" already exists."
                        );

                        return;
                    }
                }
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not check if the movie already exists."
            );

            return;
        }


        // =====================================================
        // INSERT
        // =====================================================

        String sql =
                "INSERT INTO movie " +
                "(title, description, genre, language, " +
                "duration, age_rating) " +
                "VALUES (?, ?, ?, ?, ?, ?)";


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
                    title
            );

            statement.setString(
                    2,
                    description
            );

            statement.setString(
                    3,
                    genre
            );

            statement.setString(
                    4,
                    language
            );

            statement.setInt(
                    5,
                    duration
            );

            statement.setString(
                    6,
                    ageRating
            );


            statement.executeUpdate();


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Movie added successfully."
            );


            closeWindow();


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not add the movie.\n\n"
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void handleCancel() {

        closeWindow();
    }


    // =========================================================
    // CLOSE
    // =========================================================

    private void closeWindow() {

        Stage stage =
                (Stage)
                        txtTitle
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
}
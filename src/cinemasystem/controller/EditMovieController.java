package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Movie;

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
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.layout.HBox;

public class EditMovieController {

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


    private Movie movie;


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
    }


    // =========================================================
    // SET MOVIE
    // =========================================================

    public void setMovie(Movie movie) {

        this.movie = movie;


        txtTitle.setText(
                movie.getTitle()
        );

        txtDescription.setText(
                movie.getDescription()
        );


        cmbLanguage.setValue(
                movie.getLanguage()
        );


        txtDuration.setText(
                String.valueOf(
                        movie.getDuration()
                )
        );


        cmbAgeRating.setValue(
                movie.getAgeRating()
        );


        /*
         * Load all existing genres.
         */

        loadGenres(
                movie.getGenre()
        );
    }


    // =========================================================
    // LOAD GENRES
    // =========================================================

    private void loadGenres(
            String genres) {

        genreContainer
                .getChildren()
                .clear();


        if (genres == null
                || genres.trim().isEmpty()) {

            addGenreRow(null);

            return;
        }


        String[] genreArray =
                genres.split(",");


        for (String genre : genreArray) {

            String value =
                    genre.trim();


            if (!value.isEmpty()) {

                addGenreRow(
                        value
                );
            }
        }


        /*
         * Safety:
         * always keep at least one row.
         */

        if (genreContainer
                .getChildren()
                .isEmpty()) {

            addGenreRow(null);
        }
    }


    // =========================================================
    // ADD GENRE ROW
    // =========================================================

    private void addGenreRow(
            String selectedGenre) {

        HBox row =
                createGenreRow();


        genreContainer
                .getChildren()
                .add(row);


        if (selectedGenre != null) {

            ComboBox<String> combo =
                    (ComboBox<String>)
                            row.getChildren()
                                    .get(0);

            combo.setValue(
                    selectedGenre
            );
        }
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


        Button addButton =
                new Button("+");

        addButton.setPrefWidth(
                48
        );

        addButton.setPrefHeight(
                48
        );

        addButton.setStyle(
                "-fx-background-color:#2E7D32;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:22px;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:8;" +
                "-fx-cursor:hand;"
        );


        addButton.setOnAction(
                event ->
                        addGenreRow(null)
        );


        /*
         * Only rows after the first one
         * should have a remove button.
         */

        if (!genreContainer
                .getChildren()
                .isEmpty()) {

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
                    addButton,
                    removeButton
            );

        } else {

            row.getChildren().addAll(
                    combo,
                    addButton
            );
        }


        return row;
    }


    // =========================================================
    // GET GENRES
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


            if (value != null) {

                String genre =
                        value.toString()
                                .trim();


                if (!genre.isEmpty()
                        && !selectedGenres
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

        if (movie == null) {
            return;
        }


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
        // VALIDATION
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
        // UPDATE
        // =====================================================

        String sql =
                "UPDATE movie SET " +
                "title = ?, " +
                "description = ?, " +
                "genre = ?, " +
                "language = ?, " +
                "duration = ?, " +
                "age_rating = ? " +
                "WHERE movie_id = ?";


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

            statement.setInt(
                    7,
                    movie.getMovieId()
            );


            int affectedRows =
                    statement.executeUpdate();


            if (affectedRows > 0) {

                /*
                 * Update the existing Movie object too.
                 * This is important because your MovieController
                 * is holding this same object.
                 */

                movie.setTitle(title);
                movie.setDescription(description);
                movie.setGenre(genre);
                movie.setLanguage(language);
                movie.setDuration(duration);
                movie.setAgeRating(ageRating);


                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Movie updated successfully."
                );


                closeWindow();


            } else {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Update Movie",
                        "Movie could not be found."
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not update the movie.\n\n"
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
}
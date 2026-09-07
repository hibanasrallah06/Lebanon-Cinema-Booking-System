package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Movie;
import cinemasystem.util.Session;

import javafx.application.Platform;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.Pos;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MovieController {

    // =========================================================
    // TMDB
    // =========================================================

    /*
     * IMPORTANT:
     *
     * Set the TMDB Read Access Token in the TMDB_READ_ACCESS_TOKEN
     * environment variable. Never commit API tokens to Git.
     */

    private static final String TMDB_TOKEN = System.getenv("TMDB_READ_ACCESS_TOKEN");
    private static final String TMDB_API =
            "https://api.themoviedb.org/3";

    private static final String TMDB_IMAGE =
            "https://image.tmdb.org/t/p/w500";


    // =========================================================
    // POSTER CACHE
    // =========================================================

    /*
     * Keeps already found poster paths.
     *
     * This prevents TMDB from being called again every time
     * the movie list is refreshed after Edit.
     */

    private final ConcurrentHashMap<String, String> posterCache =
            new ConcurrentHashMap<>();

    /*
     * Movies that were searched but have no poster.
     * This prevents repeatedly searching TMDB for them.
     */

    private final Set<String> noPosterCache =
            ConcurrentHashMap.newKeySet();


    // =========================================================
    // EMPLOYEE NAME
    // =========================================================

    private String employeeName;

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }


    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private FlowPane movieContainer;

    @FXML
    private TextField txtSearch;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnEdit;

    @FXML
    private Button btnDelete;


    // =========================================================
    // MOVIES
    // =========================================================

    private final ObservableList<Movie> movieList =
            FXCollections.observableArrayList();


    // =========================================================
    // EXECUTOR
    // =========================================================

    private final ExecutorService executor =
            Executors.newCachedThreadPool();


    // =========================================================
    // CARD SIZE
    // =========================================================

    private static final double CARD_WIDTH = 250;

    private static final double CARD_HEIGHT = 560;

    private static final double POSTER_WIDTH = 230;

    private static final double POSTER_HEIGHT = 250;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        applyMoviePermissions();

        loadMovies();

        if (txtSearch != null) {

            txtSearch.textProperty().addListener(
                    (observable, oldValue, newValue) -> {

                        searchMovies(newValue);
                    }
            );
        }
    }


    // =========================================================
    // PERMISSIONS
    // =========================================================

    private void applyMoviePermissions() {

        boolean canModify = canModifyMovies();

        /*
         * Only disable controls if they actually exist
         * in the FXML.
         *
         * This prevents:
         *
         * NullPointerException:
         * btnEdit is null
         */

        if (btnAdd != null) {
            btnAdd.setDisable(!canModify);
        }

        if (btnEdit != null) {
            btnEdit.setDisable(!canModify);
        }

        if (btnDelete != null) {
            btnDelete.setDisable(!canModify);
        }
    }


    // =========================================================
    // LOAD MOVIES
    // =========================================================

    private void loadMovies() {

        movieList.clear();

        String sql =
                "SELECT movie_id, title, description, genre, " +
                "language, duration, age_rating " +
                "FROM movie " +
                "ORDER BY movie_id";


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
                        createMovieFromResult(result);

                movieList.add(movie);
            }

            displayMovies();

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
    // CREATE MOVIE
    // =========================================================

    private Movie createMovieFromResult(ResultSet result)
            throws SQLException {

        return new Movie(
                result.getInt("movie_id"),
                result.getString("title"),
                result.getString("description"),
                result.getString("genre"),
                result.getString("language"),
                result.getInt("duration"),
                result.getString("age_rating")
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void searchMovies(String searchText) {

        String text =
                searchText == null
                ? ""
                : searchText.trim();


        if (text.isEmpty()) {

            loadMovies();

            return;
        }


        movieList.clear();


        String sql =
                "SELECT movie_id, title, description, genre, " +
                "language, duration, age_rating " +
                "FROM movie " +
                "WHERE LOWER(title) LIKE LOWER(?) " +
                "OR LOWER(genre) LIKE LOWER(?) " +
                "ORDER BY movie_id";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            String searchPattern =
                    "%" + text + "%";


            statement.setString(
                    1,
                    searchPattern
            );

            statement.setString(
                    2,
                    searchPattern
            );


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    Movie movie =
                            createMovieFromResult(result);

                    movieList.add(movie);
                }
            }


            displayMovies();


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Search Error",
                    "Could not search for movies.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // DISPLAY MOVIES
    // =========================================================

    private void displayMovies() {

        if (movieContainer == null) {

            System.err.println(
                    "ERROR: movieContainer is null. " +
                    "Check Movie.fxml fx:id=\"movieContainer\"."
            );

            return;
        }


        movieContainer.getChildren().clear();


        for (Movie movie : movieList) {

            VBox card =
                    createMovieCard(movie);

            movieContainer
                    .getChildren()
                    .add(card);


            /*
             * Load poster in background.
             */

            loadPosterForMovie(
                    movie,
                    card
            );
        }
    }


    // =========================================================
    // CREATE MOVIE CARD
    // =========================================================

    private VBox createMovieCard(Movie movie) {

        VBox card = new VBox();

        /*
         * FIX:
         *
         * Every card has exactly the same width and height.
         */

        card.setPrefWidth(CARD_WIDTH);
        card.setMinWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);

        card.setPrefHeight(CARD_HEIGHT);
        card.setMinHeight(CARD_HEIGHT);
        card.setMaxHeight(CARD_HEIGHT);

        card.setSpacing(8);

        card.setPadding(
                new javafx.geometry.Insets(10)
        );

        card.setAlignment(
                Pos.TOP_CENTER
        );

        card.setStyle(
                normalCardStyle()
        );

        card.setUserData(movie);


        // =====================================================
        // POSTER
        // =====================================================

        ImageView poster =
                createPlaceholderPoster();

        card.getChildren().add(poster);


        // =====================================================
        // TITLE
        // =====================================================

        Label title =
                new Label(
                        safe(movie.getTitle())
                );

        title.setWrapText(true);

        title.setMaxWidth(POSTER_WIDTH);

        title.setMinHeight(30);

        title.setAlignment(
                Pos.CENTER
        );

        title.setStyle(
                "-fx-font-size:17px;" +
                "-fx-font-weight:bold;" +
                "-fx-text-fill:#222222;"
        );


        // =====================================================
        // GENRE
        // =====================================================

        Label genre =
                new Label(
                        "Genre: "
                        + safe(movie.getGenre())
                );

        genre.setWrapText(true);

        genre.setMaxWidth(POSTER_WIDTH);

        genre.setAlignment(
                Pos.CENTER
        );

        genre.setStyle(
                "-fx-font-size:13px;" +
                "-fx-text-fill:#555555;"
        );


        // =====================================================
        // LANGUAGE
        // =====================================================

        Label language =
                new Label(
                        "Language: "
                        + safe(movie.getLanguage())
                );

        language.setWrapText(true);

        language.setMaxWidth(POSTER_WIDTH);

        language.setAlignment(
                Pos.CENTER
        );

        language.setStyle(
                "-fx-font-size:13px;" +
                "-fx-text-fill:#555555;"
        );


        // =====================================================
        // DURATION
        // =====================================================

        Label duration =
                new Label(
                        "Duration: "
                        + movie.getDuration()
                        + " min"
                );

        duration.setAlignment(
                Pos.CENTER
        );

        duration.setStyle(
                "-fx-font-size:13px;" +
                "-fx-text-fill:#555555;"
        );


        // =====================================================
        // AGE RATING
        // =====================================================

        Label ageRating =
                new Label(
                        "Rating: "
                        + safe(movie.getAgeRating())
                );

        ageRating.setAlignment(
                Pos.CENTER
        );

        ageRating.setStyle(
                "-fx-font-size:13px;" +
                "-fx-font-weight:bold;" +
                "-fx-text-fill:#2E7D32;"
        );


        card.getChildren().addAll(
                title,
                genre,
                language,
                duration,
                ageRating
        );


        // =====================================================
        // DESCRIPTION
        // =====================================================

        String description =
                safe(movie.getDescription());


        if (!description.isEmpty()) {

            Label desc =
                    new Label(description);

            desc.setWrapText(true);

            desc.setMaxWidth(
                    POSTER_WIDTH
            );

            desc.setMaxHeight(90);

            desc.setAlignment(
                    Pos.TOP_LEFT
            );

            desc.setStyle(
                    "-fx-font-size:12px;" +
                    "-fx-text-fill:#777777;"
            );

            card.getChildren().add(desc);
        }


        // =====================================================
        // SPACER
        // =====================================================

        /*
         * IMPORTANT:
         *
         * This pushes the buttons to the bottom
         * of every card.
         */

        Region spacer =
                new Region();

        VBox.setVgrow(
                spacer,
                javafx.scene.layout.Priority.ALWAYS
        );

        card.getChildren().add(
                spacer
        );


        // =====================================================
        // EDIT + DELETE BUTTONS
        // =====================================================

        HBox actionButtons =
                new HBox(8);

        actionButtons.setAlignment(
                Pos.CENTER
        );


        Button editButton =
                new Button("Edit");

        Button deleteButton =
                new Button("Delete");


        editButton.setPrefWidth(105);

        editButton.setPrefHeight(38);


        deleteButton.setPrefWidth(105);

        deleteButton.setPrefHeight(38);


        editButton.setStyle(
                "-fx-background-color:#757575;" +
                "-fx-text-fill:white;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:6;" +
                "-fx-cursor:hand;"
        );


        deleteButton.setStyle(
                "-fx-background-color:#D32F2F;" +
                "-fx-text-fill:white;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:6;" +
                "-fx-cursor:hand;"
        );


        // =====================================================
        // EDIT BUTTON
        // =====================================================

        editButton.setOnAction(event -> {

            if (!canModifyMovies()) {

                showAccessDenied();

                return;
            }

            openEditMovie(movie);
        });


        // =====================================================
        // DELETE BUTTON
        // =====================================================

        deleteButton.setOnAction(event -> {

            if (!canModifyMovies()) {

                showAccessDenied();

                return;
            }

            deleteMovie(movie);
        });


        actionButtons.getChildren().addAll(
                editButton,
                deleteButton
        );


        /*
         * IMPORTANT:
         *
         * Buttons are added LAST.
         * Therefore they stay at the bottom.
         */

        card.getChildren().add(
                actionButtons
        );


        // =====================================================
        // CARD SELECTION
        // =====================================================

        makeCardSelectable(
                card,
                movie
        );


        return card;
    }


    // =========================================================
    // PLACEHOLDER POSTER
    // =========================================================

    private ImageView createPlaceholderPoster() {

        ImageView imageView =
                new ImageView();

        imageView.setFitWidth(
                POSTER_WIDTH
        );

        imageView.setFitHeight(
                POSTER_HEIGHT
        );

        imageView.setPreserveRatio(
                false
        );


        String svg =
                "<svg xmlns='http://www.w3.org/2000/svg' "
                + "width='230' height='250'>"

                + "<rect width='230' height='250' "
                + "fill='%23222222'/>"

                + "<text x='115' y='120' "
                + "text-anchor='middle' "
                + "fill='white' "
                + "font-size='18'>"

                + "MOVIE"

                + "</text>"

                + "<text x='115' y='145' "
                + "text-anchor='middle' "
                + "fill='%23AAAAAA' "
                + "font-size='12'>"

                + "Loading poster..."

                + "</text>"

                + "</svg>";


        try {

            String encoded =
                    java.net.URLEncoder
                            .encode(
                                    svg,
                                    StandardCharsets.UTF_8
                            )
                            .replace(
                                    "+",
                                    "%20"
                            );


            Image image =
                    new Image(
                            "data:image/svg+xml,"
                            + encoded,
                            false
                    );


            imageView.setImage(
                    image
            );


        } catch (Exception e) {

            e.printStackTrace();
        }


        return imageView;
    }


    // =========================================================
    // LOAD POSTER FROM TMDB
    // =========================================================

    private void loadPosterForMovie(
            Movie movie,
            VBox card) {


        if (!isTMDBConfigured()) {

            return;
        }


        String title =
                safe(movie.getTitle()).trim();


        if (title.isEmpty()) {

            return;
        }


        // =====================================================
        // CACHE
        // =====================================================

        String cachedPoster =
                posterCache.get(title);


        if (cachedPoster != null
                && !cachedPoster.isBlank()) {

            setPosterOnCard(
                    card,
                    cachedPoster
            );

            movie.setPosterPath(
                    cachedPoster
            );

            return;
        }


        /*
         * Don't search again if we already know
         * this movie has no poster.
         */

        if (noPosterCache.contains(title)) {

            return;
        }


        // =====================================================
        // BACKGROUND REQUEST
        // =====================================================

        executor.submit(() -> {

            try {

                String posterPath =
                        searchTMDBPoster(title);


                if (posterPath == null
                        || posterPath.isBlank()) {

                    noPosterCache.add(title);

                    return;
                }


                /*
                 * Save result in cache.
                 */

                posterCache.put(
                        title,
                        posterPath
                );


                movie.setPosterPath(
                        posterPath
                );


                Platform.runLater(() -> {

                    setPosterOnCard(
                            card,
                            posterPath
                    );
                });


            } catch (Exception e) {

                System.err.println(
                        "Could not load TMDB poster for: "
                        + title
                );

                System.err.println(
                        e.getMessage()
                );
            }
        });
    }


    // =========================================================
    // SET POSTER ON CARD
    // =========================================================

    private void setPosterOnCard(
            VBox card,
            String posterPath) {


        if (posterPath == null
                || posterPath.isBlank()) {

            return;
        }


        Platform.runLater(() -> {

            if (card == null) {
                return;
            }


            if (card.getChildren().isEmpty()) {
                return;
            }


            if (!(card.getChildren().get(0)
                    instanceof ImageView)) {

                return;
            }


            ImageView poster =
                    (ImageView)
                    card.getChildren().get(0);


            try {

                Image image =
                        new Image(
                                TMDB_IMAGE
                                + posterPath,
                                POSTER_WIDTH,
                                POSTER_HEIGHT,
                                false,
                                true,
                                true
                        );


                poster.setImage(
                        image
                );


            } catch (Exception e) {

                e.printStackTrace();
            }
        });
    }


    // =========================================================
    // CHECK TMDB
    // =========================================================

    private boolean isTMDBConfigured() {

    return TMDB_TOKEN != null
            && !TMDB_TOKEN.isBlank();
}


    // =========================================================
    // SEARCH TMDB
    // =========================================================

    private String searchTMDBPoster(
            String movieTitle)
            throws Exception {


        String[] languages = {

            "en-US",
            "ar-LB",
            "fr-FR"
        };


        for (String language : languages) {

            String poster =
                    searchTMDBPoster(
                            movieTitle,
                            language
                    );


            if (poster != null
                    && !poster.isBlank()) {

                return poster;
            }
        }


        return null;
    }


    // =========================================================
    // SEARCH TMDB WITH LANGUAGE
    // =========================================================

    private String searchTMDBPoster(
            String movieTitle,
            String language)
            throws Exception {


        String encodedTitle =
                java.net.URLEncoder.encode(
                        movieTitle,
                        StandardCharsets.UTF_8
                );


        String endpoint =
                TMDB_API
                + "/search/movie?query="
                + encodedTitle
                + "&include_adult=false"
                + "&language="
                + language
                + "&page=1";


        URL url =
                URI.create(
                        endpoint
                ).toURL();


        HttpURLConnection connection =
                (HttpURLConnection)
                url.openConnection();


        connection.setRequestMethod(
                "GET"
        );


        connection.setRequestProperty(
                "Authorization",
                "Bearer " + TMDB_TOKEN
        );


        connection.setRequestProperty(
                "accept",
                "application/json"
        );


        connection.setConnectTimeout(
                20000
        );

        connection.setReadTimeout(
                20000
        );


        int responseCode =
                connection.getResponseCode();


        if (responseCode != 200) {

            System.err.println(
                    "TMDB HTTP error: "
                    + responseCode
            );

            connection.disconnect();

            return null;
        }


        StringBuilder response =
                new StringBuilder();


        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection
                                                .getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;


            while (
                    (line =
                            reader.readLine())
                            != null
            ) {

                response.append(
                        line
                );
            }
        }


        connection.disconnect();


        return extractFirstPosterPath(
                response.toString()
        );
    }


    // =========================================================
    // EXTRACT POSTER PATH
    // =========================================================

    private String extractFirstPosterPath(
            String json) {


        Pattern pattern =
                Pattern.compile(
                        "\"poster_path\"\\s*:\\s*\"([^\"]+)\""
                );


        Matcher matcher =
                pattern.matcher(
                        json
                );


        while (matcher.find()) {

            String path =
                    matcher.group(1);


            if (path != null
                    && !path.isBlank()
                    && !path.equals("null")) {

                return path;
            }
        }


        return null;
    }


    // =========================================================
    // ADD MOVIE
    // =========================================================

    @FXML
    private void handleAdd() {

        if (!canModifyMovies()) {

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


           Stage stage = new Stage();

stage.setTitle("Add Movie");
stage.setScene(new Scene(root));

stage.getIcons().add(
    new javafx.scene.image.Image(
        getClass().getResourceAsStream("/cinemasystem/images/icon.png")
    )
);

stage.showAndWait();


            /*
             * Refresh list.
             */

            loadMovies();


            if (txtSearch != null) {
                txtSearch.clear();
            }


        } catch (Exception e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Add Movie window.\n\n"
                    + e.getMessage()
            );
        }
    }

    // =========================================================
    // OPEN EDIT MOVIE
    // =========================================================

    private void openEditMovie(
            Movie movie) {


        try {

            String oldTitle =
                    safe(movie.getTitle()).trim();


            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/EditMovie.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            EditMovieController controller =
                    loader.getController();


            controller.setMovie(
                    movie
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Edit Movie"
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


            /*
             * If the title changed,
             * remove the old title from cache.
             */

            String newTitle =
                    safe(movie.getTitle()).trim();


            if (!oldTitle.equalsIgnoreCase(
                    newTitle
            )) {

                posterCache.remove(
                        oldTitle
                );

                noPosterCache.remove(
                        oldTitle
                );
            }


            /*
             * Reload movies.
             *
             * Cached posters will appear immediately.
             */

            loadMovies();


            if (txtSearch != null) {
                txtSearch.clear();
            }


        } catch (Exception e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Edit Movie window.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // DELETE MOVIE FROM CARD
    // =========================================================

    private void deleteMovie(
            Movie selectedMovie) {


        if (selectedMovie == null) {
            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Delete Movie"
        );


        confirmation.setHeaderText(
                null
        );


        confirmation.setContentText(
                "Are you sure you want to delete \""
                + selectedMovie.getTitle()
                + "\"?"
        );


        Optional<ButtonType> result =
                confirmation.showAndWait();


        if (result.isEmpty()
                || result.get()
                != ButtonType.OK) {

            return;
        }


        Connection connection =
                null;


        try {

            connection =
                    DBConnection.getConnection();


            /*
             * Use transaction so both deletions
             * succeed together.
             */

            connection.setAutoCommit(
                    false
            );


            // =================================================
            // 1. DELETE movie_show RECORDS
            // =================================================

            String deleteShowsSql =
                    "DELETE FROM movie_show "
                    + "WHERE movie_id = ?";


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    deleteShowsSql
                            )
            ) {

                statement.setInt(
                        1,
                        selectedMovie.getMovieId()
                );


                statement.executeUpdate();
            }


            // =================================================
            // 2. DELETE MOVIE
            // =================================================

            String deleteMovieSql =
                    "DELETE FROM movie "
                    + "WHERE movie_id = ?";


            int affectedRows;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    deleteMovieSql
                            )
            ) {

                statement.setInt(
                        1,
                        selectedMovie.getMovieId()
                );


                affectedRows =
                        statement.executeUpdate();
            }


            if (affectedRows > 0) {

                connection.commit();


                /*
                 * Remove from cache.
                 */

                String title =
                        safe(
                                selectedMovie
                                        .getTitle()
                        ).trim();


                posterCache.remove(
                        title
                );


                noPosterCache.remove(
                        title
                );


                movieList.remove(
                        selectedMovie
                );


                displayMovies();


                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Movie deleted successfully."
                );


            } else {

                connection.rollback();


                showAlert(
                        Alert.AlertType.WARNING,
                        "Delete Movie",
                        "Movie could not be found."
                );
            }


        } catch (SQLException e) {

            /*
             * Rollback if something fails.
             */

            if (connection != null) {

                try {

                    connection.rollback();

                } catch (SQLException rollbackError) {

                    rollbackError.printStackTrace();
                }
            }


            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not delete the movie.\n\n"
                    + e.getMessage()
            );


        } finally {

            if (connection != null) {

                try {

                    connection.setAutoCommit(
                            true
                    );

                    connection.close();

                } catch (SQLException closeError) {

                    closeError.printStackTrace();
                }
            }
        }
    }


    // =========================================================
    // DELETE MOVIE - TOP BUTTON
    // =========================================================

    @FXML
    private void handleDelete() {

        if (!canModifyMovies()) {

            showAccessDenied();

            return;
        }


        Movie selectedMovie =
                getSelectedMovie();


        if (selectedMovie == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Movie Selected",
                    "Please click a movie card first."
            );

            return;
        }


        /*
         * Use exactly the same delete method
         * as the card Delete button.
         */

        deleteMovie(
                selectedMovie
        );
    }


    // =========================================================
    // GET SELECTED MOVIE
    // =========================================================

    private Movie getSelectedMovie() {

        if (movieContainer == null) {
            return null;
        }


        for (
                javafx.scene.Node node :
                movieContainer.getChildren()
        ) {

            if (node instanceof VBox) {

                VBox card =
                        (VBox) node;


                Object data =
                        card.getUserData();


                if (data instanceof Movie) {

                    String style =
                            card.getStyle();


                    if (style.contains(
                            "#2E7D32"
                    )) {

                        return (Movie) data;
                    }
                }
            }
        }


        return null;
    }


    // =========================================================
    // CARD SELECTION
    // =========================================================

    private void makeCardSelectable(
            VBox card,
            Movie movie) {


        card.setUserData(
                movie
        );


        card.setOnMouseClicked(
                event -> {


                    /*
                     * Remove selection from
                     * all cards.
                     */

                    for (
                            javafx.scene.Node node :
                            movieContainer
                                    .getChildren()
                    ) {

                        if (node instanceof VBox) {

                            VBox otherCard =
                                    (VBox) node;


                            otherCard.setStyle(
                                    normalCardStyle()
                            );
                        }
                    }


                    /*
                     * Select this card.
                     */

                    card.setStyle(
                            selectedCardStyle()
                    );
                }
        );
    }


    // =========================================================
    // CARD STYLES
    // =========================================================

    private String normalCardStyle() {

        return
                "-fx-background-color:white;"
                + "-fx-background-radius:12;"
                + "-fx-border-radius:12;"
                + "-fx-border-color:#DDDDDD;"
                + "-fx-border-width:1;"
                + "-fx-padding:10;"
                + "-fx-effect:"
                + "dropshadow(gaussian,#00000022,8,0,0,3);";
    }


    private String selectedCardStyle() {

        return
                "-fx-background-color:white;"
                + "-fx-background-radius:12;"
                + "-fx-border-radius:12;"
                + "-fx-border-color:#2E7D32;"
                + "-fx-border-width:3;"
                + "-fx-padding:10;"
                + "-fx-effect:"
                + "dropshadow(gaussian,#00000044,10,0,0,4);";
    }


    // =========================================================
    // MODIFY PERMISSION
    // =========================================================

    private boolean canModifyMovies() {

        String role =
                Session.getRole();


        if (role == null) {

            return false;
        }


        role =
                role.trim()
                        .toLowerCase();


        return role.equals("admin")
                || role.equals("manager");
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
                    movieContainer
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
                    "Could not return to Dashboard.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // ACCESS DENIED
    // =========================================================

    private void showAccessDenied() {

        showAlert(
                Alert.AlertType.WARNING,
                "Access Denied",
                "Employees are not allowed to add, edit, or delete movies."
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


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value) {

        if (value == null) {

            return "";
        }


        return value;
    }
}
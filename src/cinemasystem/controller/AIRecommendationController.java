package cinemasystem.controller;

import cinemasystem.dao.MovieDAO;
import cinemasystem.model.Movie;
import cinemasystem.service.AIRecommendationService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.Pos;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;


public class AIRecommendationController {

    private String employeeName;


    // =========================================================
    // EMPLOYEE
    // =========================================================

    public void setEmployeeName(
            String employeeName) {

        this.employeeName =
                employeeName;
    }


    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private TextArea txtRequest;

    @FXML
    private Button btnRecommend;

    @FXML
    private Label lblStatus;

    @FXML
    private FlowPane movieContainer;

    @FXML
    private VBox emptyBox;


    // =========================================================
    // SERVICES
    // =========================================================

    private final MovieDAO movieDAO =
            new MovieDAO();


    /*
     * FREE LOCAL AI
     *
     * Uses Ollama.
     * No OpenAI API.
     * No OPENAI_API_KEY.
     * No payment.
     */
    private final AIRecommendationService
            aiRecommendationService =
            new AIRecommendationService();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        movieContainer.setHgap(20);

        movieContainer.setVgap(20);

        movieContainer.setAlignment(
                Pos.TOP_LEFT
        );


        emptyBox.setVisible(true);

        emptyBox.setManaged(true);


        lblStatus.setText("");


        /*
         * Responsive width.
         */
        movieContainer.widthProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) -> {

                            updateResponsiveWidths();
                        }
                );
    }
    
    


    // =========================================================
    // RECOMMENDATION
    // =========================================================

    @FXML
    private void handleRecommendation() {

        String request =
                txtRequest.getText();


        // =====================================================
        // VALIDATION
        // =====================================================

        if (request == null ||
                request.trim().isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Request",
                    "Please describe what type of movie you want."
            );

            return;
        }


        try {

            // =================================================
            // DISABLE BUTTON
            // =================================================

            btnRecommend.setDisable(true);


            lblStatus.setText(
                    "🤖 AI is analyzing your request..."
            );


            // =================================================
            // CLEAR OLD RESULTS
            // =================================================

            movieContainer
                    .getChildren()
                    .clear();


            hideEmptyBox();


            // =================================================
            // DATABASE
            // =================================================

            List<Movie> movies =
                    movieDAO.getAllMovies();


            if (movies == null ||
                    movies.isEmpty()) {

                lblStatus.setText(
                        "No movies are available in the database."
                );

                showEmptyBox();

                return;
            }


            // =================================================
            // LOCAL AI
            // =================================================

            List<String> recommendationIds =
                    aiRecommendationService
                            .getRecommendationIds(
                                    request,
                                    movies      
                            );


            // =================================================
            // NO MATCH
            // =================================================

            if (recommendationIds == null ||
                    recommendationIds.isEmpty()) {

                lblStatus.setText(
                        "No matching movies were found."
                );

                showNoResultsCard();

                return;
            }


            // =================================================
            // FIND MOVIES
            // =================================================

            List<Movie> recommendations =
                    findMoviesByIds(
                            recommendationIds,
                            movies
                    );


            if (recommendations.isEmpty()) {

                lblStatus.setText(
                        "No matching movies were found."
                );

                showNoResultsCard();

                return;
            }


            // =================================================
            // AI HEADER
            // =================================================

            VBox aiHeader =
                    createAIHeader(
                            request
                    );


            movieContainer
                    .getChildren()
                    .add(aiHeader);


            // =================================================
            // MOVIE CARDS
            // =================================================

            for (Movie movie :
                    recommendations) {

                VBox movieCard =
                        createMovieCard(
                                movie
                        );


                movieContainer
                        .getChildren()
                        .add(movieCard);
            }


            // =================================================
            // RESPONSIVE UPDATE
            // =================================================

            updateResponsiveWidths();


            // =================================================
            // SUCCESS
            // =================================================

            lblStatus.setText(
                    "✨ AI found "
                    + recommendations.size()
                    + " recommendation"
                    + (
                            recommendations.size() == 1
                                    ? ""
                                    : "s"
                    )
                    + " for you!"
            );


        } catch (Exception e) {

            e.printStackTrace();


            lblStatus.setText(
                    "AI could not generate recommendations."
            );


            showAlert(
                    Alert.AlertType.ERROR,
                    "AI Recommendation Error",
                    "Could not generate movie recommendations.\n\n"
                    + e.getMessage()
            );


            showEmptyBox();


        } finally {

            btnRecommend.setDisable(false);
        }
    }


    // =========================================================
    // FIND MOVIES BY IDS
    // =========================================================

    private List<Movie> findMoviesByIds(
            List<String> ids,
            List<Movie> movies) {


        List<Movie> result =
                new ArrayList<>();


        for (String id :
                ids) {


            if (id == null) {
                continue;
            }


            String cleanId =
                    id.trim();


            for (Movie movie :
                    movies) {


                String movieId =
                        String.valueOf(
                                movie.getMovieId()
                        );


                if (movieId.equals(
                        cleanId
                )) {


                    result.add(
                            movie
                    );


                    break;
                }
            }
        }


        return result;
    }


    // =========================================================
    // AI HEADER
    // =========================================================

    private VBox createAIHeader(
            String request) {


        VBox container =
                new VBox();


        container.setSpacing(12);


        /*
         * No fixed 900px.
         */
        container.setPrefWidth(600);

        container.setMinWidth(0);

        container.setMaxWidth(600);


        container.setStyle(
                "-fx-background-color:white;" +
                "-fx-background-radius:18;" +
                "-fx-padding:22;" +
                "-fx-effect:" +
                "dropshadow(" +
                "gaussian," +
                "rgba(0,0,0,0.08)," +
                "10,0,0,3);"
        );


        // =====================================================
        // HEADER ROW
        // =====================================================

        HBox header =
                new HBox();


        header.setSpacing(12);


        header.setAlignment(
                Pos.CENTER_LEFT
        );


        // =====================================================
        // ROBOT
        // =====================================================

        Label icon =
                new Label("🤖");


        icon.setMinWidth(52);

        icon.setMinHeight(52);

        icon.setPrefWidth(52);

        icon.setPrefHeight(52);


        icon.setAlignment(
                Pos.CENTER
        );


        icon.setStyle(
                "-fx-background-color:#FFF0B8;" +
                "-fx-background-radius:15;" +
                "-fx-font-size:23px;"
        );


        // =====================================================
        // TITLE BOX
        // =====================================================

        VBox titleBox =
                new VBox();


        titleBox.setSpacing(3);


        Label title =
                new Label(
                        "AI Movie Recommendation"
                );


        title.setWrapText(true);


        title.setStyle(
                "-fx-text-fill:#080808;" +
                "-fx-font-size:20px;" +
                "-fx-font-weight:bold;"
        );


        Label subtitle =
                new Label(
                        "Powered by Local AI"
                );


        subtitle.setStyle(
                "-fx-text-fill:#718096;" +
                "-fx-font-size:13px;"
        );


        titleBox.getChildren().addAll(
                title,
                subtitle
        );


        HBox.setHgrow(
                titleBox,
                Priority.ALWAYS
        );


        header.getChildren().addAll(
                icon,
                titleBox
        );


        // =====================================================
        // REQUEST
        // =====================================================

        Label requestLabel =
                new Label(
                        "Your request: \""
                        + request.trim()
                        + "\""
                );


        requestLabel.setWrapText(true);


        requestLabel.setMaxWidth(
                Double.MAX_VALUE
        );


        requestLabel.setStyle(
                "-fx-background-color:#F5F6F8;" +
                "-fx-background-radius:10;" +
                "-fx-padding:12;" +
                "-fx-text-fill:#475569;" +
                "-fx-font-size:14px;" +
                "-fx-font-weight:bold;"
        );


        container.getChildren().addAll(
                header,
                requestLabel
        );


        return container;
    }


    // =========================================================
    // MOVIE CARD
    // =========================================================

    private VBox createMovieCard(
            Movie movie) {


        VBox card =
                new VBox();


        card.setSpacing(12);


        card.setPrefWidth(280);

        card.setMinWidth(240);

        card.setMaxWidth(280);


        card.setPrefHeight(275);


        card.setStyle(
                "-fx-background-color:white;" +
                "-fx-background-radius:18;" +
                "-fx-padding:22;" +
                "-fx-effect:" +
                "dropshadow(" +
                "gaussian," +
                "rgba(0,0,0,0.10)," +
                "12,0,0,4);"
        );


        // =====================================================
        // ICON
        // =====================================================

        Label icon =
                new Label("🎬");


        icon.setMinWidth(52);

        icon.setMinHeight(52);

        icon.setPrefWidth(52);

        icon.setPrefHeight(52);


        icon.setAlignment(
                Pos.CENTER
        );


        icon.setStyle(
                "-fx-background-color:#FFF0B8;" +
                "-fx-background-radius:15;" +
                "-fx-font-size:23px;"
        );


        // =====================================================
        // TITLE
        // =====================================================

        Label title =
                new Label(
                        safe(
                                movie.getTitle()
                        )
                );


        title.setWrapText(true);


        title.setMaxWidth(
                Double.MAX_VALUE
        );


        title.setStyle(
                "-fx-text-fill:#080808;" +
                "-fx-font-size:19px;" +
                "-fx-font-weight:bold;"
        );


        // =====================================================
        // TOP ROW
        // =====================================================

        HBox topRow =
                new HBox();


        topRow.setSpacing(12);


        topRow.setAlignment(
                Pos.CENTER_LEFT
        );


        HBox.setHgrow(
                title,
                Priority.ALWAYS
        );


        topRow.getChildren().addAll(
                icon,
                title
        );


        // =====================================================
        // GENRE
        // =====================================================

        Label genre =
                new Label(
                        "🎭 "
                        + safe(
                                movie.getGenre()
                        )
                );


        genre.setWrapText(true);

        genre.setMaxWidth(
                Double.MAX_VALUE
        );


        genre.setStyle(
                "-fx-text-fill:#64748B;" +
                "-fx-font-size:14px;" +
                "-fx-font-weight:bold;"
        );


        // =====================================================
        // LANGUAGE
        // =====================================================

        Label language =
                new Label(
                        "🌐 "
                        + safe(
                                movie.getLanguage()
                        )
                );


        language.setWrapText(true);

        language.setMaxWidth(
                Double.MAX_VALUE
        );


        language.setStyle(
                "-fx-text-fill:#64748B;" +
                "-fx-font-size:13px;"
        );


        // =====================================================
        // DURATION
        // =====================================================

        Label duration =
                new Label(
                        "⏱ "
                        + movie.getDuration()
                        + " minutes"
                );


        duration.setWrapText(true);

        duration.setMaxWidth(
                Double.MAX_VALUE
        );


        duration.setStyle(
                "-fx-text-fill:#64748B;" +
                "-fx-font-size:13px;"
        );


        // =====================================================
        // AGE
        // =====================================================

        Label ageRating =
                new Label(
                        "🔞 "
                        + safe(
                                movie.getAgeRating()
                        )
                );


        ageRating.setWrapText(true);

        ageRating.setMaxWidth(
                Double.MAX_VALUE
        );


        ageRating.setStyle(
                "-fx-text-fill:#64748B;" +
                "-fx-font-size:13px;"
        );


        // =====================================================
        // DESCRIPTION
        // =====================================================

        String description =
                safe(
                        movie.getDescription()
                );


        if (description.length() > 110) {

            description =
                    description.substring(
                            0,
                            110
                    )
                    + "...";
        }


        Label descriptionLabel =
                new Label(
                        description
                );


        descriptionLabel.setWrapText(true);


        descriptionLabel.setMaxWidth(
                Double.MAX_VALUE
        );


        descriptionLabel.setStyle(
                "-fx-text-fill:#718096;" +
                "-fx-font-size:12px;"
        );


        // =====================================================
        // BADGE
        // =====================================================

        Label badge =
                new Label(
                        "✨ Recommended for you"
                );


        badge.setWrapText(true);


        badge.setStyle(
                "-fx-background-color:#FFF0B8;" +
                "-fx-background-radius:8;" +
                "-fx-padding:6 10 6 10;" +
                "-fx-text-fill:#7A5B00;" +
                "-fx-font-size:11px;" +
                "-fx-font-weight:bold;"
        );


        // =====================================================
        // ADD
        // =====================================================

        card.getChildren().addAll(
                topRow,
                badge,
                genre,
                language,
                duration,
                ageRating,
                descriptionLabel
        );


        return card;
    }


    // =========================================================
    // NO RESULTS
    // =========================================================

    private void showNoResultsCard() {


        VBox card =
                new VBox();


        card.setSpacing(12);


        card.setAlignment(
                Pos.CENTER
        );


        card.setPrefWidth(600);

        card.setMinWidth(0);

        card.setMaxWidth(600);


        card.setMinHeight(240);

        card.setPrefHeight(240);


        card.setStyle(
                "-fx-background-color:white;" +
                "-fx-background-radius:18;" +
                "-fx-padding:30;" +
                "-fx-effect:" +
                "dropshadow(" +
                "gaussian," +
                "rgba(0,0,0,0.08)," +
                "10,0,0,3);"
        );


        Label icon =
                new Label("🤖");


        icon.setStyle(
                "-fx-font-size:35px;"
        );


        Label title =
                new Label(
                        "No matching movies found"
                );


        title.setWrapText(true);


        title.setAlignment(
                Pos.CENTER
        );


        title.setStyle(
                "-fx-text-fill:#080808;" +
                "-fx-font-size:20px;" +
                "-fx-font-weight:bold;"
        );


        Label message =
                new Label(
                        "Try describing another genre, "
                        + "language, duration, or movie preference."
                );


        message.setWrapText(true);


        message.setMaxWidth(
                500
        );


        message.setAlignment(
                Pos.CENTER
        );


        message.setStyle(
                "-fx-text-fill:#718096;" +
                "-fx-font-size:14px;"
        );


        card.getChildren().addAll(
                icon,
                title,
                message
        );


        movieContainer
                .getChildren()
                .add(card);


        updateResponsiveWidths();
    }


    // =========================================================
    // RESPONSIVE WIDTHS
    // =========================================================

    private void updateResponsiveWidths() {


        if (movieContainer == null) {
            return;
        }


        double width =
                movieContainer.getWidth();


        if (width <= 0) {
            return;
        }


        /*
         * Keep some space for scrollbar
         * and FlowPane padding.
         */
        double availableWidth =
                width - 30;


        if (availableWidth < 240) {

            availableWidth = 240;
        }


        for (Node node :
                movieContainer.getChildren()) {


            if (!(node instanceof VBox)) {
                continue;
            }


            VBox box =
                    (VBox) node;


            // =================================================
            // AI HEADER
            // =================================================

            if (isAIHeader(box)) {


                box.setPrefWidth(
                        availableWidth
                );


                box.setMaxWidth(
                        availableWidth
                );


                continue;
            }


            // =================================================
            // NO RESULTS
            // =================================================

            if (isNoResultsCard(box)) {


                box.setPrefWidth(
                        availableWidth
                );


                box.setMaxWidth(
                        availableWidth
                );


                continue;
            }


            // =================================================
            // MOVIE CARD
            // =================================================

            if (isMovieCard(box)) {


                double cardWidth;


                if (availableWidth >= 600) {

                    cardWidth = 280;

                } else {

                    cardWidth =
                            Math.max(
                                    240,
                                    availableWidth
                            );
                }


                box.setPrefWidth(
                        cardWidth
                );


                box.setMinWidth(
                        cardWidth
                );


                box.setMaxWidth(
                        cardWidth
                );
            }
        }
    }


    // =========================================================
    // IDENTIFY AI HEADER
    // =========================================================

    private boolean isAIHeader(
            VBox box) {


        for (Node node :
                box.getChildren()) {


            if (node instanceof HBox) {


                HBox row =
                        (HBox) node;


                for (Node child :
                        row.getChildren()) {


                    if (child instanceof Label) {


                        Label label =
                                (Label) child;


                        if ("🤖".equals(
                                label.getText()
                        )) {

                            return true;
                        }
                    }
                }
            }
        }


        return false;
    }


    // =========================================================
    // IDENTIFY NO RESULTS
    // =========================================================

    private boolean isNoResultsCard(
            VBox box) {


        for (Node node :
                box.getChildren()) {


            if (node instanceof Label) {


                Label label =
                        (Label) node;


                if ("No matching movies found"
                        .equals(
                                label.getText()
                        )) {

                    return true;
                }
            }
        }


        return false;
    }


    // =========================================================
    // IDENTIFY MOVIE CARD
    // =========================================================

    private boolean isMovieCard(
            VBox box) {


        return box.getChildren().size() >= 6;
    }


    // =========================================================
    // EMPTY BOX
    // =========================================================

    private void showEmptyBox() {

        emptyBox.setVisible(true);

        emptyBox.setManaged(true);
    }


    private void hideEmptyBox() {

        emptyBox.setVisible(false);

        emptyBox.setManaged(false);
    }


    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            String value) {


        if (value == null ||
                value.trim().isEmpty()) {

            return "Not specified";
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
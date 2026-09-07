package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Cinema;
import cinemasystem.util.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Optional;


public class CinemaController {

    // =========================================================
    // EMPLOYEE NAME
    // =========================================================

    private String employeeName;


    public void setEmployeeName(String employeeName) {

        this.employeeName = employeeName;
    }


    // =========================================================
    // CINEMA TABLE
    // =========================================================

    @FXML
    private TableView<Cinema> tblCinemas;

    @FXML
    private TableColumn<Cinema, Integer> colCinemaId;

    @FXML
    private TableColumn<Cinema, String> colCinemaName;

    @FXML
    private TableColumn<Cinema, String> colAddress;

    @FXML
    private TableColumn<Cinema, String> colPhone;

    @FXML
    private TableColumn<Cinema, String> colCity;


    // =========================================================
    // HALL TABLE
    // =========================================================

    @FXML
    private TableView<HallItem> tblHalls;

    @FXML
    private TableColumn<HallItem, Integer> colHallId;

    @FXML
    private TableColumn<HallItem, String> colHallName;

    @FXML
    private TableColumn<HallItem, Integer> colCapacity;


    // =========================================================
    // LISTS
    // =========================================================

    private final ObservableList<Cinema> cinemaList =
            FXCollections.observableArrayList();

    private final ObservableList<HallItem> hallList =
            FXCollections.observableArrayList();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        // =====================================================
        // CINEMA COLUMNS
        // =====================================================

        colCinemaId.setCellValueFactory(
                new PropertyValueFactory<>("cinemaId")
        );

        colCinemaName.setCellValueFactory(
                new PropertyValueFactory<>("cinemaName")
        );

        colAddress.setCellValueFactory(
                new PropertyValueFactory<>("address")
        );

        colPhone.setCellValueFactory(
                new PropertyValueFactory<>("phone")
        );

        colCity.setCellValueFactory(
                new PropertyValueFactory<>("cityName")
        );


        // =====================================================
        // HALL COLUMNS
        // =====================================================

        colHallId.setCellValueFactory(
                new PropertyValueFactory<>("hallId")
        );

        colHallName.setCellValueFactory(
                new PropertyValueFactory<>("hallName")
        );

        colCapacity.setCellValueFactory(
                new PropertyValueFactory<>("capacity")
        );


        // =====================================================
        // TABLE ITEMS
        // =====================================================

        tblCinemas.setItems(cinemaList);

        tblHalls.setItems(hallList);


        // =====================================================
        // LOAD CINEMAS
        // =====================================================

        loadCinemas();
    }


    // =========================================================
// LOAD CINEMAS
// =========================================================

public void loadCinemas() {

    cinemaList.clear();

    hallList.clear();

    String role = Session.getRole();

    if (role == null) {
        role = "";
    }

    role = role.trim().toLowerCase();

    String sql;

    // =====================================================
    // ADMIN
    // =====================================================

    if (role.equals("admin")) {

        sql =
                "SELECT c.cinema_id, " +
                "c.cinema_name, " +
                "c.address, " +
                "c.phone, " +
                "c.city_id, " +
                "ci.city_name " +
                "FROM cinema c " +
                "INNER JOIN city ci " +
                "ON c.city_id = ci.city_id " +
                "ORDER BY c.cinema_id";
    }

    // =====================================================
    // MANAGER
    // =====================================================

    else if (role.equals("manager")) {

        /*
         * Manager sees ONLY his cinema.
         */

        sql =
                "SELECT c.cinema_id, " +
                "c.cinema_name, " +
                "c.address, " +
                "c.phone, " +
                "c.city_id, " +
                "ci.city_name " +
                "FROM cinema c " +
                "INNER JOIN city ci " +
                "ON c.city_id = ci.city_id " +
                "WHERE c.cinema_id = ? " +
                "ORDER BY c.cinema_id";
    }

    // =====================================================
    // EMPLOYEE / UNKNOWN
    // =====================================================

    else {

        return;
    }

    try (
            Connection connection =
                    DBConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        // =================================================
        // MANAGER CINEMA ID
        // =================================================

        if (role.equals("manager")) {

            statement.setInt(
                    1,
                    Session.getCinemaId()
            );
        }

        // =================================================
        // EXECUTE QUERY
        // =================================================

        try (
                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                cinemaList.add(
                        new CinemaItem(
                                result.getInt("cinema_id"),
                                result.getString("cinema_name"),
                                result.getString("address"),
                                result.getString("phone"),
                                result.getInt("city_id"),
                                result.getString("city_name")
                        )
                );
            }
        }

        // =================================================
        // SET TABLE
        // =================================================

        tblCinemas.setItems(cinemaList);

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
    // CINEMA SELECT
    // =========================================================

    @FXML
    private void handleCinemaSelect() {

        Cinema selectedCinema =
                tblCinemas.getSelectionModel()
                        .getSelectedItem();


        if (selectedCinema == null) {

            hallList.clear();

            return;
        }


        /*
         * Extra security:
         *
         * Manager can only select his own cinema.
         */

        if (isManager()
                && selectedCinema.getCinemaId()
                != Session.getCinemaId()) {

            hallList.clear();

            showAlert(
                    Alert.AlertType.WARNING,
                    "Access Denied",
                    "You can only access your cinema."
            );

            return;
        }


        loadHalls(
                selectedCinema.getCinemaId()
        );
    }


    // =========================================================
    // LOAD HALLS
    // =========================================================

    private void loadHalls(int cinemaId) {

        hallList.clear();


        /*
         * Extra security:
         *
         * Manager can only load halls
         * from his cinema.
         */

        if (isManager()
                && cinemaId != Session.getCinemaId()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Access Denied",
                    "You can only access halls in your cinema."
            );

            return;
        }


        String sql =
                "SELECT hall_id, hall_name, capacity " +
                "FROM hall " +
                "WHERE cinema_id = ? " +
                "ORDER BY hall_id";


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
                                    result.getString("hall_name"),
                                    result.getInt("capacity")
                            )
                    );
                }
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


    // =========================================================
    // ADD CINEMA
    // =========================================================

    @FXML
    private void handleAdd() {

        /*
         * ONLY ADMIN
         */

        if (!isAdmin()) {

            showAccessDenied();

            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/AddCinema.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            AddCinemaController controller =
                    loader.getController();


            controller.setCinemaController(this);


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Add Cinema"
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
                    "Could not open Add Cinema.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // EDIT CINEMA
    // =========================================================

    @FXML
    private void handleEdit() {

        /*
         * ONLY ADMIN
         */

        if (!isAdmin()) {

            showAccessDenied();

            return;
        }


        Cinema selectedCinema =
                tblCinemas.getSelectionModel()
                        .getSelectedItem();


        if (selectedCinema == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Cinema",
                    "Please select a cinema first."
            );

            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/EditCinema.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            EditCinemaController controller =
                    loader.getController();


            controller.setCinema(
                    selectedCinema,
                    this
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Edit Cinema"
            );


            stage.setScene(
                    new Scene(root)
            );
            
            stage.getIcons().add(
    new javafx.scene.image.Image(
        getClass().getResourceAsStream("/cinemasystem/images/icon.png")
    )
);


            stage.setResizable(false);


            stage.show();


        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Edit Cinema.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // DELETE CINEMA
    // =========================================================

    @FXML
    private void handleDelete() {

        /*
         * ONLY ADMIN
         */

        if (!isAdmin()) {

            showAccessDenied();

            return;
        }


        Cinema selectedCinema =
                tblCinemas.getSelectionModel()
                        .getSelectedItem();


        if (selectedCinema == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Delete Cinema",
                    "Please select a cinema first."
            );

            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Delete Cinema"
        );


        confirmation.setHeaderText(
                null
        );


        confirmation.setContentText(
                "Are you sure you want to delete \""
                + selectedCinema.getCinemaName()
                + "\"?"
        );


        Optional<ButtonType> result =
                confirmation.showAndWait();


        if (result.isEmpty()
                || result.get() != ButtonType.OK) {

            return;
        }


        deleteCinema(
                selectedCinema.getCinemaId()
        );
    }


    // =========================================================
    // DELETE CINEMA FROM DATABASE
    // =========================================================

    private void deleteCinema(int cinemaId) {

        /*
         * ONLY ADMIN
         */

        if (!isAdmin()) {

            showAccessDenied();

            return;
        }


        String sql =
                "DELETE FROM cinema " +
                "WHERE cinema_id = ?";


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


            int rowsAffected =
                    statement.executeUpdate();


            if (rowsAffected > 0) {

                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Cinema deleted successfully."
                );


                hallList.clear();


                loadCinemas();
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not delete the cinema.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // ADD HALL
    // =========================================================

    @FXML
    private void handleAddHall() {

        /*
         * ADMIN + MANAGER
         */

        if (!isAdmin() && !isManager()) {

            showAccessDenied();

            return;
        }


        Cinema selectedCinema =
                tblCinemas.getSelectionModel()
                        .getSelectedItem();


        if (selectedCinema == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Hall",
                    "Please select a cinema first."
            );

            return;
        }


        /*
         * Manager can ONLY add halls
         * to his own cinema.
         */

        if (isManager()
                && selectedCinema.getCinemaId()
                != Session.getCinemaId()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Access Denied",
                    "You can only add halls to your cinema."
            );

            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/AddHall.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            AddHallController controller =
                    loader.getController();


            controller.setCinemaId(
                    selectedCinema.getCinemaId()
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Add Hall"
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
             * Refresh halls
             */

            loadHalls(
                    selectedCinema.getCinemaId()
            );


        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Add Hall window.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // EDIT HALL
    // =========================================================

    @FXML
    private void handleEditHall() {

        /*
         * ADMIN + MANAGER
         */

        if (!isAdmin() && !isManager()) {

            showAccessDenied();

            return;
        }


        HallItem selectedHall =
                tblHalls.getSelectionModel()
                        .getSelectedItem();


        if (selectedHall == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Hall",
                    "Please select a hall first."
            );

            return;
        }


        /*
         * Manager security check
         */

        if (isManager()) {

            if (!hallBelongsToCinema(
                    selectedHall.getHallId(),
                    Session.getCinemaId())) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Access Denied",
                        "This hall does not belong to your cinema."
                );

                return;
            }
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/cinemasystem/view/EditHall.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            EditHallController controller =
                    loader.getController();


            controller.setHall(
                    selectedHall.getHallId(),
                    selectedHall.getHallName(),
                    selectedHall.getCapacity()
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "Edit Hall"
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
             * Refresh selected cinema halls
             */

            Cinema selectedCinema =
                    tblCinemas.getSelectionModel()
                            .getSelectedItem();


            if (selectedCinema != null) {

                loadHalls(
                        selectedCinema.getCinemaId()
                );
            }


        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not open Edit Hall window.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // DELETE HALL
    // =========================================================

    @FXML
    private void handleDeleteHall() {

        /*
         * ADMIN + MANAGER
         */

        if (!isAdmin() && !isManager()) {

            showAccessDenied();

            return;
        }


        HallItem selected =
                tblHalls.getSelectionModel()
                        .getSelectedItem();


        if (selected == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Delete Hall",
                    "Please select a hall first."
            );

            return;
        }


        /*
         * Manager security check
         */

        if (isManager()) {

            if (!hallBelongsToCinema(
                    selected.getHallId(),
                    Session.getCinemaId())) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Access Denied",
                        "This hall does not belong to your cinema."
                );

                return;
            }
        }


        /*
         * Confirmation
         */

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Delete Hall"
        );


        confirmation.setHeaderText(
                null
        );


        confirmation.setContentText(
                "Are you sure you want to delete hall:\n\n"
                + selected.getHallName()
                + "?"
        );


        Optional<ButtonType> result =
                confirmation.showAndWait();


        if (result.isEmpty()
                || result.get() != ButtonType.OK) {

            return;
        }


        int hallId =
                selected.getHallId();


        /*
         * =====================================================
         * DELETE SEATS FIRST
         * =====================================================
         */

        String deleteSeatsSql =
                "DELETE FROM seat " +
                "WHERE hall_id = ?";


        /*
         * =====================================================
         * DELETE HALL
         * =====================================================
         *
         * For Manager:
         *
         * DELETE only when the hall belongs
         * to his cinema.
         */

        String deleteHallSql;


        if (isManager()) {

            deleteHallSql =
                    "DELETE FROM hall " +
                    "WHERE hall_id = ? " +
                    "AND cinema_id = ?";

        } else {

            deleteHallSql =
                    "DELETE FROM hall " +
                    "WHERE hall_id = ?";
        }


        try (
                Connection connection =
                        DBConnection.getConnection()
        ) {


            /*
             * Start transaction
             */

            connection.setAutoCommit(false);


            try (
                    PreparedStatement deleteSeats =
                            connection.prepareStatement(
                                    deleteSeatsSql
                            );

                    PreparedStatement deleteHall =
                            connection.prepareStatement(
                                    deleteHallSql
                            )
            ) {


                /*
                 * =================================================
                 * 1. DELETE SEATS
                 * =================================================
                 */

                deleteSeats.setInt(
                        1,
                        hallId
                );


                deleteSeats.executeUpdate();


                /*
                 * =================================================
                 * 2. DELETE HALL
                 * =================================================
                 */

                deleteHall.setInt(
                        1,
                        hallId
                );


                if (isManager()) {

                    deleteHall.setInt(
                            2,
                            Session.getCinemaId()
                    );
                }


                int rowsAffected =
                        deleteHall.executeUpdate();


                /*
                 * If nothing was deleted,
                 * manager is not allowed to delete this hall.
                 */

                if (rowsAffected == 0) {

                    connection.rollback();


                    showAlert(
                            Alert.AlertType.WARNING,
                            "Access Denied",
                            "This hall does not belong to your cinema."
                    );

                    return;
                }


                /*
                 * =================================================
                 * 3. COMMIT
                 * =================================================
                 */

                connection.commit();


                /*
                 * =================================================
                 * 4. REFRESH
                 * =================================================
                 */

                Cinema selectedCinema =
                        tblCinemas.getSelectionModel()
                                .getSelectedItem();


                if (selectedCinema != null) {

                    loadHalls(
                            selectedCinema.getCinemaId()
                    );
                }


                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Hall deleted successfully."
                );


            } catch (SQLException e) {

                /*
                 * Something failed
                 * -> undo everything
                 */

                connection.rollback();

                throw e;
            }


        } catch (SQLException e) {

            e.printStackTrace();


            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not delete the hall.\n\n"
                    + e.getMessage()
            );
        }
    }


    // =========================================================
    // CHECK HALL BELONGS TO CINEMA
    // =========================================================

    private boolean hallBelongsToCinema(
            int hallId,
            int cinemaId) {

        String sql =
                "SELECT COUNT(*) " +
                "FROM hall " +
                "WHERE hall_id = ? " +
                "AND cinema_id = ?";


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


            statement.setInt(
                    2,
                    cinemaId
            );


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


    // =========================================================
    // ROLE HELPERS
    // =========================================================

    private boolean isAdmin() {

        String role =
                Session.getRole();


        return role != null
                && role.trim()
                        .equalsIgnoreCase("admin");
    }


    private boolean isManager() {

        String role =
                Session.getRole();


        return role != null
                && role.trim()
                        .equalsIgnoreCase("manager");
    }


    private boolean isEmployee() {

        String role =
                Session.getRole();


        return role != null
                && role.trim()
                        .equalsIgnoreCase("employee");
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
    // HALL ITEM
    // =========================================================

    public static class HallItem {

        private final int hallId;

        private final String hallName;

        private final int capacity;


        public HallItem(
                int hallId,
                String hallName,
                int capacity) {

            this.hallId = hallId;

            this.hallName = hallName;

            this.capacity = capacity;
        }


        public int getHallId() {

            return hallId;
        }


        public String getHallName() {

            return hallName;
        }


        public int getCapacity() {

            return capacity;
        }
    }


    // =========================================================
    // CINEMA ITEM
    // =========================================================

    /*
     * This class is only used to display the cinema
     * ID and name when loading the Manager's cinema.
     *
     * The original controller uses Cinema as the TableView type,
     * so the table is still populated using Cinema objects.
     *
     * If your current project already has CinemaItem elsewhere,
     * you do not need to use this class.
     */

    public static class CinemaItem extends Cinema {

        public CinemaItem(
        int cinemaId,
        String cinemaName,
        String address,
        String phone,
        int cityId,
        String cityName)
        {
            super(
                cinemaId,
                cinemaName,
                address,
                phone,
                cityId,
                cityName
            );
}
    }


    // =========================================================
    // BACK TO DASHBOARD
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
                            tblCinemas
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
                    "Error",
                    "Could not return to Dashboard."
            );
        }
    }
}
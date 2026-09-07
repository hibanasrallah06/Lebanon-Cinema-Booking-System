package cinemasystem.controller;

import cinemasystem.database.DBConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Optional;


public class AddCinemaController {

    // =========================
    // FIELDS
    // =========================

    @FXML
    private TextField txtCinemaName;

    @FXML
    private TextField txtAddress;

    @FXML
    private TextField txtPhone;

    @FXML
    private ComboBox<CityItem> cmbCity;


    // =========================
    // CITY LIST
    // =========================

    private ObservableList<CityItem> cityList =
            FXCollections.observableArrayList();


    // =========================
    // CINEMA CONTROLLER
    // =========================

    private CinemaController cinemaController;


    // =========================
    // INITIALIZE
    // =========================

    @FXML
    public void initialize() {

        cmbCity.setItems(cityList);

        loadCities();
    }


    // =========================
    // SET CINEMA CONTROLLER
    // =========================

    public void setCinemaController(
            CinemaController cinemaController) {

        this.cinemaController = cinemaController;
    }


    // =========================
    // LOAD CITIES
    // =========================

    private void loadCities() {

        cityList.clear();

        String sql =
                "SELECT city_id, city_name " +
                "FROM city " +
                "ORDER BY city_name";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            while (result.next()) {

                CityItem city =
                        new CityItem(
                                result.getInt("city_id"),
                                result.getString("city_name")
                        );

                cityList.add(city);
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not load cities."
            );
        }
    }


    // =========================
    // ADD CINEMA
    // =========================

    @FXML
    private void handleSave() {

        String cinemaName =
                txtCinemaName.getText().trim();

        String address =
                txtAddress.getText().trim();

        String phone =
                txtPhone.getText().trim();

        CityItem selectedCity =
                cmbCity.getValue();


        // =========================
        // VALIDATION
        // =========================

        if (cinemaName.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Cinema",
                    "Please enter the cinema name."
            );

            txtCinemaName.requestFocus();

            return;
        }


        if (selectedCity == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add Cinema",
                    "Please select a city."
            );

            cmbCity.requestFocus();

            return;
        }


        // =========================
        // INSERT CINEMA
        // =========================

        String sql =
                "INSERT INTO cinema " +
                "(cinema_name, address, phone, city_id) " +
                "VALUES (?, ?, ?, ?)";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    cinemaName
            );

            statement.setString(
                    2,
                    address
            );

            statement.setString(
                    3,
                    phone
            );

            statement.setInt(
                    4,
                    selectedCity.getCityId()
            );


            statement.executeUpdate();


            // =========================
            // SUCCESS MESSAGE
            // =========================

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Cinema added successfully."
            );


            // =========================
            // REFRESH CINEMA TABLE
            // =========================

            if (cinemaController != null) {

                cinemaController.loadCinemas();
            }


            // =========================
            // CLOSE WINDOW
            // =========================

            closeWindow();


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not add the cinema."
            );
        }
    }


    // =========================
    // ADD CITY
    // =========================

    @FXML
    private void handleAddCity() {

        TextInputDialog dialog =
                new TextInputDialog();


        dialog.setTitle("Add City");

        dialog.setHeaderText(
                "Create a new city"
        );

        dialog.setContentText(
                "City name:"
        );


        Optional<String> result =
                dialog.showAndWait();


        // User pressed Cancel
        if (result.isEmpty()) {

            return;
        }


        String cityName =
                result.get().trim();


        // =========================
        // VALIDATION
        // =========================

        if (cityName.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Add City",
                    "Please enter a city name."
            );

            return;
        }


        // =========================
        // CHECK DUPLICATE CITY
        // =========================

        String checkSql =
                "SELECT city_id " +
                "FROM city " +
                "WHERE LOWER(city_name) = LOWER(?)";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(checkSql)
        ) {

            statement.setString(
                    1,
                    cityName
            );


            ResultSet resultSet =
                    statement.executeQuery();


            if (resultSet.next()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Add City",
                        "This city already exists."
                );

                return;
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not check the city."
            );

            return;
        }


        // =========================
        // INSERT CITY
        // =========================

        String sql =
                "INSERT INTO city (city_name) " +
                "VALUES (?)";


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    cityName
            );


            statement.executeUpdate();


            // =========================
            // RELOAD CITIES
            // =========================

            loadCities();


            // =========================
            // SELECT NEW CITY
            // =========================

            for (CityItem city : cityList) {

                if (city.getCityName()
                        .equalsIgnoreCase(cityName)) {

                    cmbCity.setValue(city);

                    break;
                }
            }


            // =========================
            // SUCCESS
            // =========================

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "City added successfully."
            );


        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not add the city."
            );
        }
    }


    // =========================
    // CANCEL
    // =========================

    @FXML
    private void handleCancel() {

        closeWindow();
    }


    // =========================
    // CLOSE WINDOW
    // =========================

    private void closeWindow() {

        if (txtCinemaName.getScene() == null) {

            return;
        }


        Stage stage =
                (Stage) txtCinemaName
                        .getScene()
                        .getWindow();


        stage.close();
    }


    // =========================
    // ALERT
    // =========================

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


    // =========================
    // CITY ITEM
    // =========================

    public static class CityItem {

        private int cityId;

        private String cityName;


        public CityItem(
                int cityId,
                String cityName) {

            this.cityId = cityId;

            this.cityName = cityName;
        }


        public int getCityId() {

            return cityId;
        }


        public String getCityName() {

            return cityName;
        }


        @Override
        public String toString() {

            return cityName;
        }
    }
}
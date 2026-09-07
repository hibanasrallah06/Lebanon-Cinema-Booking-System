package cinemasystem.controller;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Cinema;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditCinemaController {

    @FXML
    private TextField txtCinemaName;

    @FXML
    private TextField txtAddress;

    @FXML
    private TextField txtPhone;

    @FXML
    private ComboBox<CityItem> cmbCity;

    private ObservableList<CityItem> cityList =
            FXCollections.observableArrayList();

    private CinemaController cinemaController;

    private int cinemaId;


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
    // RECEIVE CINEMA
    // =========================

    public void setCinema(
            int cinemaId,
            String cinemaName,
            String address,
            String phone,
            int cityId) {

        this.cinemaId = cinemaId;

        txtCinemaName.setText(cinemaName);

        txtAddress.setText(
                address == null ? "" : address
        );

        txtPhone.setText(
                phone == null ? "" : phone
        );

        // Select current city
        for (CityItem city : cityList) {

            if (city.getCityId() == cityId) {

                cmbCity.setValue(city);

                break;
            }
        }
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

                cityList.add(
                        new CityItem(
                                result.getInt("city_id"),
                                result.getString("city_name")
                        )
                );
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
    // UPDATE CINEMA
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


        if (cinemaName.isEmpty() ||
            selectedCity == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Edit Cinema",
                    "Please enter the cinema name and select a city."
            );

            return;
        }


        String sql =
                "UPDATE cinema " +
                "SET cinema_name = ?, " +
                "address = ?, " +
                "phone = ?, " +
                "city_id = ? " +
                "WHERE cinema_id = ?";


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

            statement.setInt(
                    5,
                    cinemaId
            );


            statement.executeUpdate();


            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Cinema updated successfully."
            );


            // Refresh Cinema table
            if (cinemaController != null) {

                cinemaController.loadCinemas();
            }


            closeWindow();

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Could not update the cinema."
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
    
    
    public void setCinema(
        Cinema cinema,
        CinemaController cinemaController) {

    this.cinemaController = cinemaController;

    cinemaId = cinema.getCinemaId();

    txtCinemaName.setText(
            cinema.getCinemaName()
    );

    txtAddress.setText(
            cinema.getAddress()
    );

    txtPhone.setText(
            cinema.getPhone()
    );

    for (CityItem city : cityList) {

        if (city.getCityId() == cinema.getCityId()) {

            cmbCity.setValue(city);
            break;
        }
    }
    }
}
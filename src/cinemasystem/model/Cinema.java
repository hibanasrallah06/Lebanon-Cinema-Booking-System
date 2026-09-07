package cinemasystem.model;

public class Cinema {

    private int cinemaId;
    private String cinemaName;
    private String address;
    private String phone;
    private int cityId;
    private String cityName;

    public Cinema(
            int cinemaId,
            String cinemaName,
            String address,
            String phone,
            int cityId,
            String cityName) {

        this.cinemaId = cinemaId;
        this.cinemaName = cinemaName;
        this.address = address;
        this.phone = phone;
        this.cityId = cityId;
        this.cityName = cityName;
    }

    public int getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(int cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    @Override
    public String toString() {
        return cinemaName;
    }
}
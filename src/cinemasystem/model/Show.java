package cinemasystem.model;

public class Show {

    private int showId;
    private String movie;
    private String hall;
    private String cinemaName;
    private String showDate;
    private String startTime;
    private String endTime;
    private double ticketPrice;

    public Show(
            int showId,
            String movie,
            String hall,
            String cinemaName,
            String showDate,
            String startTime,
            String endTime,
            double ticketPrice) {

        this.showId = showId;
        this.movie = movie;
        this.hall = hall;
        this.cinemaName = cinemaName;
        this.showDate = showDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ticketPrice = ticketPrice;
    }

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public String getMovie() {
        return movie;
    }

    public void setMovie(String movie) {
        this.movie = movie;
    }

    public String getHall() {
        return hall;
    }

    public void setHall(String hall) {
        this.hall = hall;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getShowDate() {
        return showDate;
    }

    public void setShowDate(String showDate) {
        this.showDate = showDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}
package cinemasystem.model;

public class Snack {

    private int snackId;
    private String snackName;
    private double price;
    private boolean active;

    public Snack(
            int snackId,
            String snackName,
            double price,
            boolean active) {

        this.snackId = snackId;
        this.snackName = snackName;
        this.price = price;
        this.active = active;
    }

    public int getSnackId() {
        return snackId;
    }

    public String getSnackName() {
        return snackName;
    }

    public double getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return snackName;
    }
}
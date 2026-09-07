package cinemasystem.model;

public class Movie {

    private int movieId;
    private String title;
    private String description;
    private String genre;
    private String language;
    private int duration;
    private String ageRating;

    // TMDB poster path
    private String posterPath;

    public Movie() {
    }

    public Movie(int movieId,
                 String title,
                 String description,
                 String genre,
                 String language,
                 int duration,
                 String ageRating) {

        this.movieId = movieId;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.language = language;
        this.duration = duration;
        this.ageRating = ageRating;
    }

    public Movie(int movieId,
                 String title,
                 String description,
                 String genre,
                 String language,
                 int duration,
                 String ageRating,
                 String posterPath) {

        this.movieId = movieId;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.language = language;
        this.duration = duration;
        this.ageRating = ageRating;
        this.posterPath = posterPath;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    @Override
    public String toString() {
        return title;
    }
}
package cinemasystem.dao;

import cinemasystem.database.DBConnection;
import cinemasystem.model.Movie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    public List<Movie> getAllMovies() {

        List<Movie> movies = new ArrayList<>();

        String sql =
                "SELECT movie_id, title, description, genre, " +
                "language, duration, age_rating " +
                "FROM movie";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            while (result.next()) {

                Movie movie = new Movie();

                movie.setMovieId(
                        result.getInt("movie_id")
                );

                movie.setTitle(
                        result.getString("title")
                );

                movie.setDescription(
                        result.getString("description")
                );

                movie.setGenre(
                        result.getString("genre")
                );

                movie.setLanguage(
                        result.getString("language")
                );

                movie.setDuration(
                        result.getInt("duration")
                );

                movie.setAgeRating(
                        result.getString("age_rating")
                );

                movies.add(movie);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return movies;
    }
}
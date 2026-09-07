package cinemasystem.service;

import cinemasystem.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class RecommendationService {

    public List<Movie> recommendMovies(
            List<Movie> movies,
            String userRequest) {

        List<MovieScore> scoredMovies =
                new ArrayList<>();

        if (userRequest == null ||
                userRequest.trim().isEmpty()) {

            return movies;
        }

        String request =
                userRequest.toLowerCase().trim();

        String[] keywords =
                request.split("\\s+");

        for (Movie movie : movies) {

            int score = 0;

            String title =
                    safe(movie.getTitle()).toLowerCase();

            String description =
                    safe(movie.getDescription()).toLowerCase();

            String genre =
                    safe(movie.getGenre()).toLowerCase();

            String language =
                    safe(movie.getLanguage()).toLowerCase();

            for (String keyword : keywords) {

                if (keyword.length() < 3) {
                    continue;
                }

                if (genre.contains(keyword)) {
                    score += 5;
                }

                if (title.contains(keyword)) {
                    score += 4;
                }

                if (description.contains(keyword)) {
                    score += 3;
                }

                if (language.contains(keyword)) {
                    score += 2;
                }
            }

            if (score > 0) {

                scoredMovies.add(
                        new MovieScore(movie, score)
                );
            }
        }

        scoredMovies.sort(
                (a, b) ->
                        Integer.compare(
                                b.score,
                                a.score
                        )
        );

        List<Movie> recommendations =
                new ArrayList<>();

        for (MovieScore item : scoredMovies) {

            recommendations.add(
                    item.movie
            );

            if (recommendations.size() == 5) {
                break;
            }
        }

        return recommendations;
    }


    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }


    private static class MovieScore {

        Movie movie;
        int score;

        MovieScore(
                Movie movie,
                int score) {

            this.movie = movie;
            this.score = score;
        }
    }
}
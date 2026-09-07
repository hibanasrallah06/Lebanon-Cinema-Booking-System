package cinemasystem.service;

import cinemasystem.model.Movie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SmartRecommendationService {

    public List<Movie> recommend(
            String userRequest,
            List<Movie> movies) {

        List<MovieScore> scoredMovies =
                new ArrayList<>();

        String request =
                userRequest == null
                        ? ""
                        : userRequest.toLowerCase(Locale.ROOT);

        for (Movie movie : movies) {

            if (movie == null) {
                continue;
            }

            int score = calculateScore(
                    request,
                    movie
            );

            if (score > 0) {
                scoredMovies.add(
                        new MovieScore(movie, score)
                );
            }
        }

        scoredMovies.sort(
                Comparator.comparingInt(
                        MovieScore::getScore
                ).reversed()
        );

        List<Movie> result =
                new ArrayList<>();

        int limit =
                Math.min(3, scoredMovies.size());

        for (int i = 0; i < limit; i++) {

            result.add(
                    scoredMovies
                            .get(i)
                            .getMovie()
            );
        }

        return result;
    }


    private int calculateScore(
            String request,
            Movie movie) {

        int score = 0;

        String title =
                safe(movie.getTitle());

        String genre =
                safe(movie.getGenre());

        String language =
                safe(movie.getLanguage());

        String ageRating =
                safe(movie.getAgeRating());

        String description =
                safe(movie.getDescription());


        // =====================================================
        // GENRE
        // =====================================================

        if (containsAny(
                request,
                "action",
                "اكشن",
                "أكشن")) {

            if (genre.contains("action")) {
                score += 10;
            }
        }

        if (containsAny(
                request,
                "comedy",
                "funny",
                "comed",
                "كوميدي",
                "كوميديا")) {

            if (genre.contains("comedy")) {
                score += 10;
            }
        }

        if (containsAny(
                request,
                "horror",
                "scary",
                "رعب")) {

            if (genre.contains("horror")) {
                score += 10;
            }
        }

        if (containsAny(
                request,
                "romance",
                "romantic",
                "love",
                "رومانسي",
                "رومانسية")) {

            if (genre.contains("romance")) {
                score += 10;
            }
        }

        if (containsAny(
                request,
                "drama",
                "dramatic",
                "دراما")) {

            if (genre.contains("drama")) {
                score += 10;
            }
        }

        if (containsAny(
                request,
                "adventure",
                "adventurous",
                "مغامرة")) {

            if (genre.contains("adventure")) {
                score += 10;
            }
        }

        if (containsAny(
                request,
                "animation",
                "animated",
                "cartoon",
                "انيميشن",
                "كرتون")) {

            if (genre.contains("animation")) {
                score += 10;
            }
        }

        if (containsAny(
                request,
                "sci-fi",
                "science fiction",
                "science",
                "خيال علمي")) {

            if (genre.contains("sci-fi")
                    || genre.contains("science")) {

                score += 10;
            }
        }

        if (containsAny(
                request,
                "thriller",
                "suspense",
                "تشويق")) {

            if (genre.contains("thriller")) {
                score += 10;
            }
        }


        // =====================================================
        // LANGUAGE
        // =====================================================

        if (containsAny(
                request,
                "english",
                "إنجليزي",
                "انجليزي")) {

            if (language.contains("english")) {
                score += 8;
            }
        }

        if (containsAny(
                request,
                "arabic",
                "عربي",
                "العربية")) {

            if (language.contains("arabic")) {
                score += 8;
            }
        }

        if (containsAny(
                request,
                "french",
                "فرنسي",
                "الفرنسية")) {

            if (language.contains("french")) {
                score += 8;
            }
        }


        // =====================================================
        // DURATION
        // =====================================================

        int duration =
                movie.getDuration();

        if (containsAny(
                request,
                "short",
                "short movie",
                "قصير")) {

            if (duration <= 100) {
                score += 6;
            }
        }

        if (containsAny(
                request,
                "long",
                "long movie",
                "طويل")) {

            if (duration >= 130) {
                score += 6;
            }
        }

        if (containsAny(
                request,
                "quick",
                "fast",
                "سريع")) {

            if (duration <= 100) {
                score += 5;
            }
        }


        // =====================================================
        // DESCRIPTION
        // =====================================================

        String[] words =
                request.split("\\s+");

        for (String word : words) {

            if (word.length() < 3) {
                continue;
            }

            if (genre.contains(word)) {
                score += 5;
            }

            if (description.contains(word)) {
                score += 3;
            }

            if (title.contains(word)) {
                score += 7;
            }

            if (language.contains(word)) {
                score += 4;
            }

            if (ageRating.contains(word)) {
                score += 2;
            }
        }


        // =====================================================
        // GENERAL KEYWORDS
        // =====================================================

        if (containsAny(
                request,
                "fun",
                "funny",
                "مضحك")) {

            if (genre.contains("comedy")) {
                score += 5;
            }
        }

        if (containsAny(
                request,
                "fight",
                "fighting",
                "battle",
                "قتال")) {

            if (genre.contains("action")) {
                score += 5;
            }

            if (description.contains("fight")
                    || description.contains("battle")) {

                score += 4;
            }
        }

        if (containsAny(
                request,
                "love",
                "couple",
                "حب",
                "علاقة")) {

            if (genre.contains("romance")) {
                score += 5;
            }
        }

        return score;
    }


    private boolean containsAny(
            String text,
            String... values) {

        for (String value : values) {

            if (text.contains(
                    value.toLowerCase(Locale.ROOT))) {

                return true;
            }
        }

        return false;
    }


    private String safe(String value) {

        return value == null
                ? ""
                : value.toLowerCase(
                        Locale.ROOT
                );
    }


    private static class MovieScore {

        private final Movie movie;
        private final int score;

        MovieScore(
                Movie movie,
                int score) {

            this.movie = movie;
            this.score = score;
        }

        Movie getMovie() {
            return movie;
        }

        int getScore() {
            return score;
        }
    }
}
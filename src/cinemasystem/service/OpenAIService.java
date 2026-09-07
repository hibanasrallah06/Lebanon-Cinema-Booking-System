package cinemasystem.service;

import cinemasystem.model.Movie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenAIService {

    /*
     * =========================================================
     * LOCAL SMART AI RECOMMENDATION ENGINE
     * =========================================================
     *
     * IMPORTANT:
     *
     * This does NOT use OpenAI API.
     * This does NOT require an API key.
     * This does NOT cost money.
     *
     * It analyzes:
     *
     * - Genre
     * - Language
     * - Duration
     * - Age Rating
     * - Title
     * - Description
     * - User mood/preferences
     * - Arabic
     * - English
     * - Arabizi
     *
     * Then it calculates a score for every movie
     * and returns the best 3 movies.
     */


    // =========================================================
    // MAIN METHOD USED BY CONTROLLER
    // =========================================================

    public List<String> getRecommendationIds(
            String userRequest,
            List<Movie> movies) {

        List<String> result =
                new ArrayList<>();


        if (movies == null ||
                movies.isEmpty()) {

            return result;
        }


        if (userRequest == null ||
                userRequest.trim().isEmpty()) {

            return result;
        }


        // =====================================================
        // NORMALIZE USER REQUEST
        // =====================================================

        String request =
                normalize(
                        userRequest
                );


        // =====================================================
        // DETECT GENERAL REQUEST
        // =====================================================

        boolean generalRequest =
                isGeneralMovieRequest(
                        request
                );


        // =====================================================
        // CREATE SCORED MOVIES
        // =====================================================

        List<ScoredMovie> scoredMovies =
                new ArrayList<>();


        for (Movie movie : movies) {

            if (movie == null) {
                continue;
            }


            int score =
                    calculateScore(
                            request,
                            movie
                    );


            scoredMovies.add(
                    new ScoredMovie(
                            movie,
                            score
                    )
            );
        }


        // =====================================================
        // SORT BY SCORE
        // =====================================================

        scoredMovies.sort(
                Comparator
                        .comparingInt(
                                ScoredMovie::getScore
                        )
                        .reversed()
        );


        // =====================================================
        // GENERAL REQUEST
        // =====================================================

        /*
         * Example:
         *
         * "movies available now"
         * "what movies do you have"
         * "show me available movies"
         *
         * Since Movie does not contain showtime information,
         * we interpret this as:
         *
         * "movies currently stored in the database"
         */

        if (generalRequest) {

            for (int i = 0;
                 i < scoredMovies.size()
                         && result.size() < 3;
                 i++) {

                Movie movie =
                        scoredMovies
                                .get(i)
                                .getMovie();


                result.add(
                        String.valueOf(
                                movie.getMovieId()
                        )
                );
            }

            return result;
        }


        // =====================================================
        // ADD TOP MATCHES
        // =====================================================

        for (ScoredMovie scored :
                scoredMovies) {

            /*
             * Minimum score.
             *
             * If the user request has some meaningful
             * relationship with the movie, it can pass.
             */

            if (scored.getScore() >= 10) {

                result.add(
                        String.valueOf(
                                scored.getMovie()
                                        .getMovieId()
                        )
                );
            }


            if (result.size() >= 3) {
                break;
            }
        }


        // =====================================================
        // FALLBACK
        // =====================================================

        /*
         * If nothing matched strongly,
         * return the best available movies instead
         * of showing an empty result.
         */

        if (result.isEmpty()) {

            for (int i = 0;
                 i < scoredMovies.size()
                         && result.size() < 3;
                 i++) {

                result.add(
                        String.valueOf(
                                scoredMovies
                                        .get(i)
                                        .getMovie()
                                        .getMovieId()
                        )
                );
            }
        }


        return result;
    }


    // =========================================================
    // CALCULATE MOVIE SCORE
    // =========================================================

    private int calculateScore(
            String request,
            Movie movie) {


        int score = 0;


        String title =
                normalize(
                        safe(movie.getTitle())
                );


        String genre =
                normalize(
                        safe(movie.getGenre())
                );


        String language =
                normalize(
                        safe(movie.getLanguage())
                );


        String description =
                normalize(
                        safe(movie.getDescription())
                );


        String ageRating =
                normalize(
                        safe(movie.getAgeRating())
                );


        String fullMovieText =
                title
                + " "
                + genre
                + " "
                + language
                + " "
                + description
                + " "
                + ageRating;


        // =====================================================
        // EXTRACT USER PREFERENCES
        // =====================================================

        Set<String> genres =
                detectGenres(request);


        Set<String> languages =
                detectLanguages(request);


        Set<String> moods =
                detectMoods(request);


        Set<String> durationPreferences =
                detectDurationPreferences(
                        request
                );


        Set<String> agePreferences =
                detectAgePreferences(
                        request
                );


        // =====================================================
        // GENRE MATCH
        // =====================================================

        for (String wantedGenre :
                genres) {

            if (genre.contains(wantedGenre)) {

                score += 40;

            } else if (
                    description.contains(
                            wantedGenre
                    )) {

                score += 20;
            }
        }


        // =====================================================
        // LANGUAGE MATCH
        // =====================================================

        for (String wantedLanguage :
                languages) {

            if (language.contains(
                    wantedLanguage)) {

                score += 20;
            }
        }


        // =====================================================
        // MOOD / PREFERENCE MATCH
        // =====================================================

        for (String mood :
                moods) {

            if (fullMovieText.contains(
                    mood)) {

                score += 15;
            }
        }


        // =====================================================
        // DURATION
        // =====================================================

        int duration =
                movie.getDuration();


        for (String preference :
                durationPreferences) {

            if (preference.equals("short")
                    && duration > 0
                    && duration <= 110) {

                score += 15;
            }


            if (preference.equals("medium")
                    && duration >= 100
                    && duration <= 140) {

                score += 15;
            }


            if (preference.equals("long")
                    && duration >= 140) {

                score += 15;
            }
        }


        // =====================================================
        // AGE RATING
        // =====================================================

        for (String age :
                agePreferences) {

            if (ageRating.contains(age)) {

                score += 15;
            }
        }


        // =====================================================
        // EXACT TITLE MATCH
        // =====================================================

        if (!title.isEmpty()
                && request.contains(title)) {

            score += 50;
        }


        // =====================================================
        // WORD MATCHING
        // =====================================================

        Set<String> requestWords =
                extractWords(request);


        for (String word :
                requestWords) {

            if (word.length() < 3) {
                continue;
            }


            if (title.contains(word)) {

                score += 15;

            } else if (genre.contains(word)) {

                score += 12;

            } else if (language.contains(word)) {

                score += 10;

            } else if (description.contains(word)) {

                score += 5;
            }
        }


        return score;
    }


    // =========================================================
    // DETECT GENRES
    // =========================================================

    private Set<String> detectGenres(
            String request) {

        Set<String> genres =
                new HashSet<>();


        Map<String, String[]> dictionary =
                new HashMap<>();


        dictionary.put(
                "action",
                new String[]{
                        "action",
                        "اكشن",
                        "أكشن",
                        "akشن",
                        "akشن"
                }
        );


        dictionary.put(
                "comedy",
                new String[]{
                        "comedy",
                        "funny",
                        "ضحك",
                        "كوميدي",
                        "كوميديا",
                        "comedie",
                        "d7ek",
                        "md7ek",
                        "fun"
                }
        );


        dictionary.put(
                "horror",
                new String[]{
                        "horror",
                        "scary",
                        "رعب",
                        "مرعب",
                        "خوف",
                        "5of",
                        "m5awif"
                }
        );


        dictionary.put(
                "romance",
                new String[]{
                        "romance",
                        "romantic",
                        "love",
                        "رومانسي",
                        "حب",
                        "7ob",
                        "romans"
                }
        );


        dictionary.put(
                "drama",
                new String[]{
                        "drama",
                        "dramatic",
                        "دراما",
                        "درامي"
                }
        );


        dictionary.put(
                "thriller",
                new String[]{
                        "thriller",
                        "suspense",
                        "تشويق",
                        "اثارة",
                        "إثارة"
                }
        );


        dictionary.put(
                "adventure",
                new String[]{
                        "adventure",
                        "adventurous",
                        "مغامرة",
                        "مغامرات",
                        "m8amra",
                        "mghamra"
                }
        );


        dictionary.put(
                "animation",
                new String[]{
                        "animation",
                        "animated",
                        "cartoon",
                        "انيميشن",
                        "رسوم",
                        "كرتون"
                }
        );


        dictionary.put(
                "science fiction",
                new String[]{
                        "sci fi",
                        "science fiction",
                        "scifi",
                        "خيال علمي",
                        "5ayal 3elme"
                }
        );


        dictionary.put(
                "fantasy",
                new String[]{
                        "fantasy",
                        "خيال",
                        "سحر",
                        "magic"
                }
        );


        dictionary.put(
                "crime",
                new String[]{
                        "crime",
                        "criminal",
                        "جريمة",
                        "جرائم"
                }
        );


        for (Map.Entry<String, String[]> entry :
                dictionary.entrySet()) {

            for (String keyword :
                    entry.getValue()) {

                if (request.contains(
                        normalize(keyword)
                )) {

                    genres.add(
                            entry.getKey()
                    );

                    break;
                }
            }
        }


        return genres;
    }


    // =========================================================
    // DETECT LANGUAGES
    // =========================================================

    private Set<String> detectLanguages(
            String request) {

        Set<String> languages =
                new HashSet<>();


        if (containsAny(
                request,
                "english",
                "انجليزي",
                "إنجليزي",
                "english movie",
                "english film",
                "bel english"
        )) {

            languages.add("english");
        }


        if (containsAny(
                request,
                "arabic",
                "عربي",
                "عربية",
                "arabic movie",
                "arabic film",
                "bel 3arabe",
                "3arabe"
        )) {

            languages.add("arabic");
        }


        if (containsAny(
                request,
                "french",
                "فرنسي",
                "فرنسية"
        )) {

            languages.add("french");
        }


        if (containsAny(
                request,
                "spanish",
                "اسباني",
                "إسباني"
        )) {

            languages.add("spanish");
        }


        return languages;
    }


    // =========================================================
    // DETECT MOODS
    // =========================================================

    private Set<String> detectMoods(
            String request) {

        Set<String> moods =
                new HashSet<>();


        if (containsAny(
                request,
                "fun",
                "funny",
                "happy",
                "light",
                "خفيف",
                "مضحك",
                "حلو",
                "حلوة",
                "ممتع",
                "ممتعة",
                "7elou",
                "7elwe"
        )) {

            moods.add("comedy");
            moods.add("fun");
            moods.add("happy");
        }


        if (containsAny(
                request,
                "family",
                "families",
                "kids",
                "children",
                "عائلة",
                "عائلي",
                "عائلية",
                "اطفال",
                "أطفال",
                "ولاد",
                "للولاد",
                "lal 3ayle",
                "3ayle"
        )) {

            moods.add("family");
            moods.add("kids");
        }


        if (containsAny(
                request,
                "exciting",
                "exciting movie",
                "تشويق",
                "حماس",
                "حماسي",
                "مثير",
                "مغامرة"
        )) {

            moods.add("exciting");
            moods.add("adventure");
            moods.add("thriller");
        }


        if (containsAny(
                request,
                "dark",
                "serious",
                "غامض",
                "غامضة",
                "داكن",
                "جدي"
        )) {

            moods.add("dark");
            moods.add("serious");
            moods.add("thriller");
        }


        return moods;
    }


    // =========================================================
    // DETECT DURATION
    // =========================================================

    private Set<String> detectDurationPreferences(
            String request) {

        Set<String> result =
                new HashSet<>();


        if (containsAny(
                request,
                "short",
                "quick",
                "not long",
                "قصير",
                "قصيرة",
                "مش طويل",
                "مش طويلة",
                "m2assar",
                "2asir",
                "2asira"
        )) {

            result.add("short");
        }


        if (containsAny(
                request,
                "medium",
                "average length",
                "متوسط",
                "متوسطة"
        )) {

            result.add("medium");
        }


        if (containsAny(
                request,
                "long",
                "long movie",
                "طويل",
                "طويلة",
                "مش مشكلة الوقت",
                "tawil",
                "tawile"
        )) {

            result.add("long");
        }


        return result;
    }


    // =========================================================
    // DETECT AGE
    // =========================================================

    private Set<String> detectAgePreferences(
            String request) {

        Set<String> result =
                new HashSet<>();


        if (containsAny(
                request,
                "pg",
                "pg-13",
                "pg13"
        )) {

            result.add("pg");
            result.add("pg-13");
            result.add("pg13");
        }


        if (containsAny(
                request,
                "family",
                "kids",
                "children",
                "kids friendly",
                "للأطفال",
                "اطفال",
                "أطفال"
        )) {

            result.add("g");
            result.add("pg");
        }


        if (containsAny(
                request,
                "adult",
                "mature",
                "للكبار",
                "للبالغين"
        )) {

            result.add("r");
            result.add("18");
        }


        return result;
    }


    // =========================================================
    // GENERAL REQUEST DETECTION
    // =========================================================

    private boolean isGeneralMovieRequest(
            String request) {

        return containsAny(
                request,

                "movies available",
                "movie available",
                "available movies",
                "available movie",
                "movies available now",
                "movie available now",
                "what movies do you have",
                "what movies are available",
                "show me movies",
                "give me movies",
                "movies now",
                "available now",
                "currently available",
                "currently showing",
                "what can i watch",
                "شو في افلام",
                "شو في فيلم",
                "شو الافلام",
                "شو الأفلام",
                "افلام موجودة",
                "أفلام موجودة",
                "افلام متوفرة",
                "أفلام متوفرة",
                "شو في",
                "شو بقدر احضر",
                "شو فيني احضر",
                "شو فينا نحضر"
        );
    }


    // =========================================================
    // EXTRACT WORDS
    // =========================================================

    private Set<String> extractWords(
            String text) {

        Set<String> words =
                new HashSet<>();


        String[] parts =
                text.split(
                        "[^a-zA-Z0-9\\u0600-\\u06FF]+"
                );


        for (String part :
                parts) {

            String word =
                    normalize(part);


            if (word.length() >= 3) {

                words.add(word);
            }
        }


        return words;
    }


    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(
            String text) {

        if (text == null) {
            return "";
        }


        return text
                .toLowerCase()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }


    // =========================================================
    // CONTAINS ANY
    // =========================================================

    private boolean containsAny(
            String text,
            String... values) {

        for (String value :
                values) {

            if (text.contains(
                    normalize(value)
            )) {

                return true;
            }
        }


        return false;
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "";
        }


        return value;
    }


    // =========================================================
    // SCORED MOVIE
    // =========================================================

    private static class ScoredMovie {

        private final Movie movie;

        private final int score;


        public ScoredMovie(
                Movie movie,
                int score) {

            this.movie = movie;
            this.score = score;
        }


        public Movie getMovie() {

            return movie;
        }


        public int getScore() {

            return score;
        }
    }
}
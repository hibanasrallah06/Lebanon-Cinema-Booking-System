package cinemasystem.service;

import cinemasystem.model.Movie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class AIRecommendationService {


    private final OllamaService ollamaService =
            new OllamaService();


    // =========================================================
    // RECOMMEND MOVIE IDS
    // =========================================================

    public List<String> getRecommendationIds(
            String userRequest,
            List<Movie> movies) throws Exception {


        if (userRequest == null ||
                userRequest.trim().isEmpty()) {

            return new ArrayList<>();
        }


        if (movies == null ||
                movies.isEmpty()) {

            return new ArrayList<>();
        }


        // =====================================================
        // BUILD MOVIE DATABASE FOR AI
        // =====================================================

        String movieData =
                buildMovieData(
                        movies
                );


        // =====================================================
        // AI PROMPT
        // =====================================================

        String prompt =

                "You are an intelligent movie "
                + "recommendation AI inside a cinema "
                + "booking system.\n\n"

                + "The user will describe what kind "
                + "of movie they want. "
                + "Understand the meaning of the request, "
                + "including casual language, Arabic "
                + "transliteration, English, and mixed language.\n\n"

                + "For example:\n"
                + "- 'bade film bdahek' means the user wants "
                + "a funny/comedy movie.\n"
                + "- 'bade shi scary' means horror/scary.\n"
                + "- 'bade action' means action.\n"
                + "- 'بدي فيلم رومانسي' means romance.\n\n"

                + "USER REQUEST:\n"
                + userRequest.trim()
                + "\n\n"

                + "AVAILABLE MOVIES IN THE DATABASE:\n"
                + movieData
                + "\n\n"

                + "IMPORTANT RULES:\n"

                + "1. Recommend ONLY movies from the "
                + "database above.\n"

                + "2. NEVER invent a movie.\n"

                + "3. Match the meaning of the user's "
                + "request, not only exact words.\n"

                + "4. Consider title, genre, language, "
                + "duration, age rating, and description.\n"

                + "5. Return maximum 3 movies.\n"

                + "6. If several movies match, choose "
                + "the strongest matches.\n"

                + "7. If nothing really matches, return "
                + "an empty list.\n\n"

                + "OUTPUT FORMAT:\n"
                + "MOVIE_IDS: id1,id2,id3\n\n"

                + "Return ONLY the MOVIE_IDS line. "
                + "Do not explain anything else.";


        // =====================================================
        // ASK LOCAL AI
        // =====================================================

        String aiResponse =
                ollamaService.generate(
                        prompt
                );


        // =====================================================
        // PARSE IDS
        // =====================================================

        List<String> ids =
                parseMovieIds(
                        aiResponse
                );


        // =====================================================
        // SECURITY / VALIDATION
        // =====================================================

        return validateMovieIds(
                ids,
                movies
        );
    }


    // =========================================================
    // BUILD MOVIE DATA
    // =========================================================

    private String buildMovieData(
            List<Movie> movies) {


        StringBuilder builder =
                new StringBuilder();


        for (Movie movie :
                movies) {


            builder.append(
                    "ID="
            ).append(
                    movie.getMovieId()
            );


            builder.append(
                    " | Title="
            ).append(
                    safe(
                            movie.getTitle()
                    )
            );


            builder.append(
                    " | Genre="
            ).append(
                    safe(
                            movie.getGenre()
                    )
            );


            builder.append(
                    " | Language="
            ).append(
                    safe(
                            movie.getLanguage()
                    )
            );


            builder.append(
                    " | Duration="
            ).append(
                    movie.getDuration()
            ).append(
                    " minutes"
            );


            builder.append(
                    " | Age="
            ).append(
                    safe(
                            movie.getAgeRating()
                    )
            );


            builder.append(
                    " | Description="
            ).append(
                    safe(
                            movie.getDescription()
                    )
            );


            builder.append(
                    "\n"
            );
        }


        return builder.toString();
    }


    // =========================================================
    // PARSE MOVIE IDS
    // =========================================================

    private List<String> parseMovieIds(
            String response) {


        List<String> ids =
                new ArrayList<>();


        if (response == null ||
                response.trim().isEmpty()) {

            return ids;
        }


        String text =
                response.trim();


        /*
         * We specifically look for:
         *
         * MOVIE_IDS: 1,2,3
         */
        int index =
                text.toUpperCase()
                        .indexOf(
                                "MOVIE_IDS"
                        );


        if (index == -1) {

            return ids;
        }


        int colon =
                text.indexOf(
                        ":",
                        index
                );


        if (colon == -1) {

            return ids;
        }


        String idPart =
                text.substring(
                        colon + 1
                );


        /*
         * Remove everything after
         * a possible new line.
         */
        int newline =
                idPart.indexOf(
                        "\n"
                );


        if (newline != -1) {

            idPart =
                    idPart.substring(
                            0,
                            newline
                    );
        }


        idPart =
                idPart
                        .replace(
                                "[",
                                ""
                        )
                        .replace(
                                "]",
                                ""
                        )
                        .replace(
                                "\"",
                                ""
                        )
                        .trim();


        if (idPart.isEmpty()) {

            return ids;
        }


        String[] parts =
                idPart.split(",");


        for (String part :
                parts) {


            String id =
                    part.trim();


            /*
             * Movie IDs in this project
             * are expected to be numeric.
             */
            if (id.matches(
                    "\\d+"
            )) {

                if (!ids.contains(id)) {

                    ids.add(id);
                }
            }


            /*
             * Maximum 3.
             */
            if (ids.size() >= 3) {

                break;
            }
        }


        return ids;
    }


    // =========================================================
    // VALIDATE IDS AGAINST DATABASE
    // =========================================================

    private List<String> validateMovieIds(
            List<String> ids,
            List<Movie> movies) {


        List<String> validIds =
                new ArrayList<>();


        Set<String> databaseIds =
                new HashSet<>();


        for (Movie movie :
                movies) {

            databaseIds.add(
                    String.valueOf(
                            movie.getMovieId()
                    )
            );
        }


        for (String id :
                ids) {

            if (databaseIds.contains(
                    id
            )) {

                validIds.add(id);
            }


            if (validIds.size() >= 3) {

                break;
            }
        }


        return validIds;
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value) {


        if (value == null ||
                value.trim().isEmpty()) {

            return "Not specified";
        }


        return value
                .replace(
                        "\n",
                        " "
                )
                .replace(
                        "\r",
                        " "
                );
    }
}
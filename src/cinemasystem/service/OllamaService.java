package cinemasystem.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


/**
 * =========================================================
 * OLLAMA SERVICE
 * =========================================================
 *
 * Free local AI using Ollama.
 *
 * Model:
 *      llama3.2
 *
 * URL:
 *      http://localhost:11434/api/generate
 *
 * No API key.
 * No OpenAI.
 * No payment.
 */
public class OllamaService {


    // =========================================================
    // CONFIGURATION
    // =========================================================

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/generate";


    private static final String MODEL =
            "llama3.2";


    // =========================================================
    // HTTP CLIENT
    // =========================================================

    private final HttpClient client =
            HttpClient.newHttpClient();


    // =========================================================
    // GENERATE
    // =========================================================

    /**
     * Sends a prompt to Ollama and returns
     * the AI response text.
     *
     * This is the method used by:
     *
     * AIRecommendationService
     *
     * through:
     *
     * ollamaService.generate(prompt)
     */
    public String generate(
            String prompt) throws Exception {


        // =====================================================
        // VALIDATE PROMPT
        // =====================================================

        if (prompt == null ||
                prompt.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "AI prompt cannot be empty."
            );
        }


        // =====================================================
        // ESCAPE PROMPT
        // =====================================================

        String escapedPrompt =
                escapeJson(prompt);


        // =====================================================
        // OLLAMA REQUEST JSON
        // =====================================================

        String json =

                "{"
                + "\"model\":\""
                + MODEL
                + "\","

                + "\"prompt\":\""
                + escapedPrompt
                + "\","

                + "\"stream\":false,"

                + "\"options\":{"
                + "\"temperature\":0.1,"
                + "\"num_predict\":100"
                + "}"

                + "}";


        // =====================================================
        // CREATE HTTP REQUEST
        // =====================================================

        HttpRequest request =
                HttpRequest.newBuilder()

                        .uri(
                                URI.create(
                                        OLLAMA_URL
                                )
                        )

                        .header(
                                "Content-Type",
                                "application/json"
                        )

                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )

                        .build();


        // =====================================================
        // SEND REQUEST
        // =====================================================

        HttpResponse<String> response =
                client.send(

                        request,

                        HttpResponse.BodyHandlers
                                .ofString()
                );


        // =====================================================
        // CHECK HTTP STATUS
        // =====================================================

        if (response.statusCode() < 200 ||
                response.statusCode() >= 300) {

            throw new IOException(

                    "Ollama API Error: "
                    + response.statusCode()
                    + "\n"
                    + response.body()
            );
        }


        // =====================================================
        // EXTRACT RESPONSE
        // =====================================================

        return extractResponseText(
                response.body()
        );
    }


    // =========================================================
    // EXTRACT OLLAMA RESPONSE
    // =========================================================

    /**
     * Ollama normally returns:
     *
     * {
     *   "model":"llama3.2",
     *   "response":"[\"1\",\"3\"]",
     *   "done":true
     * }
     *
     * We only need the value of "response".
     */
    private String extractResponseText(
            String json) throws IOException {


        if (json == null ||
                json.trim().isEmpty()) {

            return "";
        }


        String marker =
                "\"response\"";


        int markerIndex =
                json.indexOf(marker);


        if (markerIndex == -1) {

            throw new IOException(
                    "Ollama response does not contain a response field.\n"
                    + json
            );
        }


        int colonIndex =
                json.indexOf(
                        ":",
                        markerIndex
                );


        if (colonIndex == -1) {

            throw new IOException(
                    "Invalid Ollama response."
            );
        }


        int firstQuote =
                json.indexOf(
                        "\"",
                        colonIndex + 1
                );


        if (firstQuote == -1) {

            throw new IOException(
                    "Invalid Ollama response format."
            );
        }


        int closingQuote =
                findClosingQuote(
                        json,
                        firstQuote + 1
                );


        if (closingQuote == -1) {

            throw new IOException(
                    "Could not read Ollama response."
            );
        }


        String result =
                json.substring(
                        firstQuote + 1,
                        closingQuote
                );


        return unescapeJson(
                result
        ).trim();
    }


    // =========================================================
    // FIND CLOSING QUOTE
    // =========================================================

    private int findClosingQuote(
            String text,
            int start) {


        boolean escaped =
                false;


        for (
                int i = start;
                i < text.length();
                i++
        ) {


            char c =
                    text.charAt(i);


            if (
                    c == '"'
                    && !escaped
            ) {

                return i;
            }


            if (
                    c == '\\'
                    && !escaped
            ) {

                escaped = true;

            } else {

                escaped = false;
            }
        }


        return -1;
    }


    // =========================================================
    // ESCAPE JSON
    // =========================================================

    private String escapeJson(
            String value) {


        if (value == null) {

            return "";
        }


        return value

                .replace(
                        "\\",
                        "\\\\"
                )

                .replace(
                        "\"",
                        "\\\""
                )

                .replace(
                        "\r",
                        "\\r"
                )

                .replace(
                        "\n",
                        "\\n"
                )

                .replace(
                        "\t",
                        "\\t"
                );
    }


    // =========================================================
    // UNESCAPE JSON
    // =========================================================

    private String unescapeJson(
            String value) {


        if (value == null) {

            return "";
        }


        return value

                .replace(
                        "\\n",
                        "\n"
                )

                .replace(
                        "\\r",
                        "\r"
                )

                .replace(
                        "\\t",
                        "\t"
                )

                .replace(
                        "\\\"",
                        "\""
                )

                .replace(
                        "\\\\",
                        "\\"
                );
    }
}
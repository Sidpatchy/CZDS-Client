package com.sidpatchy.czdsclient.IO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidpatchy.czdsclient.Bean.AuthResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HttpConnectionManager {

    private final Logger logger = LogManager.getLogger();
    private String token;
    private final String username;
    private final String password;
    private final String baseAuthenticationEndpoint;
    private final String baseEndpoint;
    private final ObjectMapper objectMapper;

    /**
     * Constructs an instance of HttpConnectionManager with the provided parameters.
     *
     * @param username the username to be used for authentication
     * @param password the password to be used for authentication
     * @param baseAuthenticationEndpoint the base URL for the authentication endpoint
     * @param baseEndpoint the base URL for the main API endpoint
     */
    public HttpConnectionManager(String username, String password, String baseAuthenticationEndpoint, String baseEndpoint) {
        this.username = username;
        this.password = password;
        this.baseAuthenticationEndpoint = baseAuthenticationEndpoint;
        this.baseEndpoint = baseEndpoint;
        this.objectMapper = new ObjectMapper(); // Customize the ObjectMapper instance if needed
        this.token = null;
    }

    /**
     * Authenticates the user by sending a POST request to the authentication endpoint with the user's credentials.
     * If authentication is successful, the retrieved token is stored for future use.
     *
     * @throws Exception If an error occurs while attempting to authenticate or if the authentication fails.
     */
    public void authenticate() throws Exception {
        if (token != null) {
            return;
        }

        URL url = new URI(baseAuthenticationEndpoint + "/api/authenticate/").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String authPayload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        logger.debug("Auth Payload: " + authPayload);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = authPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = connection.getResponseCode();
        logger.debug("Response Code: " + status);

        if (status == HttpURLConnection.HTTP_OK) {
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                AuthResponse authResponse = objectMapper.readValue(reader, AuthResponse.class);
                this.token = authResponse.getAccessToken();
            }
        } else if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new RuntimeException("Invalid username or password.");
        } else {
            try (InputStreamReader reader = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)) {
                String errorResponse = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
                logger.error("Authentication failed with HTTP status: " + status + ". Error: " + errorResponse);
                throw new RuntimeException("Authentication failed with HTTP status: " + status + ". Error: " + errorResponse);
            }
        }
    }

    /**
     * Sets up an HttpURLConnection object with the specified URL and HTTP method.
     *
     * @param url The URL to connect to.
     * @param method The HTTP method to use (e.g., "GET", "POST", "PUT", "PATCH").
     * @return An HttpURLConnection object configured with the specified URL and method.
     * @throws Exception If an error occurs during connection setup.
     */
    private HttpURLConnection setupConnection(URL url, String method) throws Exception {
        authenticate(); // Ensure we're authenticated before making a request

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + this.token);
        connection.setConnectTimeout(10000); // 10 seconds connect timeout
        connection.setReadTimeout(10000); // 10 seconds read timeout
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            connection.setDoOutput(true); // Necessary for POST, PUT, and PATCH
        }
        return connection;
    }

    /**
     * Executes a GET request to the specified endpoint and returns the response deserialized into the specified type.
     *
     * @param endpoint The endpoint to which the GET request should be sent.
     * @param typeOfT The class type of the response expected.
     * @param <T> The type of the response object.
     * @return A CompletableFuture that, when completed, will contain the response deserialized into the specified type.
     */
    public <T> CompletableFuture<T> get(String endpoint, Class<T> typeOfT) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeRequestWithRetry("GET", endpoint, null, typeOfT);
            } catch (Exception e) {
                logger.error("Error during GET request", e);
                throw new RuntimeException(e);
            }
        }, Executors.newCachedThreadPool());
    }

    /**
     * Sends a POST request to the specified endpoint with the given data and returns the response as an instance of the specified type.
     *
     * @param <T> The type of the response object.
     * @param endpoint The endpoint to which the POST request is sent.
     * @param data The data to be sent with the POST request.
     * @param typeOfT The class type of the response expected.
     * @return A CompletableFuture that will contain the response of the request, deserialized into the specified type.
     */
    public <T> CompletableFuture<T> post(String endpoint, Object data, Class<T> typeOfT) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeRequestWithRetry("POST", endpoint, data, typeOfT);
            } catch (Exception e) {
                logger.error("Error during POST request", e);
                throw new RuntimeException(e);
            }
        }, Executors.newCachedThreadPool());
    }

    /**
     * Sends an HTTP PUT request to the specified endpoint with the given data and returns a response of the specified type asynchronously.
     *
     * @param endpoint The endpoint to which the PUT request should be sent.
     * @param data The data to be sent in the request body.
     * @param typeOfT The class type of the response expected.
     * @return A CompletableFuture representing the pending completion of the request. The result is the response deserialized into the specified type.
     */
    public <T> CompletableFuture<T> put(String endpoint, Object data, Class<T> typeOfT) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeRequestWithRetry("PUT", endpoint, data, typeOfT);
            } catch (Exception e) {
                logger.error("Error during PUT request", e);
                throw new RuntimeException(e);
            }
        }, Executors.newCachedThreadPool());
    }

    /**
     * Initiates a DELETE request to the specified endpoint.
     *
     * @param endpoint The endpoint to which the DELETE request is to be sent.
     * @return A CompletableFuture that resolves to true if the request was successful (HTTP status 200),
     *         or false otherwise.
     */
    public CompletableFuture<Boolean> delete(String endpoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(baseEndpoint + endpoint);
                HttpURLConnection connection = setupConnection(url, "DELETE");

                int status = connection.getResponseCode();
                return status == HttpURLConnection.HTTP_OK;
            } catch (Exception e) {
                logger.error("Error during DELETE request", e);
                throw new RuntimeException(e);
            }
        }, Executors.newCachedThreadPool());
    }

    /**
     * Executes an HTTP PATCH request asynchronously to the specified endpoint with the provided data.
     *
     * @param <T> The type of the response object.
     * @param endpoint The endpoint to which the request should be sent.
     * @param data The data to be sent in the request body.
     * @param typeOfT The class type of the response object.
     * @return A CompletableFuture representing the pending result of the PATCH request.
     */
    public <T> CompletableFuture<T> patch(String endpoint, Object data, Class<T> typeOfT) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeRequestWithRetry("PATCH", endpoint, data, typeOfT);
            } catch (Exception e) {
                logger.error("Error during PATCH request", e);
                throw new RuntimeException(e);
            }
        }, Executors.newCachedThreadPool());
    }

    /**
     * Executes an HTTP request with a retry mechanism in case of an unauthorized response.
     *
     * @param method The HTTP method to be used (e.g., GET, POST).
     * @param endpoint The endpoint to which the request is to be sent.
     * @param data The data to be sent with the request.
     * @param typeOfT The class type of the response expected.
     * @return The response of the request, deserialized into the specified type.
     * @throws Exception If an error occurs during the request or retry process.
     */
    private <T> T executeRequestWithRetry(String method, String endpoint, Object data, Class<T> typeOfT) throws Exception {
        try {
            return executeRequest(method, endpoint, data, typeOfT);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("HTTP error 401")) { // Check for unauthorized response
                token = null; // Clear the token
                authenticate(); // Re-authenticate
                return executeRequest(method, endpoint, data, typeOfT); // Retry the request
            } else {
                throw e;
            }
        }
    }

    /**
     * Executes an HTTP request to the specified endpoint using the provided method and data.
     * Parses the response into the specified type.
     *
     * @param <T> The type of the response object.
     * @param method The HTTP method to use (e.g., "GET", "POST").
     * @param endpoint The endpoint to which the request should be sent.
     * @param data The data to be sent in the request body (for methods like POST, PUT, PATCH).
     * @param typeOfT The class type of the response object.
     * @return The parsed response of type T.
     * @throws Exception If there is an error during the request.
     */
    private <T> T executeRequest(String method, String endpoint, Object data, Class<T> typeOfT) throws Exception {
        URL url = new URL(baseEndpoint + endpoint);
        HttpURLConnection connection = setupConnection(url, method);

        if (data != null) {
            String jsonInput = objectMapper.writeValueAsString(data);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP error " + status);
        }

        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return objectMapper.readValue(reader, typeOfT);
        }
    }

    /**
     * Downloads a file from the specified URL and saves it to the given output directory.
     *
     * @param downloadURL The URL from which to download the file.
     * @param outputDirectoryPath The directory path where the downloaded file will be saved.
     * @return The downloaded file.
     * @throws Exception If there is an error during the download process.
     */
    public File downloadFile(String downloadURL, String outputDirectoryPath) throws Exception {
        logger.info(downloadURL);

        authenticate(); // Ensure authentication before making the request

        URL url = new URI(downloadURL).toURL();
        HttpURLConnection connection = setupConnection(url, "GET");

        int fileLength = connection.getContentLength();
        if (fileLength == -1) {
            logger.warn("File size is unknown. Progress will not be shown.");
        }

        // Extract the file name from the URL
        String fileName = new File(url.getPath()).getName() + ".gz";

        // Combine the output directory and file name
        File outputDirectory = new File(outputDirectoryPath);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs(); // Create the directory if it doesn't exist
        }
        File outputFile = new File(outputDirectory, fileName);

        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (fileLength > 0) { // Show progress only if file size is known
                    int progress = (int) (totalBytesRead * 100 / fileLength);
                    System.out.print("\rDownloading: " + progress + "%");
                } else {
                    System.out.print("\rDownloading: " + totalBytesRead + " bytes");
                }
            }

            System.out.println("\nDownload completed successfully.");
            return outputFile;
        } catch (Exception e) {
            logger.error("Error during file download", e);
            throw new RuntimeException("File download failed", e);
        }
    }

    /**
     * Retrieves the current authentication token. Mostly present for debug purposes.
     *
     * @return the current authentication token as a String.
     */
    public String getToken() {
        return token;
    }
}
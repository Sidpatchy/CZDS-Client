package com.sidpatchy.czdsclient.IO;

import com.sidpatchy.czdsclient.Module.DownloadModule;

public class CZDSClient {

    private final HttpConnectionManager connectionManager;
    private final DownloadModule downloadModule;

    private final String username;
    private final String password;

    private final String baseAuthenticationEndpoint;
    private final String baseEndpoint;

    /**
     * Constructs a CZDSClient with the specified username and password. Uses default API paths.
     *
     * @param username the username to be used for authentication
     * @param password the password to be used for authentication
     */
    public CZDSClient(String username, String password) {
        this(username, password, "https://account-api.icann.org", "https://czds-api.icann.org");
    }

    /**
     * Constructs a CZDSClient with the specified username, password,
     * base authentication endpoint, and base endpoint.
     *
     * @param username the username to be used for authentication
     * @param password the password to be used for authentication
     * @param baseAuthenticationEndpoint the base URL for the authentication endpoint
     * @param baseEndpoint the base URL for the main CZDS API endpoint
     */
    public CZDSClient(String username, String password, String baseAuthenticationEndpoint, String baseEndpoint) {
        this.username = username;
        this.password = password;

        this.baseAuthenticationEndpoint = baseAuthenticationEndpoint;
        this.baseEndpoint = baseEndpoint;

        this.connectionManager = new HttpConnectionManager(username, password, baseAuthenticationEndpoint, baseEndpoint);

        this.downloadModule = new DownloadModule(connectionManager, baseEndpoint);
    }

    /**
     * Returns the DownloadModule instance associated with this CZDSClient.
     *
     * @return the DownloadModule instance.
     */
    public DownloadModule getDownloader() {
        return downloadModule;
    }

    /**
     * Returns the HttpConnectionManager instance associated with this CZDSClient. This is mostly present for debug
     * purposes.
     *
     * @return the HttpConnectionManager instance.
     */
    public HttpConnectionManager getConnectionManager() {
        return connectionManager;
    }
}

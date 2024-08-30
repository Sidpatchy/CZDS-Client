package com.sidpatchy.czdsclient.Module;

import com.sidpatchy.czdsclient.IO.HttpConnectionManager;
import com.sidpatchy.czdsclient.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DownloadModule {
    private final HttpConnectionManager connectionManager;
    private final String baseEndpoint;
    private String downloadPath = "./Downloads/";

    private Logger logger = LogManager.getLogger(DownloadModule.class);

    /**
     * Initializes a new instance of the DownloadModule.
     *
     * @param connectionManager The HttpConnectionManager responsible for managing connections.
     * @param baseEndpoint The base endpoint URL for the download service.
     */
    public DownloadModule(HttpConnectionManager connectionManager, String baseEndpoint) {
        this.connectionManager = connectionManager;
        this.baseEndpoint = baseEndpoint;
    }

    /**
     * Retrieves the download URLs for approved zone files.
     *
     * @return A CompletableFuture that, when completed, will contain a List of URLs as Strings
     *         where the approved zone files can be downloaded.
     */
    private CompletableFuture<List> getApprovedZoneDownloadUrls() {
        // Specify the endpoint for downloading the links
        String endpoint = "/czds/downloads/links";

        // Call the get method with the expected response type, which in this case would likely be a String
        return connectionManager.get(endpoint, List.class);
    }

    /**
     * Retrieves the list of approved Top-Level Domains (TLDs).
     *
     * @return A CompletableFuture that, when completed, will contain a List of Strings representing the approved TLDs.
     */
    public CompletableFuture<List<String>> getApprovedTLDs() {
        return getApprovedZoneDownloadUrls().thenApply(urls -> {
            List<String> tlds = new ArrayList<>();
            for (Object url : urls) {
                String urlString = (String) url;
                String[] parts = urlString.split("/");
                String zone = parts[parts.length - 1].replace(".zone", "");
                tlds.add(zone);
            }
            return tlds;
        });
    }

    /**
     * Download all approved zone files.
     *
     * @return A list of downloaded files.
     */
    public CompletableFuture<List<File>> downloadAllApprovedZoneFiles() {
        return getApprovedZoneDownloadUrls()
                .thenApply(zones -> {
                    logger.debug(zones.toString());

                    if (zones.isEmpty()) {
                        throw new RuntimeException("You are not authorized to download any TLDs!");
                    }

                    List<File> files = new ArrayList<>();
                    for (Object zone : zones) {
                        try {
                            files.add(connectionManager.downloadFile(zone.toString(), downloadPath));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return files;
        });
    }

    /**
     * Download a specific zone file by its TLD asynchronously.
     * @param tld The TLD for the zone file to download.
     * @return A CompletableFuture with the downloaded file.
     */
    public CompletableFuture<File> downloadZoneFile(String tld) {
        return getApprovedZoneDownloadUrls().thenApply(zones -> {
            logger.debug(zones.toString());

            File file = null;
            boolean foundMatch = false;
            for (String zone : (List<String>) zones) {
                String[] parts = zone.split("/");
                if (parts[parts.length - 1].contains(tld + ".zone")) {
                    try {
                        file = connectionManager.downloadFile(zone, downloadPath);
                        foundMatch = true;
                        break;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (!foundMatch) {
                throw new RuntimeException("You are not authorized to download the '." + tld + "' TLD!");
            }

            return file;
        });
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }
}

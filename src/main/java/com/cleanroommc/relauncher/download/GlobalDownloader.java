package com.cleanroommc.relauncher.download;

import com.cleanroommc.relauncher.CleanroomRelauncher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

public final class GlobalDownloader {

    public static final GlobalDownloader INSTANCE = new GlobalDownloader();

    private final List<ForkJoinTask> downloads = new ArrayList<>();

    public static List<FugueRelease> fetchFugueReleases() {
        CleanroomRelauncher.LOGGER.info("Fetching Fugue releases from GitHub...");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/CleanroomMC/Fugue/releases")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                CleanroomRelauncher.LOGGER.error("Failed to fetch Fugue releases: " + response.message());
                return Collections.emptyList();
            }

            String json = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            List<FugueRelease> releases = new ArrayList<>();
            for (JsonNode releaseNode : rootNode) {
                String tagName = releaseNode.get("tag_name").asText();
                JsonNode assetsNode = releaseNode.get("assets");
                if (assetsNode != null && assetsNode.isArray()) {
                    for (JsonNode assetNode : assetsNode) {
                        String assetName = assetNode.get("name").asText();
                        if (assetName.endsWith(".jar")) {
                            String downloadUrl = assetNode.get("browser_download_url").asText();
                            releases.add(new FugueRelease(tagName, downloadUrl));
                            break; // Assuming one JAR per release is sufficient
                        }
                    }
                }
            }
            CleanroomRelauncher.LOGGER.info("Successfully fetched {} Fugue releases.", releases.size());
            return releases;
        } catch (IOException e) {
            CleanroomRelauncher.LOGGER.error("Error fetching Fugue releases: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    public void from(String source, File destination) {
        URI uri;
        URL url;
        try {
            uri = URI.create(source);
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("Unable to construct url %s", source), e);
        }
        this.downloads.add(ForkJoinPool.commonPool().submit(() -> {
            try {
                FileUtils.copyURLToFile(url, destination);
                CleanroomRelauncher.LOGGER.debug("Downloaded {} to {}", uri.toString(), destination.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(String.format("Unable to download %s to %s", url, destination), e);
            }
        }));
    }

    public void immediatelyFrom(String source, File destination) {
        this.from(source, destination);
        try {
            this.downloads.remove(this.downloads.size() - 1).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to complete download", e);
        }
    }

    public void blockUntilFinished() {
        int total = this.downloads.size();
        int completed = 0;
        int last = 0;
        for (Future download : this.downloads) {
            try {
                download.get();
                completed++;
                int percentage = (completed * 100) / total;
                if (percentage % 10 == 0 && last != percentage) {
                    last = percentage;
                    CleanroomRelauncher.LOGGER.info("Download Progress: {} / {} | {}% completed.", completed, total, percentage);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Unable to complete download", e);
            }
        }
    }

}

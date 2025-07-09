package com.cleanroommc.relauncher.download;

import com.cleanroommc.relauncher.download.cache.CleanroomCache;

import java.io.IOException;

public interface CleanroomZipArtifact {

    void install(String url) throws IOException;

    void extract(CleanroomCache cache) throws IOException;

}

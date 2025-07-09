package com.cleanroommc.relauncher.download;

import com.cleanroommc.relauncher.download.cache.CleanroomCache;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class CleanroomMultiMcPack implements CleanroomZipArtifact {

    public static CleanroomMultiMcPack of(String version, Path location) {
        return new CleanroomMultiMcPack(version, location);
    }

    private final String version;
    private final Path location;

    private CleanroomMultiMcPack(String version, Path location) {
        this.version = version;
        this.location = location;
    }

    @Override
    public void install(String url) throws IOException {
        if (!Files.exists(this.location)) {
            GlobalDownloader.INSTANCE.immediatelyFrom(url, this.location.toFile());
        }
    }

    @Override
    public void extract(CleanroomCache cache) throws IOException {
        try (FileSystem jar = FileSystems.newFileSystem(this.location, null)) {
            Files.copy(jar.getPath("/patches/net.minecraft.json"), cache.getDirectory().resolve("net.minecraft.json"));
            Files.copy(jar.getPath("/patches/net.minecraftforge.json"), cache.getDirectory().resolve("net.minecraftforge.json"));
            Files.copy(jar.getPath("/patches/org.lwjgl3.json"), cache.getDirectory().resolve("org.lwjgl3.json"));
            try (Stream<Path> stream = Files.walk(jar.getPath("/libraries/"))) {
                Files.copy(stream.filter(Files::isRegularFile).findFirst().get(), cache.getUniversalJar());
            }
        }
    }

}

package com.cleanroommc.relauncher.download;

import java.util.Objects;

public class FugueRelease {
    public final String name;
    public final String downloadUrl;

    public FugueRelease(String name, String downloadUrl) {
        this.name = name;
        this.downloadUrl = downloadUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FugueRelease that = (FugueRelease) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(downloadUrl, that.downloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, downloadUrl);
    }

    @Override
    public String toString() {
        return name;
    }
}

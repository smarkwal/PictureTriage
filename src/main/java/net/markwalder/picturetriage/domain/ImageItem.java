package net.markwalder.picturetriage.domain;

import java.nio.file.Path;

public record ImageItem(Path path) {
    public String displayName() {
        return path.getFileName().toString();
    }
}

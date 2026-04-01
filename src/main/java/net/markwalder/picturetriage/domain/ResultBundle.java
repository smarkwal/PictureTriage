package net.markwalder.picturetriage.domain;

import java.util.List;

public record ResultBundle(
    List<ImageItem> keptImages,
    List<ImageItem> rankedTriageImages,
    List<ImageItem> deletedImages
) {
}

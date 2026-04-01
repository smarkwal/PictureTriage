package net.markwalder.picturetriage.service;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.ResultBundle;

import java.io.PrintStream;
import java.util.List;

public class ResultsPrinter {
    public void print(ResultBundle result, PrintStream out) {
        out.println("Final picture order:");
        out.println("1) Kept in phase 1");
        printList(result.keptImages(), out);
        out.println();

        out.println("2) Ranked in phase 2");
        printList(result.rankedTriageImages(), out);
        out.println();

        out.println("3) Deleted in phase 1");
        printList(result.deletedImages(), out);
        out.println();
    }

    private void printList(List<ImageItem> items, PrintStream out) {
        if (items.isEmpty()) {
            out.println("   (none)");
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            out.printf("   %d. %s%n", i + 1, items.get(i).path());
        }
    }
}

package org.accula.analyzer.checkers.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClonePair {
    private float metric;
    private int counter;
    private final Clone first;
    private final Clone second;

    public void incCounter() {
        this.counter++;
    }

    public float getNormalizedMetric() {
        return counter == 0 ? metric : metric / counter;
    }
}

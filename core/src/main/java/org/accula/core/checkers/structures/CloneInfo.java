package org.accula.core.checkers.structures;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class CloneInfo {
    private float metric;
    private int count;
    private Set<Interval> linesFromFirstFile;
    private Set<Interval> linesFromSecondFile;

    public float getNormalizedMetric() {
        if (linesFromSecondFile.size() != 0)
            return metric / count;
        else return metric;
    }
}

package org.whitedoggy.maplehistory.combat;

import java.util.Map;

public record SourceTotals(
        Map<String, Double> percentAppliedFlat,
        Map<String, Double> percentNotAppliedFlat,
        Map<String, Double> percent
) {
}

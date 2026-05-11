package org.whitedoggy.maplehistory.combat;

import java.util.Map;

public record StatTotals(
        Map<String, Long> percentAppliedFlat,
        Map<String, Long> percentNotAppliedFlat,
        Map<String, Double> percent
) {
}

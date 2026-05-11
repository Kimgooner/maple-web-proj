package org.whitedoggy.maplehistory.combat;

public record StatContribution(
        String endpoint,
        String label,
        String rawValue,
        String statKey,
        String bucket,
        double parsedValue
) {
}

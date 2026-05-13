package org.whitedoggy.maplehistory.combat;

public record CombatPowerTotalChange(
        String bucket,
        String statKey,
        String statName,
        String beforeValue,
        String afterValue,
        String deltaValue
) {
}

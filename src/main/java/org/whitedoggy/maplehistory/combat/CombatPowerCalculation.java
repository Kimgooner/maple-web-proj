package org.whitedoggy.maplehistory.combat;

public record CombatPowerCalculation(
        long combatPower,
        long mainStat,
        long subStat,
        long attackPower,
        double damageFactor,
        double criticalDamageFactor,
        double finalDamageFactor,
        String mainStatSource,
        String subStatSource
) {
}

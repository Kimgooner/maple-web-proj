package org.whitedoggy.maplehistory.combat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CombatPowerFormula {

    public CombatPowerCalculation calculate(String characterClass, CombatStatBag stats) {
        return calculate(characterClass, null, stats);
    }

    public CombatPowerCalculation calculate(String characterClass, Integer characterLevel, CombatStatBag stats) {
        CharacterStatProfile profile = CharacterStatProfile.from(characterClass);
        long mainBase = base(stats, profile.mainStats()) + estimatedApMainStat(profile, characterLevel);
        long subBase = base(stats, profile.subStats()) + estimatedApSubStat(profile, characterLevel);

        long mainStat = (long) Math.floor(mainBase * (1 + percent(stats, profile.mainStats()) / 100.0d)
                + nonPercent(stats, profile.mainStats()));
        long subStat = (long) Math.floor(subBase * (1 + percent(stats, profile.subStats()) / 100.0d)
                + nonPercent(stats, profile.subStats()));

        long attackBase = profile.usesMagicAttack()
                ? stats.flat(CombatStatKey.MAGIC_ATTACK)
                : Math.max(stats.flat(CombatStatKey.ATTACK_POWER), stats.flat(CombatStatKey.MAGIC_ATTACK));
        double attackPercent = profile.usesMagicAttack()
                ? stats.percent(CombatStatKey.MAGIC_ATTACK)
                : Math.max(stats.percent(CombatStatKey.ATTACK_POWER), stats.percent(CombatStatKey.MAGIC_ATTACK));
        long attack = (long) Math.floor(attackBase * (1 + attackPercent / 100.0d));

        double statFactor = (mainStat * 4.0d + subStat) / 100.0d;
        double damageFactor = 1 + (stats.percent(CombatStatKey.DAMAGE) + stats.percent(CombatStatKey.BOSS_DAMAGE)) / 100.0d;
        double criticalFactor = 1.35d + stats.percent(CombatStatKey.CRITICAL_DAMAGE) / 100.0d;
        double finalDamageFactor = 1 + stats.percent(CombatStatKey.FINAL_DAMAGE) / 100.0d;
        BigDecimal combatPower = BigDecimal.valueOf(statFactor)
                .multiply(BigDecimal.valueOf(attack))
                .multiply(BigDecimal.valueOf(damageFactor))
                .multiply(BigDecimal.valueOf(criticalFactor))
                .multiply(BigDecimal.valueOf(finalDamageFactor))
                .setScale(0, RoundingMode.DOWN);

        return new CombatPowerCalculation(
                combatPower.longValue(),
                mainStat,
                subStat,
                attack,
                round(damageFactor),
                round(criticalFactor),
                round(finalDamageFactor),
                profile.mainStats().toString(),
                profile.subStats().toString()
        );
    }

    private static long base(CombatStatBag stats, Set<CombatStatKey> keys) {
        long base = stats.flat(CombatStatKey.ALL_STAT);
        for (CombatStatKey key : keys) {
            base += stats.flat(key);
        }
        return base;
    }

    private static long nonPercent(CombatStatBag stats, Set<CombatStatKey> keys) {
        long value = stats.finalFlat(CombatStatKey.ALL_STAT);
        for (CombatStatKey key : keys) {
            value += stats.finalFlat(key);
        }
        return value;
    }

    private static double percent(CombatStatBag stats, Set<CombatStatKey> keys) {
        double value = stats.percent(CombatStatKey.ALL_STAT);
        for (CombatStatKey key : keys) {
            value += stats.percent(key);
        }
        return value;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    private static long estimatedApMainStat(CharacterStatProfile profile, Integer characterLevel) {
        if (characterLevel == null || characterLevel < 1 || profile.mainStats().contains(CombatStatKey.MAX_HP)) {
            return 0L;
        }
        if (profile.mainStats().size() > 1) {
            return 0L;
        }
        return 5L * characterLevel + 18L;
    }

    private static long estimatedApSubStat(CharacterStatProfile profile, Integer characterLevel) {
        if (characterLevel == null || characterLevel < 1 || profile.subStats().contains(CombatStatKey.MAX_HP)) {
            return 0L;
        }
        return 4L;
    }
}

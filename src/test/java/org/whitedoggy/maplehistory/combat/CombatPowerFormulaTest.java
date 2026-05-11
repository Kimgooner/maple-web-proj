package org.whitedoggy.maplehistory.combat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CombatPowerFormulaTest {

    private final CombatPowerFormula formula = new CombatPowerFormula();

    @Test
    void calculatesCombatPowerWithMapleFormula() {
        CombatStatBag stats = new CombatStatBag();
        stats.addFlat(CombatStatKey.STR, 1000);
        stats.addPercent(CombatStatKey.STR, 100);
        stats.addFlat(CombatStatKey.DEX, 200);
        stats.addPercent(CombatStatKey.DEX, 50);
        stats.addFlat(CombatStatKey.ATTACK_POWER, 100);
        stats.addPercent(CombatStatKey.ATTACK_POWER, 20);
        stats.addPercent(CombatStatKey.DAMAGE, 50);
        stats.addPercent(CombatStatKey.BOSS_DAMAGE, 100);
        stats.addPercent(CombatStatKey.CRITICAL_DAMAGE, 50);

        CombatPowerCalculation result = formula.calculate("히어로", stats);

        assertThat(result.mainStat()).isEqualTo(2000);
        assertThat(result.subStat()).isEqualTo(300);
        assertThat(result.attackPower()).isEqualTo(120);
        assertThat(result.combatPower()).isEqualTo(46065);
    }

    @Test
    void includesEstimatedLevelApForRegularJobs() {
        CombatStatBag stats = new CombatStatBag();
        stats.addFlat(CombatStatKey.ATTACK_POWER, 100);

        CombatPowerCalculation result = formula.calculate("렌", 288, stats);

        assertThat(result.mainStat()).isEqualTo(1458);
        assertThat(result.subStat()).isEqualTo(4);
    }
}

package org.whitedoggy.maplehistory.combat;

import java.util.Map;

public record PresetCombatPower(
        int presetNo,
        Map<String, Integer> sourcePresetNos,
        long combatPower,
        CombatPowerCalculation formula
) {
}

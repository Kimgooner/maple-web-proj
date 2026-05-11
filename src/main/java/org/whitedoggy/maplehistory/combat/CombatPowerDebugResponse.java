package org.whitedoggy.maplehistory.combat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CombatPowerDebugResponse(
        String characterName,
        String ocid,
        LocalDate date,
        String characterClass,
        Integer characterLevel,
        PresetSelectionMode mode,
        int selectedPresetNo,
        Map<String, Integer> selectedSourcePresetNos,
        Long nexonCurrentCombatPower,
        Long verificationDelta,
        Double verificationDeltaRate,
        CombatPowerCalculation formula,
        FormulaTrace formulaTrace,
        StatTotals totals,
        WeaponNormalization weaponNormalization,
        Map<String, SourceTotals> totalsByEndpoint,
        List<StatContribution> contributions,
        List<PresetCombatPower> presetCandidates
) {
}

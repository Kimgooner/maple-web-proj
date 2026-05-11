package org.whitedoggy.maplehistory.combat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CombatPowerPoint(
        LocalDate date,
        int selectedPresetNo,
        Map<String, Integer> selectedSourcePresetNos,
        long calculatedCombatPower,
        Long nexonCurrentCombatPower,
        Long verificationDelta,
        Double verificationDeltaRate,
        boolean verifiedAgainstCurrentPreset,
        CombatPowerCalculation formula,
        List<PresetCombatPower> presetCandidates,
        List<String> warnings
) {
}

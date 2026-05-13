package org.whitedoggy.maplehistory.combat;

import java.util.List;

public record CombatPowerDetailChange(
        String kind,
        String sourceKey,
        String sourceName,
        String label,
        List<String> lines,
        CombatPowerEquipmentSnapshot beforeEquipment,
        CombatPowerEquipmentSnapshot afterEquipment
) {
}

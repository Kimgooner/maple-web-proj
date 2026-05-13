package org.whitedoggy.maplehistory.combat;

import java.util.List;

public record CombatPowerEquipmentSnapshot(
        String name,
        String iconUrl,
        List<String> tooltipLines
) {
}

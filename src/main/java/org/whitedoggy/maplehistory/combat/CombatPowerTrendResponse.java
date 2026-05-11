package org.whitedoggy.maplehistory.combat;

import java.time.LocalDate;
import java.util.List;

public record CombatPowerTrendResponse(
        String characterName,
        String ocid,
        LocalDate from,
        LocalDate to,
        PresetSelectionMode mode,
        List<CombatPowerPoint> points
) {
}

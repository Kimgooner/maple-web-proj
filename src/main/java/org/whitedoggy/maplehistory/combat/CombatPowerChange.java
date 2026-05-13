package org.whitedoggy.maplehistory.combat;

import java.time.LocalDate;
import java.util.List;

public record CombatPowerChange(
        LocalDate previousDate,
        List<CombatPowerTotalChange> totalChanges,
        List<CombatPowerDetailChange> detailChanges
) {
}

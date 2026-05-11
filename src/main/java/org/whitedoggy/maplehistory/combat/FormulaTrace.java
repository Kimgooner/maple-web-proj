package org.whitedoggy.maplehistory.combat;

public record FormulaTrace(
        String statExpression,
        String attackExpression,
        String damageExpression,
        String criticalDamageExpression,
        String finalDamageExpression,
        String combatPowerExpression
) {
}

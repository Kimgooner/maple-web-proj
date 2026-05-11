package org.whitedoggy.maplehistory.combat;

public record WeaponNormalization(
        String weaponName,
        long originalAttack,
        long normalizedBowAttack,
        long delta
) {
}

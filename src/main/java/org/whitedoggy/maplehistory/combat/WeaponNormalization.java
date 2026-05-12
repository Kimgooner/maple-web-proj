package org.whitedoggy.maplehistory.combat;

public record WeaponNormalization(
        String weaponName,
        String statKey,
        long originalAttack,
        long normalizedAttack,
        long delta,
        String family,
        String weaponType,
        Integer bonusRank
) {
    public WeaponNormalization(String weaponName, String statKey, long originalAttack, long normalizedAttack, long delta) {
        this(weaponName, statKey, originalAttack, normalizedAttack, delta, null, null, null);
    }
}

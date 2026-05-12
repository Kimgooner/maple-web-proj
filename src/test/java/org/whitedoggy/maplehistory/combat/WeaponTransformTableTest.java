package org.whitedoggy.maplehistory.combat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WeaponTransformTableTest {

    @Test
    void normalizesGenesisWeaponToBowTemplate() {
        WeaponNormalization normalization = WeaponTransformTable.normalizeWeapon(
                "제네시스 창세검",
                875,
                210,
                246,
                22,
                8,
                CombatStatKey.ATTACK_POWER
        ).orElseThrow();

        assertThat(normalization.normalizedAttack()).isEqualTo(832);
        assertThat(normalization.bonusRank()).isEqualTo(1);
        assertThat(normalization.weaponType()).isEqualTo("제네시스 보우");
    }

    @Test
    void normalizesDestinyWeaponToBowTemplate() {
        WeaponNormalization normalization = WeaponTransformTable.normalizeWeapon(
                "데스티니 튜너",
                998,
                268,
                285,
                22,
                8,
                CombatStatKey.ATTACK_POWER
        ).orElseThrow();

        assertThat(normalization.normalizedAttack()).isEqualTo(949);
        assertThat(normalization.bonusRank()).isEqualTo(1);
        assertThat(normalization.weaponType()).isEqualTo("데스티니 보우");
    }

    @Test
    void genesisOnlySupportsReleasedStarTemplate() {
        assertThat(WeaponTransformTable.normalizeWeapon(
                "제네시스 창세검",
                875,
                163,
                246,
                21,
                8,
                CombatStatKey.ATTACK_POWER
        )).isEmpty();
    }
}

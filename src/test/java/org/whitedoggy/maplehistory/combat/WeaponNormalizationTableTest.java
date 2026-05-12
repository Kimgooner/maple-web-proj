package org.whitedoggy.maplehistory.combat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WeaponNormalizationTableTest {

    @Test
    void normalizesGenesisLongSwordFirstBonusToGenesisBowFirstBonus() {
        var normalization = WeaponNormalizationTable.normalizedBowTotal(
                "제네시스 창세검",
                "장검",
                875,
                210,
                72,
                8,
                253,
                22
        );

        assertThat(normalization).isPresent();
        assertThat(normalization.get().normalizedAttack()).isEqualTo(832);
        assertThat(normalization.get().delta()).isEqualTo(-43);
        assertThat(normalization.get().bonusRank()).isEqualTo(1);
    }

    @Test
    void normalizesGenesisLongSwordSecondBonusToGenesisBowSecondBonus() {
        var normalization = WeaponNormalizationTable.normalizedBowTotal(
                "제네시스 창세검",
                "장검",
                829,
                164,
                72,
                8,
                253,
                22
        );

        assertThat(normalization).isPresent();
        assertThat(normalization.get().normalizedAttack()).isEqualTo(789);
        assertThat(normalization.get().bonusRank()).isEqualTo(2);
    }

    @Test
    void normalizesArcaneLongSwordThirdBonusByFullBonusTable() {
        var normalization = WeaponNormalizationTable.normalizedBowTotal(
                "아케인셰이드 환검",
                "장검",
                433,
                108,
                10,
                1,
                20,
                10
        );

        assertThat(normalization).isPresent();
        assertThat(normalization.get().family()).isEqualTo("아케인셰이드");
        assertThat(normalization.get().bonusRank()).isEqualTo(3);
        assertThat(normalization.get().normalizedAttack()).isEqualTo(406);
    }

    @Test
    void normalizesDestinyFirstBonusFromAppliedScrollAndStarforceComponents() {
        var normalization = WeaponNormalizationTable.normalizedBowTotal(
                "데스티니 튜너",
                "튜너",
                998,
                268,
                72,
                8,
                285,
                22
        );

        assertThat(normalization).isPresent();
        assertThat(normalization.get().family()).isEqualTo("데스티니");
        assertThat(normalization.get().bonusRank()).isEqualTo(1);
        assertThat(normalization.get().normalizedAttack()).isEqualTo(949);
        assertThat(normalization.get().delta()).isEqualTo(-49);
    }

    @Test
    void destinyBaseAttackTableUsesObservedBaseValues() {
        assertThat(WeaponNormalizationTable.normalizedBowTotal("데스티니 피스톨", "건", 900, 196, 0, 0, 0, 0))
                .get()
                .extracting(WeaponNormalization::weaponType, WeaponNormalization::bonusRank)
                .containsExactly("건", 1);
        assertThat(WeaponNormalizationTable.normalizedBowTotal("데스티니 크로스보우", "석궁", 900, 257, 0, 0, 0, 0))
                .get()
                .extracting(WeaponNormalization::weaponType, WeaponNormalization::bonusRank)
                .containsExactly("석궁", 1);
        assertThat(WeaponNormalizationTable.normalizedMagicBase("데스티니 스태프", "스태프", 445, 320))
                .isEmpty();
    }

    @Test
    void keepsGenesisMagicBaseNormalizationOnMeasuredPath() {
        var normalization = WeaponNormalizationTable.normalizedMagicBase(
                "제네시스 매직 건틀렛",
                "매직 건틀렛",
                400,
                192
        );

        assertThat(normalization).isPresent();
        assertThat(normalization.get().normalizedAttack()).isEqualTo(372);
        assertThat(normalization.get().bonusRank()).isEqualTo(2);
    }
}

package org.whitedoggy.maplehistory.combat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class WeaponNormalizationTable {

    private static final long GENESIS_BOW_TOTAL_FIRST_BONUS = 832L;
    private static final long GENESIS_BOW_TOTAL_SECOND_BONUS = 789L;
    private static final long DESTINY_BOW_TOTAL_FIRST_BONUS = 949L;

    private static final List<WeaponRule> GENESIS_RULES = List.of(
            new WeaponRule(List.of("한손검"), 201, 157),
            new WeaponRule(List.of("한손도끼"), 201, 157),
            new WeaponRule(List.of("한손둔기"), 201, 157),
            new WeaponRule(List.of("두손검", "장검"), 210, 164),
            new WeaponRule(List.of("두손도끼"), 210, 164),
            new WeaponRule(List.of("두손둔기"), 210, 164),
            new WeaponRule(List.of("창"), 210, 164),
            new WeaponRule(List.of("폴암"), 187, 146),
            new WeaponRule(List.of("데스페라도"), 210, 164),
            new WeaponRule(List.of("건틀렛 리볼버"), 157, 123),
            new WeaponRule(List.of("튜너"), 210, 164),
            new WeaponRule(List.of("완드"), 246, 192),
            new WeaponRule(List.of("스태프"), 250, 195),
            new WeaponRule(List.of("샤이닝 로드"), 246, 192),
            new WeaponRule(List.of("ESP 리미터"), 246, 192),
            new WeaponRule(List.of("매직 건틀렛"), 246, 192),
            new WeaponRule(List.of("활"), 196, 153),
            new WeaponRule(List.of("석궁"), 201, 157),
            new WeaponRule(List.of("듀얼보우건"), 196, 153),
            new WeaponRule(List.of("에이션트 보우", "에인션트 보우", "브레스 슈터"), 196, 153),
            new WeaponRule(List.of("아대"), 106, 83),
            new WeaponRule(List.of("단검"), 196, 153),
            new WeaponRule(List.of("케인"), 201, 157),
            new WeaponRule(List.of("에너지소드"), 157, 123),
            new WeaponRule(List.of("체인"), 196, 153),
            new WeaponRule(List.of("부채"), 196, 153),
            new WeaponRule(List.of("차크람"), 196, 153),
            new WeaponRule(List.of("너클"), 157, 123),
            new WeaponRule(List.of("건"), 154, 120),
            new WeaponRule(List.of("핸드캐논"), 215, 167),
            new WeaponRule(List.of("소울슈터"), 157, 123)
    );

    private WeaponNormalizationTable() {
    }

    static Optional<Long> normalizedBowTotal(String weaponName, String equipmentPart, long bonusAttack) {
        String name = normalize(weaponName);
        if (name.contains("데스티니") || name.contains("destiny")) {
            return Optional.of(DESTINY_BOW_TOTAL_FIRST_BONUS);
        }
        if (!name.contains("제네시스") && !name.contains("genesis")) {
            return Optional.empty();
        }
        return GENESIS_RULES.stream()
                .filter(rule -> rule.matches(equipmentPart))
                .findFirst()
                .map(rule -> rule.bonusRank(bonusAttack) == 1
                        ? GENESIS_BOW_TOTAL_FIRST_BONUS
                        : GENESIS_BOW_TOTAL_SECOND_BONUS);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private record WeaponRule(List<String> aliases, long firstBonusAttack, long secondBonusAttack) {
        boolean matches(String equipmentPart) {
            String normalized = normalize(equipmentPart);
            return aliases.stream().map(WeaponNormalizationTable::normalize).anyMatch(normalized::contains);
        }

        int bonusRank(long bonusAttack) {
            long firstDistance = Math.abs(bonusAttack - firstBonusAttack);
            long secondDistance = Math.abs(bonusAttack - secondBonusAttack);
            return firstDistance <= secondDistance ? 1 : 2;
        }
    }
}

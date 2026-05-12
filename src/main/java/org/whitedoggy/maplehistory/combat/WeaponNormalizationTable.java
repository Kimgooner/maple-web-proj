package org.whitedoggy.maplehistory.combat;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class WeaponNormalizationTable {

    private static final String BOW = "활";
    private static final Map<WeaponFamily, Map<Integer, Long>> BOW_STARFORCE_ATTACK_BY_FAMILY = Map.of(
            WeaponFamily.GENESIS, Map.of(22, 250L),
            WeaponFamily.DESTINY, Map.of(22, 277L)
    );
    private static final Map<WeaponFamily, Map<String, WeaponProfile>> PROFILES = Map.of(
            WeaponFamily.CHALLENGER, profiles(
                    entry("아대", 103, 53, 42, 32, 23, 16),
                    entry("건", 150, 77, 60, 46, 33, 23),
                    entry("총", 150, 77, 60, 46, 33, 23),
                    entry("에너지소드", 154, 79, 62, 47, 34, 24),
                    entry("너클", 154, 79, 62, 47, 34, 24),
                    entry("건틀렛 리볼버", 154, 79, 62, 47, 34, 24),
                    entry("소울슈터", 154, 79, 62, 47, 34, 24),
                    entry("폴암", 184, 95, 74, 56, 41, 28),
                    entry("에인션트 보우", 192, 99, 77, 59, 43, 29),
                    entry("에이션트 보우", 192, 99, 77, 59, 43, 29),
                    entry("부채", 192, 99, 77, 59, 43, 29),
                    entry("활", 192, 99, 77, 59, 43, 29),
                    entry("브레스 슈터", 192, 99, 77, 59, 43, 29),
                    entry("듀얼보우건", 192, 99, 77, 59, 43, 29),
                    entry("단검", 192, 99, 77, 59, 43, 29),
                    entry("체인", 192, 99, 77, 59, 43, 29),
                    entry("차크람", 192, 99, 77, 59, 43, 29),
                    entry("한손검", 197, 101, 79, 60, 44, 30),
                    entry("한손도끼", 197, 101, 79, 60, 44, 30),
                    entry("한손둔기", 197, 101, 79, 60, 44, 30),
                    entry("케인", 197, 101, 79, 60, 44, 30),
                    entry("석궁", 197, 101, 79, 60, 44, 30),
                    entry("장검", 205, 106, 82, 63, 46, 31),
                    entry("튜너", 205, 106, 82, 63, 46, 31),
                    entry("창", 205, 106, 82, 63, 46, 31),
                    entry("두손검", 205, 106, 82, 63, 46, 31),
                    entry("두손도끼", 205, 106, 82, 63, 46, 31),
                    entry("두손둔기", 205, 106, 82, 63, 46, 31),
                    entry("데스페라도", 205, 106, 82, 63, 46, 31),
                    entry("핸드캐논", 210, 108, 84, 64, 47, 32),
                    entry("완드", 241, 124, 97, 73, 54, 37),
                    entry("샤이닝 로드", 241, 124, 97, 73, 54, 37),
                    entry("ESP 리미터", 241, 124, 97, 73, 54, 37),
                    entry("매직 건틀렛", 241, 124, 97, 73, 54, 37),
                    entry("스태프", 245, 126, 98, 75, 54, 37)
            ),
            WeaponFamily.ABSOLAB, profiles(
                    entry("아대", 103, 53, 42, 32, 23, 16),
                    entry("건", 150, 77, 60, 46, 33, 23),
                    entry("총", 150, 77, 60, 46, 33, 23),
                    entry("에너지소드", 154, 79, 62, 47, 34, 24),
                    entry("너클", 154, 79, 62, 47, 34, 24),
                    entry("건틀렛 리볼버", 154, 79, 62, 47, 34, 24),
                    entry("소울슈터", 154, 79, 62, 47, 34, 24),
                    entry("폴암", 184, 95, 74, 56, 41, 28),
                    entry("에인션트 보우", 192, 99, 77, 59, 43, 29),
                    entry("에이션트 보우", 192, 99, 77, 59, 43, 29),
                    entry("부채", 192, 99, 77, 59, 43, 29),
                    entry("활", 192, 99, 77, 59, 43, 29),
                    entry("브레스 슈터", 192, 99, 77, 59, 43, 29),
                    entry("듀얼보우건", 192, 99, 77, 59, 43, 29),
                    entry("단검", 192, 99, 77, 59, 43, 29),
                    entry("체인", 192, 99, 77, 59, 43, 29),
                    entry("차크람", 192, 99, 77, 59, 43, 29),
                    entry("한손검", 197, 101, 79, 60, 44, 30),
                    entry("한손도끼", 197, 101, 79, 60, 44, 30),
                    entry("한손둔기", 197, 101, 79, 60, 44, 30),
                    entry("케인", 197, 101, 79, 60, 44, 30),
                    entry("석궁", 197, 101, 79, 60, 44, 30),
                    entry("장검", 205, 106, 82, 63, 46, 31),
                    entry("튜너", 205, 106, 82, 63, 46, 31),
                    entry("창", 205, 106, 82, 63, 46, 31),
                    entry("두손검", 205, 106, 82, 63, 46, 31),
                    entry("두손도끼", 205, 106, 82, 63, 46, 31),
                    entry("두손둔기", 205, 106, 82, 63, 46, 31),
                    entry("데스페라도", 205, 106, 82, 63, 46, 31),
                    entry("핸드캐논", 210, 108, 84, 64, 47, 32),
                    entry("완드", 241, 124, 97, 73, 54, 37),
                    entry("샤이닝 로드", 241, 124, 97, 73, 54, 37),
                    entry("ESP 리미터", 241, 124, 97, 73, 54, 37),
                    entry("매직 건틀렛", 241, 124, 97, 73, 54, 37),
                    entry("스태프", 245, 126, 98, 75, 54, 37)
            ),
            WeaponFamily.ARCANE_SHADE, profiles(
                    entry("아대", 149, 92, 72, 55, 40, 27),
                    entry("건", 216, 133, 104, 79, 58, 39),
                    entry("총", 216, 133, 104, 79, 58, 39),
                    entry("에너지소드", 221, 136, 106, 81, 59, 40),
                    entry("너클", 221, 136, 106, 81, 59, 40),
                    entry("건틀렛 리볼버", 221, 136, 106, 81, 59, 40),
                    entry("소울슈터", 221, 136, 106, 81, 59, 40),
                    entry("폴암", 264, 163, 127, 96, 70, 48),
                    entry("에인션트 보우", 276, 170, 133, 101, 73, 50),
                    entry("에이션트 보우", 276, 170, 133, 101, 73, 50),
                    entry("부채", 276, 170, 133, 101, 73, 50),
                    entry("활", 276, 170, 133, 101, 73, 50),
                    entry("브레스 슈터", 276, 170, 133, 101, 73, 50),
                    entry("듀얼보우건", 276, 170, 133, 101, 73, 50),
                    entry("단검", 276, 170, 133, 101, 73, 50),
                    entry("체인", 276, 170, 133, 101, 73, 50),
                    entry("차크람", 276, 170, 133, 101, 73, 50),
                    entry("한손검", 283, 175, 136, 103, 75, 51),
                    entry("한손도끼", 283, 175, 136, 103, 75, 51),
                    entry("한손둔기", 283, 175, 136, 103, 75, 51),
                    entry("케인", 283, 175, 136, 103, 75, 51),
                    entry("석궁", 283, 175, 136, 103, 75, 51),
                    entry("장검", 295, 182, 142, 108, 78, 54),
                    entry("튜너", 295, 182, 142, 108, 78, 54),
                    entry("창", 295, 182, 142, 108, 78, 54),
                    entry("두손검", 295, 182, 142, 108, 78, 54),
                    entry("두손도끼", 295, 182, 142, 108, 78, 54),
                    entry("두손둔기", 295, 182, 142, 108, 78, 54),
                    entry("데스페라도", 295, 182, 142, 108, 78, 54),
                    entry("핸드캐논", 302, 186, 145, 110, 80, 55),
                    entry("완드", 347, 214, 167, 126, 92, 63),
                    entry("샤이닝 로드", 347, 214, 167, 126, 92, 63),
                    entry("ESP 리미터", 347, 214, 167, 126, 92, 63),
                    entry("매직 건틀렛", 347, 214, 167, 126, 92, 63),
                    entry("스태프", 353, 218, 170, 129, 94, 64)
            ),
            WeaponFamily.GENESIS, profiles(
                    entry("아대", 172, 106, 83, 63, 46, 31),
                    entry("건", 249, 154, 120, 91, 66, 45),
                    entry("총", 249, 154, 120, 91, 66, 45),
                    entry("에너지소드", 255, 157, 123, 93, 68, 46),
                    entry("너클", 255, 157, 123, 93, 68, 46),
                    entry("건틀렛 리볼버", 255, 157, 123, 93, 68, 46),
                    entry("소울슈터", 255, 157, 123, 93, 68, 46),
                    entry("폴암", 304, 187, 146, 111, 81, 55),
                    entry("에인션트 보우", 318, 196, 153, 116, 84, 58),
                    entry("에이션트 보우", 318, 196, 153, 116, 84, 58),
                    entry("부채", 318, 196, 153, 116, 84, 58),
                    entry("활", 318, 196, 153, 116, 84, 58),
                    entry("브레스 슈터", 318, 196, 153, 116, 84, 58),
                    entry("듀얼보우건", 318, 196, 153, 116, 84, 58),
                    entry("단검", 318, 196, 153, 116, 84, 58),
                    entry("체인", 318, 196, 153, 116, 84, 58),
                    entry("차크람", 318, 196, 153, 116, 84, 58),
                    entry("한손검", 326, 201, 157, 119, 87, 59),
                    entry("한손도끼", 326, 201, 157, 119, 87, 59),
                    entry("한손둔기", 326, 201, 157, 119, 87, 59),
                    entry("케인", 326, 201, 157, 119, 87, 59),
                    entry("석궁", 326, 201, 157, 119, 87, 59),
                    entry("장검", 340, 210, 164, 124, 90, 62),
                    entry("튜너", 340, 210, 164, 124, 90, 62),
                    entry("창", 340, 210, 164, 124, 90, 62),
                    entry("두손검", 340, 210, 164, 124, 90, 62),
                    entry("두손도끼", 340, 210, 164, 124, 90, 62),
                    entry("두손둔기", 340, 210, 164, 124, 90, 62),
                    entry("데스페라도", 340, 210, 164, 124, 90, 62),
                    entry("핸드캐논", 348, 214, 167, 127, 92, 63),
                    entry("완드", 400, 246, 192, 146, 106, 72),
                    entry("샤이닝 로드", 400, 246, 192, 146, 106, 72),
                    entry("ESP 리미터", 400, 246, 192, 146, 106, 72),
                    entry("매직 건틀렛", 400, 246, 192, 146, 106, 72),
                    entry("스태프", 406, 250, 195, 148, 108, 74)
            ),
            WeaponFamily.DESTINY, profiles(
                    entry("아대", 189, 136, 106, 81, 59, 40),
                    entry("건", 273, 196, 153, 116, 85, 58),
                    entry("총", 273, 196, 153, 116, 85, 58),
                    entry("에너지소드", 280, 201, 157, 119, 87, 59),
                    entry("너클", 280, 201, 157, 119, 87, 59),
                    entry("건틀렛 리볼버", 280, 201, 157, 119, 87, 59),
                    entry("소울슈터", 280, 201, 157, 119, 87, 59),
                    entry("폴암", 334, 240, 187, 142, 103, 71),
                    entry("에인션트 보우", 349, 251, 196, 148, 108, 74),
                    entry("에이션트 보우", 349, 251, 196, 148, 108, 74),
                    entry("부채", 349, 251, 196, 148, 108, 74),
                    entry("활", 349, 251, 196, 148, 108, 74),
                    entry("브레스 슈터", 349, 251, 196, 148, 108, 74),
                    entry("듀얼보우건", 349, 251, 196, 148, 108, 74),
                    entry("단검", 349, 251, 196, 148, 108, 74),
                    entry("체인", 349, 251, 196, 148, 108, 74),
                    entry("차크람", 349, 251, 196, 148, 108, 74),
                    entry("한손검", 358, 257, 201, 152, 111, 76),
                    entry("한손도끼", 358, 257, 201, 152, 111, 76),
                    entry("한손둔기", 358, 257, 201, 152, 111, 76),
                    entry("케인", 358, 257, 201, 152, 111, 76),
                    entry("석궁", 358, 257, 201, 152, 111, 76),
                    entry("장검", 373, 268, 209, 158, 115, 79),
                    entry("튜너", 373, 268, 209, 158, 115, 79),
                    entry("창", 373, 268, 209, 158, 115, 79),
                    entry("두손검", 373, 268, 209, 158, 115, 79),
                    entry("두손도끼", 373, 268, 209, 158, 115, 79),
                    entry("두손둔기", 373, 268, 209, 158, 115, 79),
                    entry("데스페라도", 373, 268, 209, 158, 115, 79),
                    entry("핸드캐논", 382, 275, 214, 162, 118, 81),
                    entry("완드", 439, 315, 246, 186, 136, 93),
                    entry("샤이닝 로드", 439, 315, 246, 186, 136, 93),
                    entry("ESP 리미터", 439, 315, 246, 186, 136, 93),
                    entry("매직 건틀렛", 439, 315, 246, 186, 136, 93),
                    entry("스태프", 445, 320, 249, 189, 139, 94)
            )
    );

    private WeaponNormalizationTable() {
    }

    static Optional<WeaponNormalization> normalizedBowTotal(
            String weaponName,
            String equipmentPart,
            long originalAttack,
            long sourceBonusAttack,
            long sourceScrollAttack,
            int sourceScrollUpgradeCount,
            long sourceStarforceAttack,
            int sourceStarforce
    ) {
        Optional<WeaponMatch> match = match(weaponName, equipmentPart, sourceBonusAttack);
        if (match.isEmpty() || originalAttack <= 0L) {
            return Optional.empty();
        }
        WeaponMatch value = match.get();
        long normalized = normalizedBowTotal(value, sourceScrollAttack, sourceScrollUpgradeCount, sourceStarforceAttack, sourceStarforce);
        return Optional.of(new WeaponNormalization(
                weaponName,
                CombatStatKey.ATTACK_POWER.name(),
                originalAttack,
                normalized,
                normalized - originalAttack,
                value.family().displayName,
                value.source().weaponType(),
                value.bonusRank()
        ));
    }

    static Optional<WeaponNormalization> normalizedMagicBase(
            String weaponName,
            String equipmentPart,
            long weaponBaseMagic,
            long sourceBonusMagic
    ) {
        Optional<WeaponMatch> match = match(weaponName, equipmentPart, sourceBonusMagic);
        if (match.isEmpty() || weaponBaseMagic <= 0L) {
            return Optional.empty();
        }
        WeaponMatch value = match.get();
        if (value.family() != WeaponFamily.GENESIS || value.source().baseAttack() <= 0L) {
            return Optional.empty();
        }
        long normalizedGenesisBase = normalize(value.source().weaponType()).contains("스태프") ? 378L : 372L;
        long normalized = Math.round(weaponBaseMagic * normalizedGenesisBase / (double) value.source().baseAttack());
        return Optional.of(new WeaponNormalization(
                weaponName,
                CombatStatKey.MAGIC_ATTACK.name(),
                weaponBaseMagic,
                normalized,
                normalized - weaponBaseMagic,
                value.family().displayName,
                value.source().weaponType(),
                value.bonusRank()
        ));
    }

    private static Optional<WeaponMatch> match(String weaponName, String equipmentPart, long sourceBonusAttack) {
        Optional<WeaponFamily> family = WeaponFamily.from(weaponName);
        if (family.isEmpty()) {
            return Optional.empty();
        }
        Map<String, WeaponProfile> familyProfiles = PROFILES.get(family.get());
        if (familyProfiles == null) {
            return Optional.empty();
        }
        Optional<WeaponProfile> source = familyProfiles.values().stream()
                .filter(profile -> profile.matches(equipmentPart))
                .max(Comparator.comparingInt(profile -> normalize(profile.weaponType()).length()));
        WeaponProfile targetBow = familyProfiles.get(normalize(BOW));
        if (source.isEmpty() || targetBow == null) {
            return Optional.empty();
        }
        return Optional.of(new WeaponMatch(family.get(), source.get(), targetBow, source.get().bonusRank(sourceBonusAttack)));
    }

    private static long normalizedBowTotal(
            WeaponMatch match,
            long sourceScrollAttack,
            int sourceScrollUpgradeCount,
            long sourceStarforceAttack,
            int sourceStarforce
    ) {
        return match.targetBow().baseAttack()
                + match.targetBow().bonus(match.bonusRank())
                + normalizedScroll(match, sourceScrollAttack, sourceScrollUpgradeCount)
                + normalizedStarforce(match, sourceStarforceAttack, sourceStarforce);
    }

    private static long normalizedScroll(WeaponMatch match, long sourceScrollAttack, int sourceScrollUpgradeCount) {
        if (sourceScrollAttack == 0L) {
            return 0L;
        }
        if (match.family() == WeaponFamily.GENESIS && sourceScrollUpgradeCount == 8 && sourceScrollAttack == 72L) {
            return 68L;
        }
        if (sourceScrollUpgradeCount <= 0) {
            return sourceScrollAttack;
        }
        return Math.round(sourceScrollAttack / (double) sourceScrollUpgradeCount * sourceScrollUpgradeCount);
    }

    private static long normalizedStarforce(WeaponMatch match, long sourceStarforceAttack, int sourceStarforce) {
        if (sourceStarforceAttack == 0L) {
            return 0L;
        }
        Map<Integer, Long> familyStarforce = BOW_STARFORCE_ATTACK_BY_FAMILY.get(match.family());
        if (familyStarforce != null && familyStarforce.containsKey(sourceStarforce)) {
            return familyStarforce.get(sourceStarforce);
        }
        return scaleByBase(match, sourceStarforceAttack);
    }

    private static long scaleByBase(WeaponMatch match, long value) {
        if (match.source().baseAttack() <= 0L || match.targetBow().baseAttack() <= 0L) {
            return value;
        }
        return Math.round(value * match.targetBow().baseAttack() / (double) match.source().baseAttack());
    }

    private static Map<String, WeaponProfile> profiles(WeaponProfile... profiles) {
        return java.util.Arrays.stream(profiles)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        profile -> normalize(profile.weaponType()),
                        profile -> profile,
                        (left, right) -> left
                ));
    }

    private static WeaponProfile entry(String weaponType, long baseAttack, long first, long second, long third, long fourth, long fifth) {
        return new WeaponProfile(weaponType, baseAttack, List.of(first, second, third, fourth, fifth));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("\u00a0", "");
    }

    private enum WeaponFamily {
        CHALLENGER("도전자"),
        ABSOLAB("앱솔랩스"),
        ARCANE_SHADE("아케인셰이드"),
        GENESIS("제네시스"),
        DESTINY("데스티니");

        private final String displayName;

        WeaponFamily(String displayName) {
            this.displayName = displayName;
        }

        static Optional<WeaponFamily> from(String weaponName) {
            String normalized = normalize(weaponName);
            if (normalized.contains("도전자") || normalized.contains("challenger")) {
                return Optional.of(CHALLENGER);
            }
            if (normalized.contains("앱솔랩스") || normalized.contains("absolab")) {
                return Optional.of(ABSOLAB);
            }
            if (normalized.contains("아케인셰이드") || normalized.contains("arcane")) {
                return Optional.of(ARCANE_SHADE);
            }
            if (normalized.contains("제네시스") || normalized.contains("genesis")) {
                return Optional.of(GENESIS);
            }
            if (normalized.contains("데스티니") || normalized.contains("destiny")) {
                return Optional.of(DESTINY);
            }
            return Optional.empty();
        }
    }

    private record WeaponProfile(String weaponType, long baseAttack, List<Long> bonusByRank) {
        boolean matches(String equipmentPart) {
            return normalize(equipmentPart).contains(normalize(weaponType));
        }

        int bonusRank(long bonusAttack) {
            if (bonusAttack <= 0L) {
                return 1;
            }
            int bestRank = 1;
            long bestDistance = Long.MAX_VALUE;
            for (int index = 0; index < bonusByRank.size(); index++) {
                long distance = Math.abs(bonusAttack - bonusByRank.get(index));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestRank = index + 1;
                }
            }
            return bestRank;
        }

        long bonus(int rank) {
            return bonusByRank.get(Math.max(1, Math.min(rank, bonusByRank.size())) - 1);
        }
    }

    private record WeaponMatch(WeaponFamily family, WeaponProfile source, WeaponProfile targetBow, int bonusRank) {
    }
}

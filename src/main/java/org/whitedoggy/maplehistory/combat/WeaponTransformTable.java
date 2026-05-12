package org.whitedoggy.maplehistory.combat;

import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WeaponTransformTable {
    private WeaponTransformTable() {}

    public static final Map<String, int[]> WEAPON_BONUS_OPTIONS = Map.ofEntries(
            e("도전자의 리벤지가즈", 53, 42, 32, 23, 16),
            e("도전자의 포인팅건", 77, 60, 46, 33, 23),
            e("도전자의 에너지소드", 79, 62, 47, 34, 24),
            e("도전자의 블로우너클", 79, 62, 47, 34, 24),
            e("도전자의 파일 갓", 79, 62, 47, 34, 24),
            e("도전자의 소울슈터", 79, 62, 47, 34, 24),
            e("도전자의 핼버드", 95, 74, 56, 41, 28),
            e("도전자의 에인션트 보우", 99, 77, 59, 43, 29),
            e("도전자의 괴선", 99, 77, 59, 43, 29),
            e("도전자의 슈팅보우", 99, 77, 59, 43, 29),
            e("도전자의 브레스 슈터", 99, 77, 59, 43, 29),
            e("도전자의 듀얼보우건", 99, 77, 59, 43, 29),
            e("도전자의 슬래셔", 99, 77, 59, 43, 29),
            e("도전자의 체인", 99, 77, 59, 43, 29),
            e("도전자의 차크람", 99, 77, 59, 43, 29),
            e("도전자의 세이버", 101, 79, 60, 44, 30),
            e("도전자의 엑스", 101, 79, 60, 44, 30),
            e("도전자의 비트해머", 101, 79, 60, 44, 30),
            e("도전자의 핀쳐케인", 101, 79, 60, 44, 30),
            e("도전자의 크로스보우", 101, 79, 60, 44, 30),
            e("도전자의 튜너", 106, 82, 63, 46, 31),
            e("도전자의 피어싱스피어", 106, 82, 63, 46, 31),
            e("도전자의 브로드세이버", 106, 82, 63, 46, 31),
            e("도전자의 브로드엑스", 106, 82, 63, 46, 31),
            e("도전자의 브로드해머", 106, 82, 63, 46, 31),
            e("도전자의 데스페라도", 106, 82, 63, 46, 31),
            e("도전자의 블래스트캐논", 108, 84, 64, 47, 32),
            e("도전자의 스펠링완드", 124, 97, 73, 54, 37),
            e("도전자의 샤이닝로드", 124, 97, 73, 54, 37),
            e("도전자의 ESP리미터", 124, 97, 73, 54, 37),
            e("도전자의 매직 건틀렛", 124, 97, 73, 54, 37),
            e("도전자의 스펠링스태프", 126, 98, 75, 54, 37),

            e("앱솔랩스 리벤지가즈", 53, 42, 32, 23, 16),
            e("앱솔랩스 포인팅건", 77, 60, 46, 33, 23),
            e("앱솔랩스 에너지소드", 79, 62, 47, 34, 24),
            e("앱솔랩스 블로우너클", 79, 62, 47, 34, 24),
            e("앱솔랩스 파일 갓", 79, 62, 47, 34, 24),
            e("앱솔랩스 소울슈터", 79, 62, 47, 34, 24),
            e("앱솔랩스 핼버드", 95, 74, 56, 41, 28),
            e("앱솔랩스 에인션트 보우", 99, 77, 59, 43, 29),
            e("앱솔랩스 괴선", 99, 77, 59, 43, 29),
            e("앱솔랩스 슈팅보우", 99, 77, 59, 43, 29),
            e("앱솔랩스 브레스 슈터", 99, 77, 59, 43, 29),
            e("앱솔랩스 듀얼보우건", 99, 77, 59, 43, 29),
            e("앱솔랩스 슬래셔", 99, 77, 59, 43, 29),
            e("앱솔랩스 체인", 99, 77, 59, 43, 29),
            e("앱솔랩스 차크람", 99, 77, 59, 43, 29),
            e("앱솔랩스 세이버", 101, 79, 60, 44, 30),
            e("앱솔랩스 엑스", 101, 79, 60, 44, 30),
            e("앱솔랩스 비트해머", 101, 79, 60, 44, 30),
            e("앱솔랩스 핀쳐케인", 101, 79, 60, 44, 30),
            e("앱솔랩스 크로스보우", 101, 79, 60, 44, 30),
            e("앱솔랩스 튜너", 106, 82, 63, 46, 31),
            e("앱솔랩스 피어싱스피어", 106, 82, 63, 46, 31),
            e("앱솔랩스 브로드세이버", 106, 82, 63, 46, 31),
            e("앱솔랩스 브로드엑스", 106, 82, 63, 46, 31),
            e("앱솔랩스 브로드해머", 106, 82, 63, 46, 31),
            e("앱솔랩스 데스페라도", 106, 82, 63, 46, 31),
            e("앱솔랩스 블래스트캐논", 108, 84, 64, 47, 32),
            e("앱솔랩스 스펠링완드", 124, 97, 73, 54, 37),
            e("앱솔랩스 샤이닝로드", 124, 97, 73, 54, 37),
            e("앱솔랩스 ESP리미터", 124, 97, 73, 54, 37),
            e("앱솔랩스 매직 건틀렛", 124, 97, 73, 54, 37),
            e("앱솔랩스 스펠링스태프", 126, 98, 75, 54, 37),
            e("라즐리 8형", 76, 56, 0, 0, 0),
            e("라피스 8형", 76, 56, 0, 0, 0),

            e("아케인셰이드 가즈", 92, 72, 55, 40, 27),
            e("아케인셰이드 피스톨", 133, 104, 79, 58, 39),
            e("아케인셰이드 에너지체인", 136, 106, 81, 59, 40),
            e("아케인셰이드 클로", 136, 106, 81, 59, 40),
            e("아케인셰이드 엘라하", 136, 106, 81, 59, 40),
            e("아케인셰이드 소울슈터", 136, 106, 81, 59, 40),
            e("아케인셰이드 폴암", 163, 127, 96, 70, 48),
            e("아케인셰이드 에인션트 보우", 170, 133, 101, 73, 50),
            e("아케인셰이드 초선", 170, 133, 101, 73, 50),
            e("아케인셰이드 보우", 170, 133, 101, 73, 50),
            e("아케인셰이드 브레스 슈터", 170, 133, 101, 73, 50),
            e("아케인셰이드 듀얼보우건", 170, 133, 101, 73, 50),
            e("아케인셰이드 대거", 170, 133, 101, 73, 50),
            e("아케인셰이드 체인", 170, 133, 101, 73, 50),
            e("아케인셰이드 차크람", 170, 133, 101, 73, 50),
            e("아케인셰이드 세이버", 175, 136, 103, 75, 51),
            e("아케인셰이드 엑스", 175, 136, 103, 75, 51),
            e("아케인셰이드 해머", 175, 136, 103, 75, 51),
            e("아케인셰이드 케인", 175, 136, 103, 75, 51),
            e("아케인셰이드 크로스보우", 175, 136, 103, 75, 51),
            e("아케인셰이드 튜너", 182, 142, 108, 78, 54),
            e("아케인셰이드 스피어", 182, 142, 108, 78, 54),
            e("아케인셰이드 투핸드소드", 182, 142, 108, 78, 54),
            e("아케인셰이드 투핸드엑스", 182, 142, 108, 78, 54),
            e("아케인셰이드 투핸드해머", 182, 142, 108, 78, 54),
            e("아케인셰이드 데스페라도", 182, 142, 108, 78, 54),
            e("아케인셰이드 환검", 182, 142, 108, 78, 54),
            e("아케인셰이드 시즈건", 186, 145, 110, 80, 55),
            e("아케인셰이드 완드", 214, 167, 126, 92, 63),
            e("아케인셰이드 샤이닝로드", 214, 167, 126, 92, 63),
            e("아케인셰이드 ESP리미터", 214, 167, 126, 92, 63),
            e("아케인셰이드 매직 건틀렛", 214, 167, 126, 92, 63),
            e("아케인셰이드 스태프", 218, 170, 129, 94, 64),
            e("라즐리 9형", 131, 95, 0, 0, 0),
            e("라피스 9형", 131, 95, 0, 0, 0),

            e("제네시스 가즈", 106, 83, 63, 46, 31),
            e("제네시스 피스톨", 154, 120, 91, 66, 45),
            e("제네시스 에너지체인", 157, 123, 93, 68, 46),
            e("제네시스 클로", 157, 123, 93, 68, 46),
            e("제네시스 엘라하", 157, 123, 93, 68, 46),
            e("제네시스 소울슈터", 157, 123, 93, 68, 46),
            e("제네시스 폴암", 187, 146, 111, 81, 55),
            e("제네시스 에인션트 보우", 196, 153, 116, 84, 58),
            e("제네시스 창세선", 196, 153, 116, 84, 58),
            e("제네시스 보우", 196, 153, 116, 84, 58),
            e("제네시스 브레스 슈터", 196, 153, 116, 84, 58),
            e("제네시스 듀얼보우건", 196, 153, 116, 84, 58),
            e("제네시스 대거", 196, 153, 116, 84, 58),
            e("제네시스 체인", 196, 153, 116, 84, 58),
            e("제네시스 이클립스", 196, 153, 116, 84, 58),
            e("제네시스 세이버", 201, 157, 119, 87, 59),
            e("제네시스 엑스", 201, 157, 119, 87, 59),
            e("제네시스 해머", 201, 157, 119, 87, 59),
            e("제네시스 케인", 201, 157, 119, 87, 59),
            e("제네시스 크로스보우", 201, 157, 119, 87, 59),
            e("제네시스 튜너", 210, 163, 124, 90, 62),
            e("제네시스 스피어", 210, 163, 124, 90, 62),
            e("제네시스 투핸드소드", 210, 163, 124, 90, 62),
            e("제네시스 투핸드엑스", 210, 163, 124, 90, 62),
            e("제네시스 투핸드해머", 210, 163, 124, 90, 62),
            e("제네시스 데스페라도", 210, 163, 124, 90, 62),
            e("제네시스 창세검", 210, 163, 124, 90, 62),
            e("제네시스 시즈건", 214, 167, 127, 92, 63),
            e("제네시스 완드", 246, 192, 146, 106, 72),
            e("제네시스 샤이닝로드", 246, 192, 146, 106, 72),
            e("제네시스 ESP리미터", 246, 192, 146, 106, 72),
            e("제네시스 매직 건틀렛", 246, 192, 146, 106, 72),
            e("제네시스 스태프", 250, 195, 148, 108, 74),
            e("제네시스 라즐리", 151, 110, 0, 0, 0),
            e("제네시스 라피스", 151, 110, 0, 0, 0),

            e("데스티니 가즈", 136, 106, 81, 59, 40),
            e("데스티니 피스톨", 196, 153, 116, 85, 58),
            e("데스티니 에너지체인", 201, 157, 119, 87, 59),
            e("데스티니 클로", 201, 157, 119, 87, 59),
            e("데스티니 엘라하", 201, 157, 119, 87, 59),
            e("데스티니 소울슈터", 201, 157, 119, 87, 59),
            e("데스티니 폴암", 240, 187, 142, 103, 71),
            e("데스티니 에인션트 보우", 251, 196, 148, 108, 74),
            e("데스티니 초월선", 251, 196, 148, 108, 74),
            e("데스티니 보우", 251, 196, 148, 108, 74),
            e("데스티니 브레스 슈터", 251, 196, 148, 108, 74),
            e("데스티니 듀얼보우건", 251, 196, 148, 108, 74),
            e("데스티니 대거", 251, 196, 148, 108, 74),
            e("데스티니 체인", 251, 196, 148, 108, 74),
            e("데스티니 차크람", 251, 196, 148, 108, 74),
            e("데스티니 세이버", 257, 201, 152, 111, 76),
            e("데스티니 엑스", 257, 201, 152, 111, 76),
            e("데스티니 해머", 257, 201, 152, 111, 76),
            e("데스티니 케인", 257, 201, 152, 111, 76),
            e("데스티니 크로스보우", 257, 201, 152, 111, 76),
            e("데스티니 튜너", 268, 209, 158, 115, 79),
            e("데스티니 스피어", 268, 209, 158, 115, 79),
            e("데스티니 투핸드소드", 268, 209, 158, 115, 79),
            e("데스티니 투핸드엑스", 268, 209, 158, 115, 79),
            e("데스티니 투핸드해머", 268, 209, 158, 115, 79),
            e("데스티니 데스페라도", 268, 209, 158, 115, 79),
            e("데스티니 초극검", 268, 209, 158, 115, 79),
            e("데스티니 블래스트 캐논", 275, 214, 162, 118, 81),
            e("데스티니 완드", 315, 246, 186, 136, 93),
            e("데스티니 샤이닝 로드", 315, 246, 186, 136, 93),
            e("데스티니 ESP리미터", 315, 246, 186, 136, 93),
            e("데스티니 매직 건틀렛", 315, 246, 186, 136, 93),
            e("데스티니 스태프", 320, 249, 189, 139, 94),
            e("데스티니 라즐리", 193, 140, 0, 0, 0),
            e("데스티니 라피스", 193, 140, 0, 0, 0)
    );

    public static final Map<String, int[]> WEAPON_STARFORCE_OPTIONS = Map.ofEntries(
            Map.entry("앱솔랩스", new int[]{9, 18, 28, 39, 51, 64, 78, 110, 143, 177, 0, 0, 0, 0, 0}),
            Map.entry("아케인셰이드", new int[]{13, 26, 40, 54, 69, 85, 102, 136, 171, 207, 244, 282, 321, 261, 402}),
            Map.entry("제네시스", new int[]{13, 26, 40, 54, 69, 85, 102, 136, 171, 207, 244, 282, 321, 261, 402}),
            Map.entry("데스티니", new int[]{16, 32, 49, 66, 84, 103, 123, 0, 0, 0, 0, 0, 0, 0, 0})
    );

    private static final Map<String, BowWeaponTemplateFamily> BOW_TEMPLATE_FAMILIES = Map.of(
            "앱솔랩스", new BowWeaponTemplateFamily("앱솔랩스", "앱솔랩스 슈팅보우", 192, 9, 1, 25),
            "아케인셰이드", new BowWeaponTemplateFamily("아케인셰이드", "아케인셰이드 보우", 276, 9, 1, 25),
            "제네시스", new BowWeaponTemplateFamily("제네시스", "제네시스 보우", 318, 8, 22, 22),
            "데스티니", new BowWeaponTemplateFamily("데스티니", "데스티니 보우", 349, 8, 22, 22)
    );

    private static Map.Entry<String, int[]> e(String itemName, int first, int second, int third, int fourth, int fifth) {
        return Map.entry(itemName, new int[]{first, second, third, fourth, fifth});
    }

    public static Integer findAttackBonus(String itemName, int tier) {
        int[] values = WEAPON_BONUS_OPTIONS.get(itemName);
        if (values == null || tier < 1 || tier > 5) {
            return null;
        }
        return values[tier - 1];
    }

    public static Integer findBonusTier(String weaponName, int bonusAttack) {
        int[] values = WEAPON_BONUS_OPTIONS.get(weaponName);
        if (values == null) {
            return 0;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] == bonusAttack) {
                return i; // index 0 = 1추
            }
        }
        return 0;
    }

    public static void getAttackStat(String weaponName, int bonusTier, int starForce, int scroll, JsonNode item) {
        String tableName = null;
        String starForceTableName = null;
        int attackStat = 0;

        if (weaponName.contains("앱솔랩스") || weaponName.contains("8형")) {
            tableName = "앱솔랩스 슈팅보우";
            starForceTableName = "앱솔랩스";
            attackStat = 192;
        } else if (weaponName.contains("아케인셰이드") || weaponName.contains("9형")) {
            tableName = "아케인셰이드 보우";
            starForceTableName = "아케인셰이드";
            attackStat = 276;
        } else if (weaponName.contains("제네시스")) {
            tableName = "제네시스 보우";
            starForceTableName = "제네시스";
            attackStat = 318;
        } else if (weaponName.contains("데스티니")) {
            tableName = "데스티니 보우";
            starForceTableName = "데스티니";
            attackStat = 349;
        }

        if (tableName == null) {
            return;
        }

        int[] values = WEAPON_BONUS_OPTIONS.get(tableName);
        int[] values_starForce = WEAPON_STARFORCE_OPTIONS.get(starForceTableName);
        attackStat += values[bonusTier - 1];
        attackStat += 8 * scroll;

        // 스타포스는 일단 1~15성 단순 계산
        int currentAttack = attackStat - values[bonusTier - 1]; // 추옵 제외
        for (int star = 1; star <= starForce; star++) {
            int delta = currentAttack / 50 + 1;
            attackStat += delta;
            currentAttack += delta;
        }

        if(starForce >= 16){
            attackStat += values_starForce[starForce-16];
        }

    }
    public static Optional<WeaponNormalization> normalizeWeapon(
            String weaponName,
            long originalStat,
            long bonusStat,
            int starForce,
            int scroll,
            CombatStatKey statKey
    ) {
        return normalizeWeapon(weaponName, originalStat, bonusStat, 0L, starForce, scroll, statKey);
    }

    public static Optional<WeaponNormalization> normalizeWeapon(
            String weaponName,
            long originalStat,
            long bonusStat,
            long starforceStat,
            int starForce,
            int scroll,
            CombatStatKey statKey
    ) {
        if (weaponName == null || weaponName.isBlank() || originalStat <= 0L) {
            return Optional.empty();
        }
        String familyName = familyNameForTransform(weaponName);
        BowWeaponTemplateFamily family = familyName == null ? null : BOW_TEMPLATE_FAMILIES.get(familyName);
        if (family == null) {
            return Optional.empty();
        }
        int bonusTier = resolveBonusTier(weaponName, Math.toIntExact(bonusStat));
        if (bonusTier <= 0) {
            return Optional.empty();
        }
        if (!family.supports(starForce)) {
            return Optional.empty();
        }
        Integer normalizedStat = family.templateAttack(starForce, bonusTier);
        if (normalizedStat == null) {
            return Optional.empty();
        }

        return Optional.of(new WeaponNormalization(
                weaponName,
                statKey.name(),
                originalStat,
                normalizedStat,
                normalizedStat - originalStat,
                familyName,
                family.bowName(),
                bonusTier
        ));
    }

    private static int resolveBonusTier(String weaponName, int bonusAttack) {
        int[] values = WEAPON_BONUS_OPTIONS.get(weaponName);
        if (values == null || values.length == 0) {
            return 0;
        }
        int bestTier = 1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            int distance = Math.abs(values[i] - bonusAttack);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTier = i + 1;
            }
        }
        return bestTier;
    }

    private static String familyNameForTransform(String weaponName) {
        if (weaponName.contains("앱솔랩스") || weaponName.contains("8형")) {
            return "앱솔랩스";
        }
        if (weaponName.contains("아케인셰이드") || weaponName.contains("9형")) {
            return "아케인셰이드";
        }
        if (weaponName.contains("제네시스")) {
            return "제네시스";
        }
        if (weaponName.contains("데스티니")) {
            return "데스티니";
        }
        if (weaponName.contains("도전자")) {
            return "도전자";
        }
        return null;
    }

    private static String targetBowName(String weaponName) {
        String familyName = familyNameForTransform(weaponName);
        if (familyName == null) {
            return null;
        }
        return switch (familyName) {
            case "앱솔랩스" -> "앱솔랩스 슈팅보우";
            case "아케인셰이드" -> "아케인셰이드 보우";
            case "제네시스" -> "제네시스 보우";
            case "데스티니" -> "데스티니 보우";
            case "도전자" -> "도전자의 세인트 보우";
            default -> null;
        };
    }

    private static Integer targetBowBaseAttack(String weaponName) {
        return switch (familyNameForTransform(weaponName)) {
            case "앱솔랩스" -> 192;
            case "아케인셰이드" -> 276;
            case "제네시스" -> 318;
            case "데스티니" -> 349;
            case "도전자" -> 192;
            default -> null;
        };
    }

    private static long normalizedScrollContribution(String familyName, int scroll) {
        if (scroll <= 0) {
            return 0L;
        }
        return 9L * scroll;
    }

    private static long starforceContribution(String familyName, int starForce, int scroll) {
        BowWeaponTemplateFamily family = BOW_TEMPLATE_FAMILIES.get(familyName);
        Integer baseAttack = family == null ? null : family.baseAttack();
        if (baseAttack == null || starForce <= 0) {
            return 0L;
        }
        long scrollContribution = normalizedScrollContribution(familyName, scroll);
        long currentAttack = baseAttack + scrollContribution;
        long total = 0L;
        int dynamicStars = Math.min(starForce, 15);
        for (int star = 1; star <= dynamicStars; star++) {
            long delta = currentAttack / 50L + 1L;
            total += delta;
            currentAttack += delta;
        }
        if (starForce >= 16) {
            int[] values = WEAPON_STARFORCE_OPTIONS.get(familyName);
            if (values != null) {
                int index = starForce - 16;
                if (index >= 0 && index < values.length) {
                    total += values[index];
                }
            }
        }
        return total;
    }

    private record BowWeaponTemplateFamily(
            String familyName,
            String bowName,
            int baseAttack,
            int scrollSlots,
            int minStar,
            int maxStar,
            Map<Integer, Map<Integer, Integer>> templateAttacks
    ) {
        private BowWeaponTemplateFamily(String familyName, String bowName, int baseAttack, int scrollSlots, int minStar, int maxStar) {
            this(
                    familyName,
                    bowName,
                    baseAttack,
                    scrollSlots,
                    minStar,
                    maxStar,
                    buildTemplateAttacks(familyName, bowName, baseAttack, scrollSlots, minStar, maxStar)
            );
        }

        boolean supports(int starForce) {
            return starForce >= minStar && starForce <= maxStar;
        }

        Integer templateAttack(int starForce, int bonusTier) {
            Map<Integer, Integer> byTier = templateAttacks.get(starForce);
            return byTier == null ? null : byTier.get(bonusTier);
        }
    }

    private static Map<Integer, Map<Integer, Integer>> buildTemplateAttacks(
            String familyName,
            String bowName,
            int baseAttack,
            int scrollSlots,
            int minStar,
            int maxStar
    ) {
        Map<Integer, Map<Integer, Integer>> templates = new HashMap<>();
        long scrolledBaseAttack = baseAttack + normalizedScrollContribution(familyName, scrollSlots);
        int[] bonusRanks = WEAPON_BONUS_OPTIONS.get(bowName);
        if (bonusRanks == null) {
            return Map.of();
        }
        for (int star = minStar; star <= maxStar; star++) {
            long starforceAttack = starforceContributionForTemplate(familyName, baseAttack, scrollSlots, star);
            Map<Integer, Integer> byTier = new HashMap<>();
            for (int tier = 1; tier <= bonusRanks.length; tier++) {
                byTier.put(tier, Math.toIntExact(scrolledBaseAttack + bonusRanks[tier - 1] + starforceAttack));
            }
            templates.put(star, Map.copyOf(byTier));
        }
        return Map.copyOf(templates);
    }

    private static long starforceContributionForTemplate(String familyName, int baseAttack, int scrollSlots, int starForce) {
        if (starForce <= 0) {
            return 0L;
        }
        long currentAttack = baseAttack + normalizedScrollContribution(familyName, scrollSlots);
        long total = 0L;
        int dynamicStars = Math.min(starForce, 15);
        for (int star = 1; star <= dynamicStars; star++) {
            long delta = currentAttack / 50L + 1L;
            total += delta;
            currentAttack += delta;
        }
        if (starForce >= 16) {
            int[] values = WEAPON_STARFORCE_OPTIONS.get(familyName);
            if (values != null) {
                int index = starForce - 16;
                if (index >= 0 && index < values.length) {
                    total += values[index];
                }
            }
        }
        return total;
    }
}

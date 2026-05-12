package org.whitedoggy.maplehistory.combat;

import java.util.Locale;
import java.util.Set;

record CharacterStatProfile(Set<CombatStatKey> mainStats, Set<CombatStatKey> subStats, boolean usesMagicAttack) {

    static CharacterStatProfile from(String characterClass) {
        String name = characterClass == null ? "" : characterClass.toLowerCase(Locale.ROOT);
        if (name.contains("제논") || name.contains("xenon")) {
            return new CharacterStatProfile(
                    Set.of(CombatStatKey.STR, CombatStatKey.DEX, CombatStatKey.LUK),
                    Set.of(CombatStatKey.STR, CombatStatKey.DEX, CombatStatKey.LUK),
                    false
            );
        }
        if (name.contains("데몬어벤져") || name.contains("demon avenger")) {
            return new CharacterStatProfile(Set.of(CombatStatKey.MAX_HP), Set.of(CombatStatKey.STR), false);
        }
        if (containsAny(name, "비숍", "아크메이지", "플레임위자드", "배틀메이지", "에반", "루미너스", "일리움", "라라", "키네시스")) {
            return new CharacterStatProfile(Set.of(CombatStatKey.INT), Set.of(CombatStatKey.LUK), true);
        }
        if (containsAny(name, "보우마스터", "신궁", "패스파인더", "윈드브레이커", "와일드헌터", "메르세데스", "카인")) {
            return new CharacterStatProfile(Set.of(CombatStatKey.DEX), Set.of(CombatStatKey.STR), false);
        }
        if (containsAny(name, "나이트로드", "섀도어", "듀얼블레이드", "나이트워커", "호영", "칼리", "팬텀")) {
            return new CharacterStatProfile(Set.of(CombatStatKey.LUK), Set.of(CombatStatKey.DEX), false);
        }
        if (containsAny(name, "캡틴", "메카닉", "엔젤릭버스터")) {
            return new CharacterStatProfile(Set.of(CombatStatKey.DEX), Set.of(CombatStatKey.STR), false);
        }
        return new CharacterStatProfile(Set.of(CombatStatKey.STR), Set.of(CombatStatKey.DEX), false);
    }

    private static boolean containsAny(String source, String... needles) {
        for (String needle : needles) {
            if (source.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

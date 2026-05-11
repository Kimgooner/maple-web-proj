package org.whitedoggy.maplehistory.combat;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.whitedoggy.maplehistory.nexon.NexonEndpoint;
import tools.jackson.databind.JsonNode;

@Component
public class CombatStatExtractor {

    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");

    public PresetStatBundle extract(
            Map<NexonEndpoint, JsonNode> documents,
            String characterClass) {
        PresetStatBundle bundle = new PresetStatBundle();
        CharacterStatProfile profile = CharacterStatProfile.from(characterClass);
        documents.forEach((endpoint, document) -> {
            if (document == null || document.isNull() || endpoint == NexonEndpoint.STAT || endpoint == NexonEndpoint.BASIC) {
                return;
            }
            switch (endpoint) {
                case ITEM_EQUIPMENT -> extractItemEquipment(document, bundle);
                case CASH_ITEM_EQUIPMENT -> extractCashItemEquipment(document, bundle);
                case SET_EFFECT -> extractSetEffect(document, bundle.common());
                case SYMBOL_EQUIPMENT -> extractSymbol(document, bundle.common());
                case ABILITY -> extractAbility(document, bundle);
                case HYPER_STAT -> extractHyperStat(document, bundle);
                case PET_EQUIPMENT -> extractPetEquipment(document, bundle.common());
                case UNION_RAIDER -> extractUnionRaider(document, bundle);
                case UNION_CHAMPION -> extractUnionChampion(document, bundle.common());
                case HEXA_MATRIX_STAT -> extractHexaStat(document, bundle, profile);
                case SKILL_0 -> extractSkill0(document, bundle.common());
                default -> extractGeneric(endpoint, document, bundle.common());
            }
        });

        return bundle;
    }

    private void extractItemEquipment(JsonNode document, PresetStatBundle bundle) {
        for (String field : document.propertyNames()) {
            Optional<Integer> preset = presetNo(field);
            if (preset.isPresent()) {
                addItemArray(document.get(field), bundle.sourcePreset("ITEM_EQUIPMENT", preset.get()));
            }
        }
        if (document.has("title") && document.get("title").hasNonNull("title_description")) {
            parseOptionText(
                    NexonEndpoint.ITEM_EQUIPMENT,
                    document.get("title").path("title_name").asText("title"),
                    document.get("title").get("title_description").asText(),
                    bundle.common(),
                    false
            );
        }
        if (bundle.presets().isEmpty() && document.has("item_equipment")) {
            addItemArray(document.get("item_equipment"), bundle.common());
        }
    }

    private void addItemArray(JsonNode items, CombatStatBag bag) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            if (item.hasNonNull("item_total_option")) {
                addStructuredOptions(NexonEndpoint.ITEM_EQUIPMENT, item.get("item_total_option"), bag, false);
            }
            applyWeaponNormalization(item, bag);
            for (String field : item.propertyNames()) {
                if (field.contains("potential_option") || field.contains("soul_option") || field.contains("title")) {
                    JsonNode value = item.get(field);
                    if (value != null && value.isTextual()) {
                        parseOptionText(NexonEndpoint.ITEM_EQUIPMENT, field, value.asText(), bag, false);
                    }
                }
            }
        }
    }

    private void applyWeaponNormalization(JsonNode item, CombatStatBag bag) {
        if (!"무기".equals(item.path("item_equipment_slot").asText())) {
            return;
        }
        String weaponName = item.path("item_name").asText();
        String equipmentPart = item.path("item_equipment_part").asText();
        long originalAttack = parseNumber(item.path("item_total_option").path("attack_power").asText())
                .map(Math::round)
                .orElse(0L);
        long bonusAttack = parseNumber(item.path("item_add_option").path("attack_power").asText())
                .map(Math::round)
                .orElse(0L);
        Optional<Long> normalizedAttack = WeaponNormalizationTable.normalizedBowTotal(weaponName, equipmentPart, bonusAttack);
        if (originalAttack > 0 && normalizedAttack.isPresent()) {
            long normalized = normalizedAttack.get();
            bag.applyWeaponNormalization(new WeaponNormalization(weaponName, originalAttack, normalized, normalized - originalAttack));
        }
    }

    private void extractCashItemEquipment(JsonNode document, PresetStatBundle bundle) {
        addCashItemArray(document.get("cash_item_equipment_base"), bundle.common());
    }

    private void addCashItemArray(JsonNode items, CombatStatBag bag) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            JsonNode options = item.get("cash_item_option");
            if (options == null || !options.isArray()) {
                continue;
            }
            for (JsonNode option : options) {
                if (option.hasNonNull("option_type") && option.hasNonNull("option_value")) {
                    parseOptionText(NexonEndpoint.CASH_ITEM_EQUIPMENT, "cash_item_option", option.get("option_type").asText() + " " + option.get("option_value").asText(), bag, false);
                }
            }
        }
    }

    private void extractSetEffect(JsonNode document, CombatStatBag bag) {
        JsonNode setEffects = document.get("set_effect");
        if (setEffects == null || !setEffects.isArray()) {
            return;
        }
        for (JsonNode setEffect : setEffects) {
            JsonNode activeEffects = setEffect.get("set_effect_info");
            if (activeEffects == null || !activeEffects.isArray()) {
                continue;
            }
            for (JsonNode activeEffect : activeEffects) {
                if (activeEffect.hasNonNull("set_option")) {
                    parseOptionText(NexonEndpoint.SET_EFFECT, setEffect.path("set_name").asText("set_option"), activeEffect.get("set_option").asText(), bag, false);
                }
            }
        }
    }

    private void extractSymbol(JsonNode document, CombatStatBag bag) {
        JsonNode symbols = document.get("symbol");
        if (symbols == null || !symbols.isArray()) {
            return;
        }
        for (JsonNode symbol : symbols) {
            addFinalFlatIfPresent(NexonEndpoint.SYMBOL_EQUIPMENT, symbol, "symbol_str", CombatStatKey.STR, bag);
            addFinalFlatIfPresent(NexonEndpoint.SYMBOL_EQUIPMENT, symbol, "symbol_dex", CombatStatKey.DEX, bag);
            addFinalFlatIfPresent(NexonEndpoint.SYMBOL_EQUIPMENT, symbol, "symbol_int", CombatStatKey.INT, bag);
            addFinalFlatIfPresent(NexonEndpoint.SYMBOL_EQUIPMENT, symbol, "symbol_luk", CombatStatKey.LUK, bag);
            addFinalFlatIfPresent(NexonEndpoint.SYMBOL_EQUIPMENT, symbol, "symbol_hp", CombatStatKey.MAX_HP, bag);
        }
    }

    private void extractAbility(JsonNode document, PresetStatBundle bundle) {
        for (int presetNo = 1; presetNo <= 3; presetNo++) {
            JsonNode preset = document.get("ability_preset_" + presetNo);
            if (preset != null && preset.has("ability_info")) {
                addAbilityInfo(preset.get("ability_info"), bundle.sourcePreset("ABILITY", presetNo));
            }
        }
    }

    private void addAbilityInfo(JsonNode abilityInfo, CombatStatBag bag) {
        if (abilityInfo == null || !abilityInfo.isArray()) {
            return;
        }
        for (JsonNode ability : abilityInfo) {
            if (ability.hasNonNull("ability_value")) {
                parseOptionText(NexonEndpoint.ABILITY, "ability_value", ability.get("ability_value").asText(), bag, true);
            }
        }
    }

    private void extractHyperStat(JsonNode document, PresetStatBundle bundle) {
        for (int presetNo = 1; presetNo <= 3; presetNo++) {
            JsonNode preset = document.get("hyper_stat_preset_" + presetNo);
            if (preset != null) {
                addHyperStatArray(preset, bundle.sourcePreset("HYPER_STAT", presetNo));
            }
        }
    }

    private void extractPetEquipment(JsonNode document, CombatStatBag bag) {
        for (int petNo = 1; petNo <= 3; petNo++) {
            JsonNode equipment = document.get("pet_" + petNo + "_equipment");
            if (equipment == null || equipment.isNull()) {
                continue;
            }
            JsonNode options = equipment.get("item_option");
            if (options == null || !options.isArray()) {
                continue;
            }
            for (JsonNode option : options) {
                if (option.hasNonNull("option_type") && option.hasNonNull("option_value")) {
                    parseOptionText(NexonEndpoint.PET_EQUIPMENT, "pet_" + petNo + "_equipment", option.get("option_type").asText() + " " + option.get("option_value").asText(), bag, false);
                }
            }
        }
    }


    private void addHyperStatArray(JsonNode preset, CombatStatBag bag) {
        if (!preset.isArray()) {
            return;
        }
        for (JsonNode stat : preset) {
            if (stat.hasNonNull("stat_increase")) {
                parseOptionText(NexonEndpoint.HYPER_STAT, "stat_increase", stat.get("stat_increase").asText(), bag, true);
            }
        }
    }

    private void extractUnionRaider(JsonNode document, PresetStatBundle bundle) {
        boolean hasPreset = false;
        for (int presetNo = 1; presetNo <= 5; presetNo++) {
            JsonNode preset = document.get("union_raider_preset_" + presetNo);
            if (preset == null || preset.isNull()) {
                continue;
            }
            hasPreset = true;
            CombatStatBag presetBag = bundle.sourcePreset("UNION_RAIDER", presetNo);
            addStringArray(NexonEndpoint.UNION_RAIDER, preset.get("union_raider_stat"), "union_raider_preset_" + presetNo + "_stat", presetBag, true);
            addStringArray(NexonEndpoint.UNION_RAIDER, preset.get("union_occupied_stat"), "union_raider_preset_" + presetNo + "_occupied", presetBag, false);
        }
        if (!hasPreset) {
            addStringArray(NexonEndpoint.UNION_RAIDER, document.get("union_raider_stat"), "union_raider_stat", bundle.common(), true);
            addStringArray(NexonEndpoint.UNION_RAIDER, document.get("union_occupied_stat"), "union_occupied_stat", bundle.common(), false);
        }
    }

    private void extractUnionChampion(JsonNode document, CombatStatBag bag) {
        JsonNode totals = document.get("champion_badge_total_info");
        if (totals == null || !totals.isArray()) {
            return;
        }
        for (JsonNode total : totals) {
            if (total.hasNonNull("stat")) {
                parseOptionText(NexonEndpoint.UNION_CHAMPION, "champion_badge_total_info", total.get("stat").asText(), bag, false);
            }
        }
    }

    private void addStringArray(NexonEndpoint endpoint, JsonNode values, String label, CombatStatBag bag, boolean finalFlat) {
        if (values == null || !values.isArray()) {
            return;
        }
        for (JsonNode value : values) {
            if (value.isTextual()) {
                parseOptionText(endpoint, label, value.asText(), bag, finalFlat);
            }
        }
    }

    private void extractHexaStat(JsonNode document, PresetStatBundle bundle, CharacterStatProfile profile) {
        addHexaCores(document.get("preset_hexa_stat_core"), bundle.common(), 1, profile);
        addHexaCores(document.get("preset_hexa_stat_core_2"), bundle.common(), 2, profile);
        addHexaCores(document.get("preset_hexa_stat_core_3"), bundle.common(), 3, profile);
    }

    private void addHexaCores(JsonNode cores, CombatStatBag bag, int coreNo, CharacterStatProfile profile) {
        if (cores == null || !cores.isArray()) {
            return;
        }
        for (JsonNode core : cores) {
            addHexaStatLine(core.path("main_stat_name").asText(null), core.path("main_stat_level").asInt(0), true, coreNo, bag, profile);
            addHexaStatLine(core.path("sub_stat_name_1").asText(null), core.path("sub_stat_level_1").asInt(0), false, coreNo, bag, profile);
            addHexaStatLine(core.path("sub_stat_name_2").asText(null), core.path("sub_stat_level_2").asInt(0), false, coreNo, bag, profile);
        }
    }

    private void addHexaStatLine(String name, int level, boolean main, int coreNo, CombatStatBag bag, CharacterStatProfile profile) {
        CombatStatKey mainStat = profile.mainStats().iterator().next();
        if (name == null || level <= 0) {
            return;
        }
        double value = hexaValue(name, level, main);
        String label = "hexa_core_" + coreNo + (main ? "_main" : "_sub");
        if (name.contains("주력 스탯")) {
            bag.addFinalFlat(mainStat, Math.round(value), NexonEndpoint.HEXA_MATRIX_STAT.name(), label, name + " Lv." + level);
        } else if (name.contains("공격력")) {
            bag.addFlat(CombatStatKey.ATTACK_POWER, Math.round(value), NexonEndpoint.HEXA_MATRIX_STAT.name(), label, name + " Lv." + level);
            bag.addFlat(CombatStatKey.MAGIC_ATTACK, Math.round(value), NexonEndpoint.HEXA_MATRIX_STAT.name(), label, name + " Lv." + level);
        } else if (name.contains("보스")) {
            bag.addPercent(CombatStatKey.BOSS_DAMAGE, value, NexonEndpoint.HEXA_MATRIX_STAT.name(), label, name + " Lv." + level);
        } else if ("데미지 증가".equals(name)) {
            bag.addPercent(CombatStatKey.DAMAGE, value, NexonEndpoint.HEXA_MATRIX_STAT.name(), label, name + " Lv." + level);
        } else if (name.contains("크리티컬")) {
            bag.addPercent(CombatStatKey.CRITICAL_DAMAGE, value, NexonEndpoint.HEXA_MATRIX_STAT.name(), label, name + " Lv." + level);
        }
    }

    private double hexaValue(String name, int level, boolean main) {
        int capped = Math.max(1, Math.min(level, 10));
        int[] mainStat = {0, 100, 200, 300, 400, 600, 800, 1000, 1300, 1600, 2000};
        int[] subStat = {0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        int[] mainAttack = {0, 5, 10, 15, 20, 30, 40, 50, 65, 80, 100};
        int[] subAttack = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
        double[] mainBoss = {0, 1, 2, 3, 4, 6, 8, 10, 13, 16, 20};
        double[] subBoss = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double[] mainDamage = {0, 0.75, 1.5, 2.25, 3, 4.5, 6, 7.5, 9.75, 12, 15};
        double[] subDamage = {0, 0.75, 1.5, 2.25, 3, 3.75, 4.5, 5.25, 6, 6.75, 7.5};
        double[] mainCritical = {0, 0.35, 0.7, 1.05, 1.4, 2.1, 2.8, 3.5, 4.55, 5.6, 7};
        double[] subCritical = {0, 0.35, 0.7, 1.05, 1.4, 1.75, 2.1, 2.45, 2.8, 3.15, 3.5};
        if (name.contains("주력 스탯")) {
            return main ? mainStat[capped] : subStat[capped];
        }
        if (name.contains("공격력")) {
            return main ? mainAttack[capped] : subAttack[capped];
        }
        if (name.contains("보스")) {
            return main ? mainBoss[capped] : subBoss[capped];
        }
        if ("데미지 증가".equals(name)) {
            return main ? mainDamage[capped] : subDamage[capped];
        }
        if (name.contains("크리티컬")) {
            return main ? mainCritical[capped] : subCritical[capped];
        }
        return 0;
    }

    private void extractSkill0(JsonNode document, CombatStatBag bag) {
        JsonNode skills = document.get("character_skill");
        if (skills == null || !skills.isArray()) {
            return;
        }
        int blessingAttack = 0;
        String blessingSource = null;
        for (JsonNode skill : skills) {
            String name = skill.path("skill_name").asText();
            String effect = skill.path("skill_effect").asText();
            if ("정령의 축복".equals(name) || "여제의 축복".equals(name)) {
                int attack = firstInt(effect).orElse(0);
                if (attack > blessingAttack) {
                    blessingAttack = attack;
                    blessingSource = name;
                }
            } else if (isCombatPowerEligibleBeginnerSkill(name, effect)) {
                parseOptionText(NexonEndpoint.SKILL_0, name, effect, bag, false);
            }
        }
        if (blessingAttack > 0) {
            bag.addFlat(CombatStatKey.ATTACK_POWER, blessingAttack, NexonEndpoint.SKILL_0.name(), blessingSource, blessingAttack + "");
            bag.addFlat(CombatStatKey.MAGIC_ATTACK, blessingAttack, NexonEndpoint.SKILL_0.name(), blessingSource, blessingAttack + "");
        }
    }

    private boolean isCombatPowerEligibleBeginnerSkill(String name, String effect) {
        if (name == null || effect == null || effect.isBlank()) {
            return false;
        }
        if ("파괴의 얄다바오트".equals(name)
                || "초월 : 불굴의 결의".equals(name)
                || "초월 : 결전의 의지".equals(name)
                || "메이플 스위츠".equals(name)
        ) {
            return true;
        }
        if (name.contains("Lv")) return true;
        return false;
    }

    private void extractGeneric(NexonEndpoint endpoint, JsonNode document, CombatStatBag bag) {
        if (document.isObject()) {
            if (document.hasNonNull("name") && document.hasNonNull("stat_value")) {
                parseOptionText(endpoint, document.get("name").asText(), document.get("name").asText() + " " + document.get("stat_value").asText(), bag, isFinalFlatEndpoint(endpoint));
            }
            if (document.hasNonNull("name")) {
                parseOptionText(endpoint, "name", document.get("name").asText(), bag, isFinalFlatEndpoint(endpoint));
            }
            if (document.hasNonNull("stat_value") && document.hasNonNull("stat_name")) {
                parseOptionText(endpoint, document.get("stat_name").asText(), document.get("stat_value").asText(), bag, isFinalFlatEndpoint(endpoint));
            }
            for (JsonNode child : document.values()) {
                extractGeneric(endpoint, child, bag);
            }
        } else if (document.isArray()) {
            for (JsonNode child : document) {
                extractGeneric(endpoint, child, bag);
            }
        }
    }

    private void addStructuredOptions(NexonEndpoint endpoint, JsonNode options, CombatStatBag bag, boolean finalFlat) {
        addFlatIfPresent(endpoint, options, "str", CombatStatKey.STR, bag, finalFlat);
        addFlatIfPresent(endpoint, options, "dex", CombatStatKey.DEX, bag, finalFlat);
        addFlatIfPresent(endpoint, options, "int", CombatStatKey.INT, bag, finalFlat);
        addFlatIfPresent(endpoint, options, "luk", CombatStatKey.LUK, bag, finalFlat);
        addFlatIfPresent(endpoint, options, "max_hp", CombatStatKey.MAX_HP, bag, finalFlat);
        addPercentIfPresent(endpoint, options, "all_stat", CombatStatKey.ALL_STAT, bag);
        addFlatIfPresent(endpoint, options, "attack_power", CombatStatKey.ATTACK_POWER, bag, finalFlat);
        addFlatIfPresent(endpoint, options, "magic_power", CombatStatKey.MAGIC_ATTACK, bag, finalFlat);
        addPercentIfPresent(endpoint, options, "damage", CombatStatKey.DAMAGE, bag);
        addPercentIfPresent(endpoint, options, "boss_damage", CombatStatKey.BOSS_DAMAGE, bag);
    }

    private void parseOptionText(NexonEndpoint endpoint, String label, String text, CombatStatBag bag, boolean finalFlat) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (text.contains("STR, DEX, LUK")) {
            addOption(endpoint, label, text.trim(), bag, finalFlat);
            return;
        }
        for (String part : text.split("[,\\n\\r]+")) {
            String option = part.trim();
            if (option.isBlank() || ignoredOption(option)) {
                continue;
            }
            addOption(endpoint, label, option, bag, finalFlat);
        }
    }

    private void addOption(NexonEndpoint endpoint, String label, String option, CombatStatBag bag, boolean finalFlat) {
        Optional<Double> number = parseNumber(option);
        if (number.isEmpty()) {
            return;
        }
        double value = number.get();
        boolean percent = option.contains("%");
        if (option.contains("최종 데미지")) {
            bag.addPercent(CombatStatKey.FINAL_DAMAGE, value, endpoint.name(), label, option);
        } else if (option.contains("보스") && option.contains("데미지")) {
            bag.addPercent(CombatStatKey.BOSS_DAMAGE, value, endpoint.name(), label, option);
        } else if (option.contains("크리티컬 데미지")) {
            bag.addPercent(CombatStatKey.CRITICAL_DAMAGE, value, endpoint.name(), label, option);
        } else if (option.contains("데미지")) {
            bag.addPercent(CombatStatKey.DAMAGE, value, endpoint.name(), label, option);
        } else if (option.contains("공격력") && option.contains("마력")) {
            addNumber(endpoint, label, option, CombatStatKey.ATTACK_POWER, value, percent, finalFlat, bag);
            addNumber(endpoint, label, option, CombatStatKey.MAGIC_ATTACK, value, percent, finalFlat, bag);
        } else if (option.contains("STR") && option.contains("DEX") && option.contains("LUK")) {
            addNumber(endpoint, label, option, CombatStatKey.STR, value, percent, finalFlat, bag);
            addNumber(endpoint, label, option, CombatStatKey.DEX, value, percent, finalFlat, bag);
            addNumber(endpoint, label, option, CombatStatKey.LUK, value, percent, finalFlat, bag);
        } else if (option.contains("공격력")) {
            addNumber(endpoint, label, option, CombatStatKey.ATTACK_POWER, value, percent, finalFlat, bag);
        } else if (option.contains("마력")) {
            addNumber(endpoint, label, option, CombatStatKey.MAGIC_ATTACK, value, percent, finalFlat, bag);
        } else if (option.contains("올스탯")) {
            addNumber(endpoint, label, option, CombatStatKey.ALL_STAT, value, percent, finalFlat, bag);
        } else if (option.contains("STR") || option.contains("힘")) {
            addNumber(endpoint, label, option, CombatStatKey.STR, value, percent, finalFlat, bag);
        } else if (option.contains("DEX") || option.contains("민첩")) {
            addNumber(endpoint, label, option, CombatStatKey.DEX, value, percent, finalFlat, bag);
        } else if (option.contains("INT") || option.contains("지력") || option.contains("지능")) {
            addNumber(endpoint, label, option, CombatStatKey.INT, value, percent, finalFlat, bag);
        } else if (option.contains("LUK") || option.contains("행운")) {
            addNumber(endpoint, label, option, CombatStatKey.LUK, value, percent, finalFlat, bag);
        } else if (option.contains("최대 HP") || option.contains("MaxHP") || option.contains("MAX HP")) {
            addNumber(endpoint, label, option, CombatStatKey.MAX_HP, value, percent, finalFlat, bag);
        }
    }

    private boolean ignoredOption(String option) {
        return option.contains("몬스터 방어율 무시")
                || option.contains("방어율 무시")
                || option.contains("상태 이상")
                || option.contains("일반 몬스터")
                || option.contains("확률")
                || option.contains("아이템 드롭")
                || option.contains("드롭률")
                || option.contains("메소 획득")
                || option.contains("경험치")
                || option.contains("버프 지속")
                || option.contains("어센틱포스")
                || option.contains("아케인포스")
                || option.contains("스킬 사용 가능");
    }

    private void addNumber(NexonEndpoint endpoint, String label, String option, CombatStatKey key, double value, boolean percent, boolean finalFlat, CombatStatBag bag) {
        if (percent) {
            bag.addPercent(key, value, endpoint.name(), label, option);
        } else if (finalFlat && key != CombatStatKey.ATTACK_POWER && key != CombatStatKey.MAGIC_ATTACK) {
            bag.addFinalFlat(key, Math.round(value), endpoint.name(), label, option);
        } else {
            bag.addFlat(key, Math.round(value), endpoint.name(), label, option);
        }
    }

    private void addFlatIfPresent(NexonEndpoint endpoint, JsonNode node, String field, CombatStatKey key, CombatStatBag bag, boolean finalFlat) {
        if (node == null || !node.hasNonNull(field)) {
            return;
        }
        long value = parseNumber(node.get(field).asText()).map(Math::round).orElse(0L);
        if (value == 0L) {
            return;
        }
        if (finalFlat && key != CombatStatKey.ATTACK_POWER && key != CombatStatKey.MAGIC_ATTACK) {
            bag.addFinalFlat(key, value, endpoint.name(), field, node.get(field).asText());
        } else {
            bag.addFlat(key, value, endpoint.name(), field, node.get(field).asText());
        }
    }

    private void addFinalFlatIfPresent(NexonEndpoint endpoint, JsonNode node, String field, CombatStatKey key, CombatStatBag bag) {
        addFlatIfPresent(endpoint, node, field, key, bag, true);
    }

    private void addPercentIfPresent(NexonEndpoint endpoint, JsonNode node, String field, CombatStatKey key, CombatStatBag bag) {
        if (node == null || !node.hasNonNull(field)) {
            return;
        }
        double value = parseNumber(node.get(field).asText()).orElse(0.0d);
        if (value != 0.0d) {
            bag.addPercent(key, value, endpoint.name(), field, node.get(field).asText());
        }
    }

    private Optional<Integer> presetNo(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        if (normalized.contains("preset_1") || normalized.endsWith("preset1")) {
            return Optional.of(1);
        }
        if (normalized.contains("preset_2") || normalized.endsWith("preset2")) {
            return Optional.of(2);
        }
        if (normalized.contains("preset_3") || normalized.endsWith("preset3")) {
            return Optional.of(3);
        }
        return Optional.empty();
    }

    private Optional<Double> parseNumber(String value) {
        Matcher matcher = NUMBER.matcher(value.replace(",", ""));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Double.parseDouble(matcher.group()));
    }

    private Optional<Integer> firstInt(String value) {
        return parseNumber(value).map(Double::intValue);
    }

    private boolean isFinalFlatEndpoint(NexonEndpoint endpoint) {
        return endpoint == NexonEndpoint.SYMBOL_EQUIPMENT
                || endpoint == NexonEndpoint.HYPER_STAT
                || endpoint == NexonEndpoint.HEXA_MATRIX_STAT;
    }
}

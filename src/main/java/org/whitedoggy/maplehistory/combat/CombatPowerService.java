package org.whitedoggy.maplehistory.combat;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.whitedoggy.maplehistory.config.NexonApiProperties;
import org.whitedoggy.maplehistory.nexon.CharacterIdentity;
import org.whitedoggy.maplehistory.nexon.NexonCharacterSnapshot;
import org.whitedoggy.maplehistory.nexon.NexonEndpoint;
import org.whitedoggy.maplehistory.nexon.NexonMapleClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CombatPowerService {

    private final NexonMapleClient nexonMapleClient;
    private final CombatStatExtractor statExtractor;
    private final CombatPowerFormula formula;
    private final NexonApiProperties properties;

    public CombatPowerService(
            NexonMapleClient nexonMapleClient,
            CombatStatExtractor statExtractor,
            CombatPowerFormula formula,
            NexonApiProperties properties
    ) {
        this.nexonMapleClient = nexonMapleClient;
        this.statExtractor = statExtractor;
        this.formula = formula;
        this.properties = properties;
    }

    public Mono<CombatPowerTrendResponse> trend(String characterName, LocalDate from, LocalDate to, PresetSelectionMode mode) {
        if (to.isBefore(from)) {
            return Mono.error(new IllegalArgumentException("to must be equal to or after from"));
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return nexonMapleClient.findCharacter(characterName)
                .flatMap(identity -> Flux.range(0, Math.toIntExact(days))
                        .map(from::plusDays)
                        .flatMap(date -> nexonMapleClient.fetchSnapshot(identity.ocid(), date)
                                .map(snapshot -> pointContext(snapshot, mode)), properties.maxConcurrency())
                        .sort(Comparator.comparing(context -> context.point().date()))
                        .collectList()
                        .map(contexts -> response(characterName, identity, from, to, mode, enrichChanges(contexts))));
    }

    public Mono<CombatPowerDebugResponse> debug(String characterName, LocalDate date, PresetSelectionMode mode) {
        return nexonMapleClient.findCharacter(characterName)
                .flatMap(identity -> nexonMapleClient.fetchSnapshot(identity.ocid(), date)
                        .map(snapshot -> debugResponse(characterName, identity.ocid(), snapshot, mode)));
    }

    private CombatPowerTrendResponse response(
            String characterName,
            CharacterIdentity identity,
            LocalDate from,
            LocalDate to,
            PresetSelectionMode mode,
            List<CombatPowerPoint> points
    ) {
        return new CombatPowerTrendResponse(characterName, identity.ocid(), from, to, mode, points);
    }

    private TrendPointContext pointContext(NexonCharacterSnapshot snapshot, PresetSelectionMode mode) {
        Evaluation evaluation = evaluate(snapshot, mode);
        CombatPowerPoint point = new CombatPowerPoint(
                snapshot.date(),
                evaluation.selected().presetNo(),
                evaluation.selected().sourcePresetNos(),
                evaluation.selected().combatPower(),
                evaluation.nexonCombatPower(),
                evaluation.delta(),
                evaluation.deltaRate(),
                evaluation.nexonCombatPower() != null && evaluation.currentPresetSelected(),
                evaluation.selected().formula(),
                evaluation.candidates(),
                warnings(snapshot, evaluation.currentPresetNos(), evaluation.nexonCombatPower(), evaluation.currentPresetSelected()),
                null
        );
        return new TrendPointContext(snapshot, point, evaluation.selectedStats(), mode);
    }

    private CombatPowerPoint point(NexonCharacterSnapshot snapshot, PresetSelectionMode mode) {
        return pointContext(snapshot, mode).point();
    }

    private Evaluation evaluate(NexonCharacterSnapshot snapshot, PresetSelectionMode mode) {
        String characterClass = characterClass(snapshot);
        Integer characterLevel = characterLevel(snapshot).orElse(null);
        PresetStatBundle bundle = statExtractor.extract(snapshot.documents(), characterClass);
        Map<String, Integer> currentPresetNos = currentPresetNos(snapshot);
        List<PresetCombatPower> candidates = candidates(snapshot, bundle, characterClass, characterLevel, currentPresetNos, mode);
        PresetCombatPower selected = selectCandidate(candidates, mode);
        CombatStatBag selectedStats = bundle.merged(selected.sourcePresetNos());
        Long nexonCombatPower = finalStatCombatPower(snapshot.document(NexonEndpoint.STAT)).orElse(null);
        boolean currentPresetSelected = selected.sourcePresetNos().equals(currentPresetNos)
                || mode == PresetSelectionMode.CURRENT;
        Long delta = nexonCombatPower == null || !currentPresetSelected ? null : selected.combatPower() - nexonCombatPower;
        Double deltaRate = delta == null || nexonCombatPower == 0
                ? null
                : delta / (double) nexonCombatPower;
        printSelectedStatDebug(
                "(web trend)",
                snapshot,
                mode,
                selected,
                selectedStats,
                nexonCombatPower,
                delta,
                deltaRate
        );
        return new Evaluation(
                characterClass,
                characterLevel,
                currentPresetNos,
                candidates,
                selected,
                selectedStats,
                nexonCombatPower,
                currentPresetSelected,
                delta,
                deltaRate
        );
    }

    private CombatPowerDebugResponse debugResponse(String characterName, String ocid, NexonCharacterSnapshot snapshot, PresetSelectionMode mode) {
        Evaluation evaluation = evaluate(snapshot, mode);
        printSelectedStatDebug(
                characterName,
                snapshot,
                mode,
                evaluation.selected(),
                evaluation.selectedStats(),
                evaluation.nexonCombatPower(),
                evaluation.delta(),
                evaluation.deltaRate()
        );

        return new CombatPowerDebugResponse(
                characterName,
                ocid,
                snapshot.date(),
                evaluation.characterClass(),
                evaluation.characterLevel(),
                mode,
                evaluation.selected().presetNo(),
                evaluation.selected().sourcePresetNos(),
                evaluation.nexonCombatPower(),
                evaluation.delta(),
                evaluation.deltaRate(),
                evaluation.selected().formula(),
                trace(evaluation.selected().formula()),
                totals(evaluation.selectedStats()),
                evaluation.selectedStats().weaponNormalization(),
                sourceTotals(evaluation.selectedStats().contributions()),
                evaluation.selectedStats().contributions(),
                evaluation.candidates()
        );
    }

    private List<CombatPowerPoint> enrichChanges(List<TrendPointContext> contexts) {
        List<CombatPowerPoint> points = new ArrayList<>();
        TrendPointContext previous = null;
        for (TrendPointContext current : contexts) {
            CombatPowerChange change = previous == null ? null : compare(previous, current);
            CombatPowerPoint point = new CombatPowerPoint(
                    current.point().date(),
                    current.point().selectedPresetNo(),
                    current.point().selectedSourcePresetNos(),
                    current.point().calculatedCombatPower(),
                    current.point().nexonCurrentCombatPower(),
                    current.point().verificationDelta(),
                    current.point().verificationDeltaRate(),
                    current.point().verifiedAgainstCurrentPreset(),
                    current.point().formula(),
                    current.point().presetCandidates(),
                    current.point().warnings(),
                    change
            );
            points.add(point);
            previous = current;
        }
        return points;
    }

    private CombatPowerChange compare(TrendPointContext previous, TrendPointContext current) {
        return new CombatPowerChange(
                previous.point().date(),
                totalChanges(previous.selectedStats(), current.selectedStats()),
                detailChanges(previous, current)
        );
    }

    private FormulaTrace trace(CombatPowerCalculation calculation) {
        double statFactor = (calculation.mainStat() * 4.0d + calculation.subStat()) / 100.0d;
        return new FormulaTrace(
                "(mainStat * 4 + subStat) / 100 = (" + calculation.mainStat() + " * 4 + " + calculation.subStat() + ") / 100 = " + statFactor,
                "floor(baseAttack * (1 + attackPercent / 100)) = " + calculation.attackPower(),
                "1 + damage% + bossDamage% = " + calculation.damageFactor(),
                "1.35 + criticalDamage% = " + calculation.criticalDamageFactor(),
                "1 + finalDamage% = " + calculation.finalDamageFactor(),
                statFactor + " * " + calculation.attackPower() + " * " + calculation.damageFactor() + " * " + calculation.criticalDamageFactor() + " * " + calculation.finalDamageFactor() + " = " + calculation.combatPower()
        );
    }

    private StatTotals totals(CombatStatBag bag) {
        return new StatTotals(longMap(bag.flatValues()), longMap(bag.finalFlatValues()), doubleMap(bag.percentValues()));
    }

    private Map<String, SourceTotals> sourceTotals(List<StatContribution> contributions) {
        return contributions.stream()
                .collect(Collectors.groupingBy(
                        StatContribution::endpoint,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), this::sourceTotal)
                ));
    }

    private SourceTotals sourceTotal(List<StatContribution> contributions) {
        return new SourceTotals(
                bucketTotal(contributions, "PERCENT_APPLIED_FLAT"),
                bucketTotal(contributions, "PERCENT_NOT_APPLIED_FLAT"),
                bucketTotal(contributions, "PERCENT")
        );
    }

    private Map<String, Double> bucketTotal(List<StatContribution> contributions, String bucket) {
        return contributions.stream()
                .filter(contribution -> bucket.equals(contribution.bucket()))
                .collect(Collectors.groupingBy(
                        StatContribution::statKey,
                        LinkedHashMap::new,
                        Collectors.summingDouble(StatContribution::parsedValue)
                ));
    }

    private List<PresetCombatPower> candidates(
            NexonCharacterSnapshot snapshot,
            PresetStatBundle bundle,
            String characterClass,
            Integer characterLevel,
            Map<String, Integer> currentPresetNos,
            PresetSelectionMode mode
    ) {
        List<Map<String, Integer>> combinations = new ArrayList<>();
        if (mode == PresetSelectionMode.CURRENT) {
            combinations.add(currentPresetNos);
        } else if (mode == PresetSelectionMode.MAX) {
            buildCombinations(new ArrayList<>(bundle.sources()), bundle, 0, new LinkedHashMap<>(), combinations);
            if (combinations.isEmpty()) {
                combinations.add(Map.of());
            }
        } else if (mode == PresetSelectionMode.BATTLE) {
            combinations.add(battlePresetNos(snapshot, bundle, currentPresetNos, characterClass, characterLevel));
        } else {
            int presetNo = mode.presetNoOrZero();
            Map<String, Integer> selected = new LinkedHashMap<>();
            for (String source : bundle.sources()) {
                if (bundle.sourcePresetNos(source).contains(presetNo)) {
                    selected.put(source, presetNo);
                } else if (currentPresetNos.containsKey(source)) {
                    selected.put(source, currentPresetNos.get(source));
                }
            }
            combinations.add(selected);
        }

        return combinations.stream()
                .map(presetNos -> {
                    CombatPowerCalculation calculation = formula.calculate(characterClass, characterLevel, bundle.merged(presetNos));
                    int displayPresetNo = presetNos.values().stream().findFirst().orElse(0);
                    return new PresetCombatPower(displayPresetNo, Map.copyOf(presetNos), calculation.combatPower(), calculation);
                })
                .sorted(Comparator.comparing(PresetCombatPower::combatPower).reversed())
                .toList();
    }

    private void buildCombinations(
            List<String> sources,
            PresetStatBundle bundle,
            int index,
            Map<String, Integer> current,
            List<Map<String, Integer>> results
    ) {
        if (index >= sources.size()) {
            results.add(Map.copyOf(current));
            return;
        }
        String source = sources.get(index);
        for (Integer presetNo : bundle.sourcePresetNos(source)) {
            current.put(source, presetNo);
            buildCombinations(sources, bundle, index + 1, current, results);
        }
        current.remove(source);
    }

    private Map<String, Integer> battlePresetNos(
            NexonCharacterSnapshot snapshot,
            PresetStatBundle bundle,
            Map<String, Integer> currentPresetNos,
            String characterClass,
            Integer characterLevel
    ) {
        Map<String, Integer> selected = new LinkedHashMap<>();
        for (String source : bundle.sources()) {
            if ("UNION_RAIDER".equals(source)) {
                continue;
            }
            Optional<Integer> best = bundle.sourcePresetNos(source).stream()
                    .min(Comparator
                            .comparingInt((Integer presetNo) -> -battlePresetScore(snapshot, source, presetNo, characterClass))
                            .thenComparingInt(presetNo -> currentPresetNos.getOrDefault(source, -1).equals(presetNo) ? 0 : 1)
                    .thenComparingInt(Integer::intValue));
            best.ifPresent(presetNo -> selected.put(source, presetNo));
        }
        if (bundle.sources().contains("UNION_RAIDER")) {
            Optional<Integer> bestUnion = bundle.sourcePresetNos("UNION_RAIDER").stream()
                    .min(Comparator
                            .comparingLong((Integer presetNo) -> -combatPowerWithPreset(bundle, selected, "UNION_RAIDER", presetNo, characterClass, characterLevel))
                            .thenComparingInt(presetNo -> -battlePresetScore(snapshot, "UNION_RAIDER", presetNo, characterClass))
                            .thenComparingInt(presetNo -> currentPresetNos.getOrDefault("UNION_RAIDER", -1).equals(presetNo) ? 0 : 1)
                            .thenComparingInt(Integer::intValue));
            bestUnion.ifPresent(presetNo -> selected.put("UNION_RAIDER", presetNo));
        }
        return selected;
    }

    private long combatPowerWithPreset(
            PresetStatBundle bundle,
            Map<String, Integer> selected,
            String source,
            int presetNo,
            String characterClass,
            Integer characterLevel
    ) {
        Map<String, Integer> candidate = new LinkedHashMap<>(selected);
        candidate.put(source, presetNo);
        return formula.calculate(characterClass, characterLevel, bundle.merged(candidate)).combatPower();
    }

    private int battlePresetScore(NexonCharacterSnapshot snapshot, String source, int presetNo, String characterClass) {
        return switch (source) {
            case "ITEM_EQUIPMENT" -> itemBattleScore(snapshot.document(NexonEndpoint.ITEM_EQUIPMENT), presetNo);
            case "HYPER_STAT" -> hyperStatBattleScore(snapshot.document(NexonEndpoint.HYPER_STAT), presetNo);
            case "ABILITY" -> abilityBattleScore(snapshot.document(NexonEndpoint.ABILITY), presetNo, characterClass);
            case "UNION_RAIDER" -> unionBattleScore(snapshot.document(NexonEndpoint.UNION_RAIDER), presetNo);
            default -> 0;
        };
    }

    private int itemBattleScore(JsonNode document, int presetNo) {
        JsonNode items = document == null ? null : document.get("item_equipment_preset_" + presetNo);
        if (items == null || !items.isArray()) {
            return 0;
        }
        int score = 0;
        for (JsonNode item : items) {
            String name = item.path("item_name").asText("");
            String part = item.path("item_equipment_part").asText("");
            String slot = item.path("item_equipment_slot").asText("");
            String text = flattenText(item);
            if (containsAny(text, "컨티뉴어스 링", "리스트레인트 링", "웨폰퍼프", "리스크테이커", "링 오브 썸", "크라이시스", "리밋링", "레벨퍼프", "얼티메이덤")) {
                score += 80;
            }
            if (containsAny(text, "아이템 드롭률", "메소 획득량", "획득 경험치", "드롭률")) {
                score -= 80;
            }
            if (name.contains("하프 이어링")) {
                score -= 60;
            }
            if (name.contains("정령의 펜던트")) {
                score -= 60;
            }
            if ((slot.contains("눈장식") || part.contains("눈장식")) && containsAny(name, "안경", "안대")) {
                score -= 20;
            }
            if ((slot.contains("얼굴장식") || part.contains("얼굴장식")) && name.contains("심볼")) {
                score -= 30;
            }
        }
        return score;
    }

    private int hyperStatBattleScore(JsonNode document, int presetNo) {
        JsonNode stats = document == null ? null : document.get("hyper_stat_preset_" + presetNo);
        if (stats == null || !stats.isArray()) {
            return 0;
        }
        int score = 0;
        for (JsonNode stat : stats) {
            String type = stat.path("stat_type").asText("");
            int level = intField(stat, "stat_level").orElse(0);
            int point = intField(stat, "stat_point").orElse(0);
            int invested = Math.max(level, point);
            if (containsAny(type, "보스 몬스터 데미지", "데미지", "크리티컬 데미지", "방어율 무시", "공격력/마력", "STR", "DEX", "INT", "LUK")) {
                score += invested * 4;
            }
            if (type.contains("방어율 무시") && invested > 0) {
                score += 20;
            }
            if (containsAny(type, "경험치", "일반 몬스터 데미지", "아케인포스") && invested >= 4) {
                score -= invested * 8;
            }
            if (containsAny(type, "미투자", "잔여") && invested > 20) {
                score -= invested;
            }
        }
        return score;
    }

    private int abilityBattleScore(JsonNode document, int presetNo, String characterClass) {
        JsonNode preset = document == null ? null : document.get("ability_preset_" + presetNo);
        JsonNode abilities = preset == null ? null : preset.get("ability_info");
        if (abilities == null || !abilities.isArray()) {
            return 0;
        }
        boolean archer = containsAny(characterClass, "보우마스터", "신궁", "패스파인더", "윈드브레이커", "와일드헌터", "메르세데스", "카인");
        int score = 0;
        for (JsonNode ability : abilities) {
            String value = ability.path("ability_value").asText("");
            int amount = firstNumber(value).orElse(0);
            if (value.contains("보스 몬스터")) {
                score += 100 + amount;
            } else if (containsAny(value, "공격력", "마력")) {
                score += 50 + amount;
            } else if (value.contains("크리티컬 확률")) {
                score += archer ? 30 + amount : -20;
            } else if (containsAny(value, "버프 지속", "재사용")) {
                score += 10 + amount;
            } else if (containsAny(value, "아이템 드롭", "메소", "획득 경험치")) {
                score -= 80;
            }
        }
        return score;
    }

    private int unionBattleScore(JsonNode document, int presetNo) {
        JsonNode preset = document == null ? null : document.get("union_raider_preset_" + presetNo);
        if (preset == null || preset.isNull()) {
            return 0;
        }
        int score = 0;
        score += unionTextScore(preset.get("union_raider_stat"), false);
        score += unionTextScore(preset.get("union_occupied_stat"), true);
        JsonNode blocks = preset.get("union_block");
        if (blocks != null && blocks.isArray()) {
            score += blocks.size();
        }
        return score;
    }

    private int unionTextScore(JsonNode values, boolean occupied) {
        if (values == null || !values.isArray()) {
            return 0;
        }
        int score = 0;
        for (JsonNode value : values) {
            String text = value.asText("");
            int amount = firstNumber(text).orElse(0);
            if (text.contains("보스 몬스터")) {
                score += 100 + amount;
            } else if (text.contains("크리티컬 데미지")) {
                score += 80 + amount;
            } else if (containsAny(text, "공격력", "마력")) {
                score += 40 + amount;
            } else if (containsAny(text, "STR", "DEX", "INT", "LUK")) {
                score += occupied ? 20 + amount : 5 + amount;
            } else if (containsAny(text, "일반 몬스터", "경험치", "메소", "버프 지속", "최대 MP", "이동속도")) {
                score -= 40 + amount;
            }
        }
        return score;
    }

    private String flattenText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        StringBuilder builder = new StringBuilder();
        if (node.isArray()) {
            for (JsonNode child : node) {
                builder.append(' ').append(flattenText(child));
            }
        } else if (node.isObject()) {
            for (String name : node.propertyNames()) {
                builder.append(' ').append(flattenText(node.get(name)));
            }
        }
        return builder.toString();
    }

    private boolean containsAny(String source, String... needles) {
        for (String needle : needles) {
            if (source.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Long> longMap(Map<CombatStatKey, Long> source) {
        return source.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue, Long::sum, LinkedHashMap::new));
    }

    private Map<String, Double> doubleMap(Map<CombatStatKey, Double> source) {
        return source.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue, Double::sum, LinkedHashMap::new));
    }

    private PresetCombatPower selectCandidate(List<PresetCombatPower> candidates, PresetSelectionMode mode) {
        if (candidates.isEmpty()) {
            CombatPowerCalculation empty = formula.calculate("", new CombatStatBag());
            return new PresetCombatPower(0, Map.of(), 0L, empty);
        }
        if (mode == PresetSelectionMode.MAX) {
            return candidates.stream().max(Comparator.comparing(PresetCombatPower::combatPower)).orElse(candidates.getFirst());
        }
        return candidates.getFirst();
    }

    private String characterClass(NexonCharacterSnapshot snapshot) {
        JsonNode basic = snapshot.document(NexonEndpoint.BASIC);
        if (basic != null && basic.hasNonNull("character_class")) {
            return basic.get("character_class").asText();
        }
        JsonNode stat = snapshot.document(NexonEndpoint.STAT);
        return stat != null && stat.hasNonNull("character_class") ? stat.get("character_class").asText() : "";
    }

    private Optional<Integer> characterLevel(NexonCharacterSnapshot snapshot) {
        JsonNode basic = snapshot.document(NexonEndpoint.BASIC);
        return intField(basic, "character_level");
    }

    private Map<String, Integer> currentPresetNos(NexonCharacterSnapshot snapshot) {
        Map<String, Integer> presetNos = new LinkedHashMap<>();
        putPresetNo(presetNos, "ITEM_EQUIPMENT", snapshot.document(NexonEndpoint.ITEM_EQUIPMENT), "preset_no");
        putPresetNo(presetNos, "CASH_ITEM_EQUIPMENT", snapshot.document(NexonEndpoint.CASH_ITEM_EQUIPMENT), "preset_no");
        putPresetNo(presetNos, "HYPER_STAT", snapshot.document(NexonEndpoint.HYPER_STAT), "use_preset_no");
        putPresetNo(presetNos, "ABILITY", snapshot.document(NexonEndpoint.ABILITY), "preset_no");
        putPresetNo(presetNos, "UNION_RAIDER", snapshot.document(NexonEndpoint.UNION_RAIDER), "use_preset_no");
        return presetNos;
    }

    private void putPresetNo(Map<String, Integer> presetNos, String source, JsonNode document, String fieldName) {
        intField(document, fieldName).ifPresent(presetNo -> presetNos.put(source, presetNo));
    }

    private Optional<Integer> intField(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || !node.hasNonNull(fieldName)) {
            return Optional.empty();
        }
        JsonNode value = node.get(fieldName);
        if (value.isInt()) {
            return Optional.of(value.asInt());
        }
        try {
            return Optional.of(Integer.parseInt(value.asText()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Integer> firstNumber(String value) {
        if (value == null) {
            return Optional.empty();
        }
        StringBuilder digits = new StringBuilder();
        boolean found = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
                found = true;
            } else if (found) {
                break;
            }
        }
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(digits.toString()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Long> finalStatCombatPower(JsonNode stat) {
        if (stat == null || stat.isNull() || !stat.has("final_stat")) {
            return Optional.empty();
        }
        for (JsonNode node : stat.get("final_stat")) {
            if (node.hasNonNull("stat_name") && "전투력".equals(node.get("stat_name").asText())) {
                String raw = node.path("stat_value").asText().replace(",", "");
                try {
                    return Optional.of(Long.parseLong(raw));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private List<String> warnings(NexonCharacterSnapshot snapshot, Map<String, Integer> currentPresetNos, Long nexonCombatPower, boolean verified) {
        List<String> warnings = new ArrayList<>();
        if (currentPresetNos.isEmpty()) {
            warnings.add("현재 장착 프리셋 번호를 API 응답에서 찾지 못했습니다.");
        }
        if (nexonCombatPower == null) {
            warnings.add("character/stat final_stat의 전투력 값을 찾지 못해 검증을 생략했습니다.");
        } else if (!verified) {
            warnings.add("선택된 프리셋이 현재 장착 프리셋이 아니어서 final_stat 검증은 참고값으로만 제공합니다.");
        }
        warnings.add("MVP 계산기는 final_stat을 입력으로 사용하지 않으며, 원천 장비/유니온/아티팩트/헥사 등에서 식별 가능한 옵션만 합산합니다.");
        warnings.add("OpenAPI에서 AP/일부 직업 기본 스탯 원천이 분리 제공되지 않는 경우 검증 오차가 남을 수 있습니다.");
        return warnings;
    }

    private void printSelectedStatDebug(
            String characterName,
            NexonCharacterSnapshot snapshot,
            PresetSelectionMode mode,
            PresetCombatPower selected,
            CombatStatBag selectedStats,
            Long nexonCombatPower,
            Long delta,
            Double deltaRate
    ) {
        System.out.println();
        System.out.println("========== Combat Power Stat Debug ==========");
        System.out.println("characterName = " + characterName);
        System.out.println("date = " + snapshot.date());
        System.out.println("mode = " + mode);
        System.out.println("selectedPresetNos = " + selected.sourcePresetNos());
        System.out.println("combatPower = " + selected.combatPower());
        System.out.println("nexonCombatPower = " + nexonCombatPower);
        System.out.println("delta = " + delta);
        System.out.println("deltaRate = " + (deltaRate == null ? null : String.format("%.8f", deltaRate)));
        System.out.println("weaponNormalization = " + selectedStats.weaponNormalization());

        CombatPowerCalculation calculation = selected.formula();

        System.out.println();
        System.out.println("[FORMULA INPUT]");
        System.out.println("mainStat = " + calculation.mainStat());
        System.out.println("subStat = " + calculation.subStat());
        System.out.println("attackPower = " + calculation.attackPower());
        System.out.println("damageFactor = " + calculation.damageFactor());
        System.out.println("criticalDamageFactor = " + calculation.criticalDamageFactor());
        System.out.println("finalDamageFactor = " + calculation.finalDamageFactor());

        System.out.println();
        System.out.println("[MERGED STAT TOTALS]");
        printCombatStatBagDebug(selectedStats);

        System.out.println();
        System.out.println("[EFFECTIVE FORMULA STATS]");
        printEffectiveFormulaStatsDebug(selectedStats, calculation, characterClass(snapshot), characterLevel(snapshot).orElse(null));

        System.out.println();
        System.out.println("[SOURCE TOTALS]");
        printSourceTotalsDebug(selectedStats.contributions());

        System.out.println();
        System.out.println("[ITEM EXCEPTIONAL OPTIONS]");
        printExceptionalOptionsDebug(snapshot, selected.sourcePresetNos().get("ITEM_EQUIPMENT"));

        System.out.println("=================================================");
        System.out.println();
    }

    private void printMaxSelectedStatDebug(
            NexonCharacterSnapshot snapshot,
            PresetSelectionMode mode,
            PresetStatBundle bundle,
            PresetCombatPower selected
    ) {
        if (mode != PresetSelectionMode.MAX) {
            return;
        }
        printSelectedStatDebug(
                "(trend)",
                snapshot,
                mode,
                selected,
                bundle.merged(selected.sourcePresetNos()),
                null,
                null,
                null
        );
    }

    private void printCombatStatBagDebug(CombatStatBag bag) {
        for (CombatStatKey key : CombatStatKey.values()) {
            long flat = bag.flat(key);
            long finalFlat = bag.finalFlat(key);
            double percent = bag.percent(key);

            if (flat == 0 && finalFlat == 0 && percent == 0.0d) {
                continue;
            }

            System.out.printf(
                    "%-20s flat=%d, finalFlat=%d, percent=%.2f%%%n",
                    key.name(),
                    flat,
                    finalFlat,
                    percent
            );
        }
    }

    private void printEffectiveFormulaStatsDebug(
            CombatStatBag bag,
            CombatPowerCalculation calculation,
            String characterClass,
            Integer characterLevel
    ) {
        CharacterStatProfile profile = CharacterStatProfile.from(characterClass);
        printEffectiveStatBucket("main", profile.mainStats(), bag, characterLevel, true, calculation.mainStat());
        printEffectiveStatBucket("sub", profile.subStats(), bag, characterLevel, false, calculation.subStat());

        CombatStatKey attackKey = profile.usesMagicAttack() ? CombatStatKey.MAGIC_ATTACK : CombatStatKey.ATTACK_POWER;
        long attackBase = bag.flat(attackKey);
        double attackPercent = bag.percent(attackKey);
        System.out.printf(
                "attack(%s): base=%d, percent=%.2f%%, result=%d%n",
                attackKey.name(),
                attackBase,
                attackPercent,
                calculation.attackPower()
        );
        System.out.printf(
                "damage: DAMAGE%%=%.2f%%, BOSS_DAMAGE%%=%.2f%%, resultFactor=%.5f%n",
                bag.percent(CombatStatKey.DAMAGE),
                bag.percent(CombatStatKey.BOSS_DAMAGE),
                calculation.damageFactor()
        );
        System.out.printf(
                "criticalDamage: CRITICAL_DAMAGE%%=%.2f%%, resultFactor=%.5f%n",
                bag.percent(CombatStatKey.CRITICAL_DAMAGE),
                calculation.criticalDamageFactor()
        );
        System.out.printf(
                "finalDamage: FINAL_DAMAGE%%=%.2f%%, resultFactor=%.5f%n",
                bag.percent(CombatStatKey.FINAL_DAMAGE),
                calculation.finalDamageFactor()
        );
    }

    private void printEffectiveStatBucket(
            String label,
            java.util.Set<CombatStatKey> keys,
            CombatStatBag bag,
            Integer characterLevel,
            boolean main,
            long result
    ) {
        long statFlat = keys.stream().mapToLong(bag::flat).sum();
        long allFlat = bag.flat(CombatStatKey.ALL_STAT);
        long ap = main ? estimatedApMainForDebug(keys, characterLevel) : estimatedApSubForDebug(keys, characterLevel);
        long base = statFlat + allFlat + ap;
        double statPercent = keys.stream().mapToDouble(bag::percent).sum();
        double allPercent = bag.percent(CombatStatKey.ALL_STAT);
        long statFinalFlat = keys.stream().mapToLong(bag::finalFlat).sum();
        long allFinalFlat = bag.finalFlat(CombatStatKey.ALL_STAT);
        System.out.printf(
                "%s%s: flat=%d + allStat=%d + AP=%d => base=%d, percent=%.2f%% + allStat%%=%.2f%% => %.2f%%, finalFlat=%d + allFinalFlat=%d => %d, result=%d%n",
                label,
                keys,
                statFlat,
                allFlat,
                ap,
                base,
                statPercent,
                allPercent,
                statPercent + allPercent,
                statFinalFlat,
                allFinalFlat,
                statFinalFlat + allFinalFlat,
                result
        );
    }

    private long estimatedApMainForDebug(java.util.Set<CombatStatKey> keys, Integer characterLevel) {
        if (characterLevel == null || characterLevel < 1 || keys.contains(CombatStatKey.MAX_HP) || keys.size() > 1) {
            return 0L;
        }
        return 5L * characterLevel + 18L;
    }

    private long estimatedApSubForDebug(java.util.Set<CombatStatKey> keys, Integer characterLevel) {
        if (characterLevel == null || characterLevel < 1 || keys.contains(CombatStatKey.MAX_HP)) {
            return 0L;
        }
        return 4L;
    }

    private void printSourceTotalsDebug(List<StatContribution> contributions) {
        Map<String, SourceTotals> totals = sourceTotals(contributions);
        totals.forEach((source, sourceTotal) -> {
            System.out.println();
            System.out.println("- " + source);
            printBucketDebug("flat(percent applied)", sourceTotal.percentAppliedFlat());
            printBucketDebug("finalFlat(percent not applied)", sourceTotal.percentNotAppliedFlat());
            printBucketDebug("percent", sourceTotal.percent());
        });
    }

    private void printExceptionalOptionsDebug(NexonCharacterSnapshot snapshot, Integer itemPresetNo) {
        JsonNode itemEquipment = snapshot.documents().get(NexonEndpoint.ITEM_EQUIPMENT);
        if (itemEquipment == null || itemEquipment.isNull()) {
            System.out.println("{}");
            return;
        }
        String field = itemPresetNo == null ? "item_equipment" : "item_equipment_preset_" + itemPresetNo;
        JsonNode items = itemEquipment.get(field);
        if (items == null || !items.isArray()) {
            System.out.println("{}");
            return;
        }
        CombatStatBag exceptionalTotals = new CombatStatBag();
        int nonZeroItems = 0;
        for (JsonNode item : items) {
            JsonNode exceptional = item.get("item_exceptional_option");
            if (exceptional == null || exceptional.isNull()) {
                continue;
            }
            long before = exceptionalTotalValue(exceptionalTotals);
            addExceptionalIfPresent(exceptionalTotals, exceptional, "str", CombatStatKey.STR);
            addExceptionalIfPresent(exceptionalTotals, exceptional, "dex", CombatStatKey.DEX);
            addExceptionalIfPresent(exceptionalTotals, exceptional, "int", CombatStatKey.INT);
            addExceptionalIfPresent(exceptionalTotals, exceptional, "luk", CombatStatKey.LUK);
            addExceptionalIfPresent(exceptionalTotals, exceptional, "max_hp", CombatStatKey.MAX_HP);
            addExceptionalIfPresent(exceptionalTotals, exceptional, "attack_power", CombatStatKey.ATTACK_POWER);
            addExceptionalIfPresent(exceptionalTotals, exceptional, "magic_power", CombatStatKey.MAGIC_ATTACK);
            if (exceptionalTotalValue(exceptionalTotals) != before) {
                nonZeroItems++;
                System.out.println("- " + item.path("item_name").asText("(unknown)") + " " + exceptional);
            }
        }
        if (nonZeroItems == 0) {
            System.out.println("{}");
            return;
        }
        System.out.println("exceptionalItemCount = " + nonZeroItems);
        printCombatStatBagDebug(exceptionalTotals);
        System.out.println("note = exceptional options are added separately because raw item_total_option excludes them.");
    }

    private List<CombatPowerTotalChange> totalChanges(CombatStatBag previous, CombatStatBag current) {
        List<CombatPowerTotalChange> changes = new ArrayList<>();
        collectTotalChanges(changes, "PERCENT", previous.percentValues(), current.percentValues());
        collectTotalChanges(changes, "PERCENT_APPLIED_FLAT", previous.flatValues(), current.flatValues());
        collectTotalChanges(changes, "PERCENT_NOT_APPLIED_FLAT", previous.finalFlatValues(), current.finalFlatValues());
        return changes;
    }

    private void collectTotalChanges(
            List<CombatPowerTotalChange> changes,
            String bucket,
            Map<CombatStatKey, ? extends Number> previous,
            Map<CombatStatKey, ? extends Number> current
    ) {
        for (CombatStatKey key : CombatStatKey.values()) {
            double before = previous.containsKey(key) ? previous.get(key).doubleValue() : 0.0d;
            double after = current.containsKey(key) ? current.get(key).doubleValue() : 0.0d;
            if (Double.compare(before, after) == 0) {
                continue;
            }
            changes.add(new CombatPowerTotalChange(
                    bucket,
                    key.name(),
                    totalChangeLabel(bucket, key),
                    formatStatValue(bucket, before),
                    formatStatValue(bucket, after),
                    formatDeltaValue(bucket, after - before)
            ));
        }
    }

    private List<CombatPowerDetailChange> detailChanges(TrendPointContext previous, TrendPointContext current) {
        List<CombatPowerDetailChange> changes = new ArrayList<>();
        changes.addAll(equipmentChanges(previous, current));
        changes.addAll(sourceStatChanges(previous.selectedStats(), current.selectedStats()));
        return changes;
    }

    private List<CombatPowerDetailChange> equipmentChanges(TrendPointContext previous, TrendPointContext current) {
        Map<String, JsonNode> previousItems = selectedEquipmentItems(previous.snapshot(), previous.point().selectedSourcePresetNos());
        Map<String, JsonNode> currentItems = selectedEquipmentItems(current.snapshot(), current.point().selectedSourcePresetNos());
        Set<String> slots = new LinkedHashSet<>();
        slots.addAll(previousItems.keySet());
        slots.addAll(currentItems.keySet());

        List<CombatPowerDetailChange> changes = new ArrayList<>();
        for (String slot : slots) {
            if (shouldSkipEquipmentSlot(slot, current.mode())) {
                continue;
            }
            JsonNode before = previousItems.get(slot);
            JsonNode after = currentItems.get(slot);
            if (equipmentSignature(before).equals(equipmentSignature(after))) {
                continue;
            }
            changes.add(new CombatPowerDetailChange(
                    "EQUIPMENT",
                    "ITEM_EQUIPMENT",
                    "장비",
                    slot,
                    equipmentChangeLines(before, after),
                    equipmentSnapshot(before),
                    equipmentSnapshot(after)
            ));
        }
        return changes;
    }

    private boolean shouldSkipEquipmentSlot(String slot, PresetSelectionMode mode) {
        if (slot == null || slot.isBlank()) {
            return false;
        }
        return mode == PresetSelectionMode.BATTLE && slot.contains("예비 특수 반지");
    }

    private List<CombatPowerDetailChange> sourceStatChanges(CombatStatBag previous, CombatStatBag current) {
        Map<String, SourceTotals> previousTotals = sourceTotals(previous.contributions());
        Map<String, SourceTotals> currentTotals = sourceTotals(current.contributions());
        Set<String> sources = new LinkedHashSet<>();
        sources.addAll(previousTotals.keySet());
        sources.addAll(currentTotals.keySet());

        List<CombatPowerDetailChange> changes = new ArrayList<>();
        for (String source : sources) {
            if ("ITEM_EQUIPMENT".equals(source) || "CASH_ITEM_EQUIPMENT".equals(source)) {
                continue;
            }
            List<String> lines = new ArrayList<>();
            SourceTotals before = previousTotals.get(source);
            SourceTotals after = currentTotals.get(source);
            collectSourceLines(lines, "PERCENT", before == null ? Map.of() : before.percent(), after == null ? Map.of() : after.percent());
            collectSourceLines(lines, "PERCENT_APPLIED_FLAT", before == null ? Map.of() : before.percentAppliedFlat(), after == null ? Map.of() : after.percentAppliedFlat());
            collectSourceLines(lines, "PERCENT_NOT_APPLIED_FLAT", before == null ? Map.of() : before.percentNotAppliedFlat(), after == null ? Map.of() : after.percentNotAppliedFlat());
            if (!lines.isEmpty()) {
                changes.add(new CombatPowerDetailChange("STAT", source, sourceName(source), sourceName(source), lines, null, null));
            }
        }
        return changes;
    }

    private void collectSourceLines(List<String> lines, String bucket, Map<String, Double> previous, Map<String, Double> current) {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(previous.keySet());
        keys.addAll(current.keySet());
        for (String key : keys) {
            double before = previous.getOrDefault(key, 0.0d);
            double after = current.getOrDefault(key, 0.0d);
            if (Double.compare(before, after) == 0) {
                continue;
            }
            lines.add(totalChangeLabel(bucket, CombatStatKey.valueOf(key))
                    + ": "
                    + formatStatValue(bucket, before)
                    + " -> "
                    + formatStatValue(bucket, after)
                    + " ("
                    + formatDeltaValue(bucket, after - before)
                    + ")");
        }
    }

    private Map<String, JsonNode> selectedEquipmentItems(NexonCharacterSnapshot snapshot, Map<String, Integer> selectedSourcePresetNos) {
        JsonNode document = snapshot.document(NexonEndpoint.ITEM_EQUIPMENT);
        if (document == null || document.isNull()) {
            return Map.of();
        }
        Integer presetNo = selectedSourcePresetNos.get("ITEM_EQUIPMENT");
        JsonNode items = presetNo == null ? document.get("item_equipment") : document.get("item_equipment_preset_" + presetNo);
        if ((items == null || !items.isArray()) && document.has("item_equipment")) {
            items = document.get("item_equipment");
        }
        if (items == null || !items.isArray()) {
            return Map.of();
        }

        Map<String, JsonNode> bySlot = new LinkedHashMap<>();
        for (JsonNode item : items) {
            String slot = item.path("item_equipment_slot").asText("");
            if (slot.isBlank()) {
                slot = item.path("item_equipment_part").asText("기타");
            }
            bySlot.put(slot, item);
        }
        return bySlot;
    }

    private String equipmentSignature(JsonNode item) {
        return item == null || item.isNull() ? "" : item.toString();
    }

    private String equipmentChangeHeadline(JsonNode before, JsonNode after) {
        if (before == null || before.isNull()) {
            return "새 장비 장착";
        }
        if (after == null || after.isNull()) {
            return "장비 해제";
        }
        String beforeName = before.path("item_name").asText("");
        String afterName = after.path("item_name").asText("");
        if (!beforeName.equals(afterName)) {
            return "다른 장비 장착";
        }
        return "같은 이름의 장비지만 옵션 변화";
    }

    private String equipmentNameChange(JsonNode before, JsonNode after) {
        return itemName(before) + " -> " + itemName(after);
    }

    private List<String> equipmentChangeLines(JsonNode before, JsonNode after) {
        List<String> lines = new ArrayList<>();
        lines.add(equipmentChangeHeadline(before, after));
        lines.add(equipmentNameChange(before, after));
        lines.addAll(equipmentStatDeltaLines(before, after));
        lines.addAll(equipmentOptionDeltaLines(before, after));
        return lines;
    }

    private List<String> equipmentStatDeltaLines(JsonNode before, JsonNode after) {
        Map<String, Double> beforeValues = equipmentComparableStats(before);
        Map<String, Double> afterValues = equipmentComparableStats(after);
        List<String> lines = new ArrayList<>();
        for (String key : equipmentStatOrder()) {
            double beforeValue = beforeValues.getOrDefault(key, 0.0d);
            double afterValue = afterValues.getOrDefault(key, 0.0d);
            if (Double.compare(beforeValue, afterValue) == 0) {
                continue;
            }
            lines.add(equipmentStatLabel(key)
                    + ": "
                    + formatEquipmentStatValue(key, beforeValue)
                    + " -> "
                    + formatEquipmentStatValue(key, afterValue)
                    + " ("
                    + formatEquipmentStatDelta(key, afterValue - beforeValue)
                    + ")");
        }
        return lines;
    }

    private List<String> equipmentOptionDeltaLines(JsonNode before, JsonNode after) {
        List<String> lines = new ArrayList<>();
        addEquipmentOptionDelta(lines, "잠재 1", before, after, "potential_option_1");
        addEquipmentOptionDelta(lines, "잠재 2", before, after, "potential_option_2");
        addEquipmentOptionDelta(lines, "잠재 3", before, after, "potential_option_3");
        addEquipmentOptionDelta(lines, "에디 1", before, after, "additional_potential_option_1");
        addEquipmentOptionDelta(lines, "에디 2", before, after, "additional_potential_option_2");
        addEquipmentOptionDelta(lines, "에디 3", before, after, "additional_potential_option_3");
        return lines;
    }

    private void addEquipmentOptionDelta(List<String> lines, String label, JsonNode before, JsonNode after, String fieldName) {
        String beforeValue = normalizedEquipmentOption(before, fieldName);
        String afterValue = normalizedEquipmentOption(after, fieldName);
        if (beforeValue.equals(afterValue)) {
            return;
        }
        lines.add(label + ": " + beforeValue + " -> " + afterValue);
    }

    private String normalizedEquipmentOption(JsonNode item, String fieldName) {
        if (item == null || item.isNull()) {
            return "없음";
        }
        String value = item.path(fieldName).asText("");
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "없음";
        }
        return value;
    }

    private Map<String, Double> equipmentComparableStats(JsonNode item) {
        if (item == null || item.isNull()) {
            return Map.of();
        }
        Map<String, Double> values = new LinkedHashMap<>();
        JsonNode total = item.get("item_total_option");
        if (total != null && !total.isNull()) {
            putEquipmentStat(values, total, "str");
            putEquipmentStat(values, total, "dex");
            putEquipmentStat(values, total, "int");
            putEquipmentStat(values, total, "luk");
            putEquipmentStat(values, total, "max_hp");
            putEquipmentStat(values, total, "max_mp");
            putEquipmentStat(values, total, "attack_power");
            putEquipmentStat(values, total, "magic_power");
            putEquipmentStat(values, total, "boss_damage");
            putEquipmentStat(values, total, "ignore_monster_armor");
            putEquipmentStat(values, total, "all_stat");
            putEquipmentStat(values, total, "damage");
            putEquipmentStat(values, total, "max_hp_rate");
            putEquipmentStat(values, total, "max_mp_rate");
        }
        values.put("starforce", parseDouble(item.path("starforce").asText("")));
        values.put("scroll_upgrade", parseDouble(item.path("scroll_upgrade").asText("")));
        return values;
    }

    private void putEquipmentStat(Map<String, Double> values, JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return;
        }
        double value = parseDouble(node.get(field).asText(""));
        if (Double.compare(value, 0.0d) != 0) {
            values.put(field, value);
        }
    }

    private List<String> equipmentStatOrder() {
        return List.of(
                "str", "dex", "int", "luk",
                "max_hp", "max_mp",
                "attack_power", "magic_power",
                "boss_damage", "ignore_monster_armor",
                "all_stat", "damage",
                "max_hp_rate", "max_mp_rate",
                "starforce", "scroll_upgrade"
        );
    }

    private String equipmentStatLabel(String key) {
        return switch (key) {
            case "str" -> "힘";
            case "dex" -> "민첩";
            case "int" -> "인트";
            case "luk" -> "럭";
            case "max_hp" -> "최대 HP";
            case "max_mp" -> "최대 MP";
            case "attack_power" -> "공격력";
            case "magic_power" -> "마력";
            case "boss_damage" -> "보스 데미지";
            case "ignore_monster_armor" -> "방무";
            case "all_stat" -> "올스탯";
            case "damage" -> "데미지";
            case "max_hp_rate" -> "최대 HP%";
            case "max_mp_rate" -> "최대 MP%";
            case "starforce" -> "스타포스";
            case "scroll_upgrade" -> "주문서";
            default -> key;
        };
    }

    private String formatEquipmentStatValue(String key, double value) {
        String suffix = equipmentPercentStat(key) ? "%" : equipmentCountSuffix(key);
        return formatPlainNumber(value) + suffix;
    }

    private String formatEquipmentStatDelta(String key, double value) {
        String prefix = value > 0 ? "+" : "";
        String suffix = equipmentPercentStat(key) ? "%" : equipmentCountSuffix(key);
        return prefix + formatPlainNumber(value) + suffix;
    }

    private boolean equipmentPercentStat(String key) {
        return Set.of("boss_damage", "ignore_monster_armor", "all_stat", "damage", "max_hp_rate", "max_mp_rate").contains(key);
    }

    private String equipmentCountSuffix(String key) {
        if ("starforce".equals(key)) {
            return "성";
        }
        if ("scroll_upgrade".equals(key)) {
            return "회";
        }
        return "";
    }

    private String itemName(JsonNode item) {
        if (item == null || item.isNull()) {
            return "없음";
        }
        return item.path("item_name").asText("알 수 없음");
    }

    private CombatPowerEquipmentSnapshot equipmentSnapshot(JsonNode item) {
        if (item == null || item.isNull()) {
            return null;
        }
        return new CombatPowerEquipmentSnapshot(
                item.path("item_name").asText("알 수 없음"),
                item.path("item_icon").asText(null),
                equipmentTooltipLines(item)
        );
    }

    private List<String> equipmentTooltipLines(JsonNode item) {
        List<String> lines = new ArrayList<>();
        addStructuredTooltip(lines, "총 옵션", item.get("item_total_option"));
        addStructuredTooltip(lines, "기본 옵션", item.get("item_base_option"));
        addStructuredTooltip(lines, "추가 옵션", item.get("item_add_option"));
        addStructuredTooltip(lines, "기타 옵션", item.get("item_etc_option"));
        addStructuredTooltip(lines, "스타포스 옵션", item.get("item_starforce_option"));
        addStructuredTooltip(lines, "익셉셔널", item.get("item_exceptional_option"));

        String starforce = item.path("starforce").asText("");
        if (!starforce.isBlank() && !"0".equals(starforce)) {
            lines.add("스타포스: " + starforce + "성");
        }
        String scrollUpgrade = item.path("scroll_upgrade").asText("");
        if (!scrollUpgrade.isBlank() && !"0".equals(scrollUpgrade)) {
            lines.add("주문서: " + scrollUpgrade + "회");
        }
        appendOptionLines(lines, "잠재", item.path("potential_option_grade").asText(null), item,
                "potential_option_1", "potential_option_2", "potential_option_3");
        appendOptionLines(lines, "에디", item.path("additional_potential_option_grade").asText(null), item,
                "additional_potential_option_1", "additional_potential_option_2", "additional_potential_option_3");
        appendSingleLine(lines, "소울", item.path("soul_option").asText(null));
        appendSingleLine(lines, "설명", item.path("item_description").asText(null));
        return lines;
    }

    private void addStructuredTooltip(List<String> lines, String label, JsonNode options) {
        if (options == null || options.isNull()) {
            return;
        }
        List<String> values = new ArrayList<>();
        collectStructuredOption(options, values, "str", "STR", false);
        collectStructuredOption(options, values, "dex", "DEX", false);
        collectStructuredOption(options, values, "int", "INT", false);
        collectStructuredOption(options, values, "luk", "LUK", false);
        collectStructuredOption(options, values, "max_hp", "최대 HP", false);
        collectStructuredOption(options, values, "max_mp", "최대 MP", false);
        collectStructuredOption(options, values, "attack_power", "공격력", false);
        collectStructuredOption(options, values, "magic_power", "마력", false);
        collectStructuredOption(options, values, "boss_damage", "보스 데미지", true);
        collectStructuredOption(options, values, "ignore_monster_armor", "방무", true);
        collectStructuredOption(options, values, "all_stat", "올스탯", true);
        collectStructuredOption(options, values, "damage", "데미지", true);
        collectStructuredOption(options, values, "max_hp_rate", "최대 HP", true);
        collectStructuredOption(options, values, "max_mp_rate", "최대 MP", true);
        if (!values.isEmpty()) {
            lines.add(label + ": " + String.join(", ", values));
        }
    }

    private void collectStructuredOption(JsonNode options, List<String> values, String field, String label, boolean percent) {
        if (!options.hasNonNull(field)) {
            return;
        }
        double value = parseDouble(options.get(field).asText(""));
        if (Double.compare(value, 0.0d) == 0) {
            return;
        }
        values.add(label + " +" + formatPlainNumber(value) + (percent ? "%" : ""));
    }

    private void appendOptionLines(List<String> lines, String label, String grade, JsonNode item, String... fields) {
        List<String> values = new ArrayList<>();
        for (String field : fields) {
            String value = item.path(field).asText(null);
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                values.add(value);
            }
        }
        if (!values.isEmpty()) {
            lines.add(label + (grade == null || grade.isBlank() ? "" : "(" + grade + ")") + ": " + String.join(" / ", values));
        }
    }

    private void appendSingleLine(List<String> lines, String label, String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return;
        }
        lines.add(label + ": " + value.replace("\r", " ").replace("\n", " "));
    }

    private String totalChangeLabel(String bucket, CombatStatKey key) {
        return switch (bucket) {
            case "PERCENT_APPLIED_FLAT" -> statName(key) + "(적용)";
            case "PERCENT_NOT_APPLIED_FLAT" -> statName(key) + "(미적용)";
            default -> statName(key);
        };
    }

    private String statName(CombatStatKey key) {
        return switch (key) {
            case STR -> "힘";
            case DEX -> "민첩";
            case INT -> "인트";
            case LUK -> "럭";
            case MAX_HP -> "최대 HP";
            case ALL_STAT -> "올스탯";
            case ATTACK_POWER -> "공격력";
            case MAGIC_ATTACK -> "마력";
            case DAMAGE -> "데미지";
            case BOSS_DAMAGE -> "보스 데미지";
            case CRITICAL_DAMAGE -> "크리티컬 데미지";
            case FINAL_DAMAGE -> "최종 데미지";
        };
    }

    private String sourceName(String source) {
        return switch (source) {
            case "SET_EFFECT" -> "세트 효과";
            case "SYMBOL_EQUIPMENT" -> "심볼";
            case "PET_EQUIPMENT" -> "펫 장비";
            case "SKILL_0" -> "0차 스킬";
            case "HEXA_MATRIX_STAT" -> "헥사 스탯";
            case "UNION_CHAMPION" -> "유니온 챔피언";
            case "UNION_ARTIFACT" -> "유니온 아티팩트";
            case "UNION_RAIDER" -> "유니온 공격대";
            case "HYPER_STAT" -> "하이퍼 스탯";
            case "ABILITY" -> "어빌리티";
            case "WEAPON_NORMALIZATION" -> "무기 보정";
            default -> source;
        };
    }

    private String formatStatValue(String bucket, double value) {
        return formatPlainNumber(value) + ("PERCENT".equals(bucket) ? "%" : "");
    }

    private String formatDeltaValue(String bucket, double delta) {
        String prefix = delta > 0 ? "+" : "";
        return prefix + formatPlainNumber(delta) + ("PERCENT".equals(bucket) ? "%" : "");
    }

    private String formatPlainNumber(double value) {
        long rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.0000001d) {
            return Long.toString(rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0.0d;
        }
    }

    private void addExceptionalIfPresent(CombatStatBag totals, JsonNode exceptional, String field, CombatStatKey key) {
        if (!exceptional.hasNonNull(field)) {
            return;
        }
        long value = parseLong(exceptional.get(field).asText()).orElse(0L);
        if (value != 0L) {
            totals.addFlat(key, value);
        }
    }

    private long exceptionalTotalValue(CombatStatBag totals) {
        long flat = 0L;
        for (CombatStatKey key : CombatStatKey.values()) {
            flat += Math.abs(totals.flat(key));
        }
        return flat;
    }

    private Optional<Long> parseLong(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.replace(",", "")));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private void printBucketDebug(String bucketName, Map<String, Double> values) {
        if (values == null || values.isEmpty()) {
            System.out.println("  " + bucketName + ": {}");
            return;
        }
        System.out.println("  " + bucketName + ":");
        values.forEach((stat, value) -> System.out.printf("    %-20s %.2f%n", stat, value));
    }

    private record Evaluation(
            String characterClass,
            Integer characterLevel,
            Map<String, Integer> currentPresetNos,
            List<PresetCombatPower> candidates,
            PresetCombatPower selected,
            CombatStatBag selectedStats,
            Long nexonCombatPower,
            boolean currentPresetSelected,
            Long delta,
            Double deltaRate
    ) {
    }

    private record TrendPointContext(
            NexonCharacterSnapshot snapshot,
            CombatPowerPoint point,
            CombatStatBag selectedStats,
            PresetSelectionMode mode
    ) {
    }
}

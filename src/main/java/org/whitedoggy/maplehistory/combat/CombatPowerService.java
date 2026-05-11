package org.whitedoggy.maplehistory.combat;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                                .map(snapshot -> point(snapshot, mode)), properties.maxConcurrency())
                        .sort(Comparator.comparing(CombatPowerPoint::date))
                        .collectList()
                        .map(points -> response(characterName, identity, from, to, mode, points)));
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

    private CombatPowerPoint point(NexonCharacterSnapshot snapshot, PresetSelectionMode mode) {
        String characterClass = characterClass(snapshot);
        Integer characterLevel = characterLevel(snapshot).orElse(null);
        PresetStatBundle bundle = statExtractor.extract(snapshot.documents(), characterClass);
        Map<String, Integer> currentPresetNos = currentPresetNos(snapshot);
        List<PresetCombatPower> candidates = candidates(bundle, characterClass, characterLevel, currentPresetNos, mode);
        PresetCombatPower selected = selectCandidate(candidates, mode);
        Long nexonCombatPower = finalStatCombatPower(snapshot.document(NexonEndpoint.STAT)).orElse(null);
        boolean currentPresetSelected = selected.sourcePresetNos().equals(currentPresetNos) || mode == PresetSelectionMode.CURRENT;
        Long delta = nexonCombatPower == null || !currentPresetSelected ? null : selected.combatPower() - nexonCombatPower;
        Double deltaRate = delta == null || nexonCombatPower == 0
                ? null
                : delta / (double) nexonCombatPower;

        List<String> warnings = warnings(snapshot, currentPresetNos, nexonCombatPower, currentPresetSelected);
        return new CombatPowerPoint(
                snapshot.date(),
                selected.presetNo(),
                selected.sourcePresetNos(),
                selected.combatPower(),
                nexonCombatPower,
                delta,
                deltaRate,
                nexonCombatPower != null && currentPresetSelected,
                selected.formula(),
                candidates,
                warnings
        );
    }

    private CombatPowerDebugResponse debugResponse(String characterName, String ocid, NexonCharacterSnapshot snapshot, PresetSelectionMode mode) {
        String characterClass = characterClass(snapshot);
        Integer characterLevel = characterLevel(snapshot).orElse(null);
        PresetStatBundle bundle = statExtractor.extract(snapshot.documents(), characterClass);
        Map<String, Integer> currentPresetNos = currentPresetNos(snapshot);
        List<PresetCombatPower> candidates = candidates(bundle, characterClass, characterLevel, currentPresetNos, mode);
        PresetCombatPower selected = selectCandidate(candidates, mode);
        CombatStatBag selectedStats = bundle.merged(selected.sourcePresetNos());
        Long nexonCombatPower = finalStatCombatPower(snapshot.document(NexonEndpoint.STAT)).orElse(null);
        Long delta = nexonCombatPower == null ? null : selected.combatPower() - nexonCombatPower;
        Double deltaRate = delta == null || nexonCombatPower == 0 ? null : delta / (double) nexonCombatPower;

        return new CombatPowerDebugResponse(
                characterName,
                ocid,
                snapshot.date(),
                characterClass,
                characterLevel,
                mode,
                selected.presetNo(),
                selected.sourcePresetNos(),
                nexonCombatPower,
                delta,
                deltaRate,
                selected.formula(),
                trace(selected.formula()),
                totals(selectedStats),
                selectedStats.weaponNormalization(),
                sourceTotals(selectedStats.contributions()),
                selectedStats.contributions(),
                candidates
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

    private void printMaxSelectedStatDebug(
            NexonCharacterSnapshot snapshot,
            PresetSelectionMode mode,
            PresetStatBundle bundle,
            PresetCombatPower selected
    ) {
        if (mode != PresetSelectionMode.MAX) {
            return;
        }

        CombatStatBag selectedStats = bundle.merged(selected.sourcePresetNos());

        System.out.println();
        System.out.println("========== MAX Combat Power Stat Debug ==========");
        System.out.println("date = " + snapshot.date());
        System.out.println("selectedPresetNos = " + selected.sourcePresetNos());
        System.out.println("combatPower = " + selected.combatPower());

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

        System.out.println("=================================================");
        System.out.println();
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
}

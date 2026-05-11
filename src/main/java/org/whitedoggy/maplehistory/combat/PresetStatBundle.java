package org.whitedoggy.maplehistory.combat;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class PresetStatBundle {

    private final CombatStatBag common = new CombatStatBag();
    private final Map<Integer, CombatStatBag> presets = new TreeMap<>();
    private final Map<String, Map<Integer, CombatStatBag>> sourcePresets = new TreeMap<>();

    public CombatStatBag common() {
        return common;
    }

    public CombatStatBag preset(int presetNo) {
        return presets.computeIfAbsent(presetNo, ignored -> new CombatStatBag());
    }

    public CombatStatBag sourcePreset(String source, int presetNo) {
        return sourcePresets
                .computeIfAbsent(source, ignored -> new TreeMap<>())
                .computeIfAbsent(presetNo, ignored -> new CombatStatBag());
    }

    public Map<Integer, CombatStatBag> presets() {
        if (presets.isEmpty()) {
            presets.put(0, new CombatStatBag());
        }
        return presets;
    }

    public CombatStatBag merged(int presetNo) {
        CombatStatBag merged = common.copy();
        merged.merge(presets.getOrDefault(presetNo, new CombatStatBag()));
        return merged;
    }

    public CombatStatBag merged(Map<String, Integer> presetNos) {
        CombatStatBag merged = common.copy();
        presets.values().forEach(merged::merge);
        for (Map.Entry<String, Integer> entry : presetNos.entrySet()) {
            CombatStatBag sourceBag = sourcePresets
                    .getOrDefault(entry.getKey(), Map.of())
                    .get(entry.getValue());
            if (sourceBag != null) {
                merged.merge(sourceBag);
            }
        }
        return merged;
    }

    public Set<String> sources() {
        return sourcePresets.keySet();
    }

    public Set<Integer> sourcePresetNos(String source) {
        return new TreeSet<>(sourcePresets.getOrDefault(source, Map.of()).keySet());
    }
}

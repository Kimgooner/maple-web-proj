package org.whitedoggy.maplehistory.combat;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CombatStatBag {

    private final Map<CombatStatKey, Long> flat = new EnumMap<>(CombatStatKey.class);
    private final Map<CombatStatKey, Long> finalFlat = new EnumMap<>(CombatStatKey.class);
    private final Map<CombatStatKey, Double> percent = new EnumMap<>(CombatStatKey.class);
    private final List<StatContribution> contributions = new ArrayList<>();
    private WeaponNormalization weaponNormalization;

    public void addFlat(CombatStatKey key, long value) {
        flat.merge(key, value, Long::sum);
    }

    public void addFlat(CombatStatKey key, long value, String endpoint, String label, String rawValue) {
        addFlat(key, value);
        contributions.add(new StatContribution(endpoint, label, rawValue, key.name(), "PERCENT_APPLIED_FLAT", value));
    }

    public void addPercent(CombatStatKey key, double value) {
        percent.merge(key, value, Double::sum);
    }

    public void addPercent(CombatStatKey key, double value, String endpoint, String label, String rawValue) {
        addPercent(key, value);
        contributions.add(new StatContribution(endpoint, label, rawValue, key.name(), "PERCENT", value));
    }

    public void addFinalFlat(CombatStatKey key, long value) {
        finalFlat.merge(key, value, Long::sum);
    }

    public void addFinalFlat(CombatStatKey key, long value, String endpoint, String label, String rawValue) {
        addFinalFlat(key, value);
        contributions.add(new StatContribution(endpoint, label, rawValue, key.name(), "PERCENT_NOT_APPLIED_FLAT", value));
    }

    public long flat(CombatStatKey key) {
        return flat.getOrDefault(key, 0L);
    }

    public double percent(CombatStatKey key) {
        return percent.getOrDefault(key, 0.0d);
    }

    public long finalFlat(CombatStatKey key) {
        return finalFlat.getOrDefault(key, 0L);
    }

    public void merge(CombatStatBag other) {
        other.flat.forEach(this::addFlat);
        other.finalFlat.forEach(this::addFinalFlat);
        other.percent.forEach(this::addPercent);
        contributions.addAll(other.contributions);
        if (other.weaponNormalization != null) {
            weaponNormalization = other.weaponNormalization;
        }
    }

    public CombatStatBag copy() {
        CombatStatBag copy = new CombatStatBag();
        copy.merge(this);
        return copy;
    }

    public Map<CombatStatKey, Long> flatValues() {
        return Map.copyOf(flat);
    }

    public Map<CombatStatKey, Long> finalFlatValues() {
        return Map.copyOf(finalFlat);
    }

    public Map<CombatStatKey, Double> percentValues() {
        return Map.copyOf(percent);
    }

    public List<StatContribution> contributions() {
        return List.copyOf(contributions);
    }

    public void applyWeaponNormalization(WeaponNormalization normalization) {
        applyWeaponNormalization(normalization, CombatStatKey.ATTACK_POWER);
    }

    public void applyWeaponNormalization(WeaponNormalization normalization, CombatStatKey statKey) {
        if (normalization == null || normalization.delta() == 0) {
            return;
        }
        weaponNormalization = normalization;
        addFlat(
                statKey,
                normalization.delta(),
                "WEAPON_NORMALIZATION",
                normalization.weaponName(),
                normalization.originalAttack() + " -> " + normalization.normalizedAttack()
        );
    }

    public void applyWeaponNormalizationAbsolute(WeaponNormalization normalization, CombatStatKey statKey) {
        if (normalization == null || normalization.normalizedAttack() == 0) {
            return;
        }
        weaponNormalization = normalization;
        addFlat(
                statKey,
                normalization.normalizedAttack(),
                "WEAPON_NORMALIZATION",
                normalization.weaponName(),
                normalization.originalAttack() + " -> " + normalization.normalizedAttack()
        );
    }

    public WeaponNormalization weaponNormalization() {
        return weaponNormalization;
    }
}

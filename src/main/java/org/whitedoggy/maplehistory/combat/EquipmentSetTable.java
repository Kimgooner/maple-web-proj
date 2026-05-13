package org.whitedoggy.maplehistory.combat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class EquipmentSetTable {

    private static final String RESOURCE_PATH = "/set-effect-table.json";

    private final Map<String, Definition> definitionsByName;
    private final Map<String, LuckyItem> luckyItemsByName;

    private EquipmentSetTable(List<Definition> definitions, List<LuckyItem> luckyItems) {
        this.definitionsByName = definitions.stream()
                .collect(Collectors.toUnmodifiableMap(Definition::name, Function.identity()));
        this.luckyItemsByName = luckyItems.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                lucky -> normalizeName(lucky.name()),
                                Function.identity(),
                                (left, right) -> left,
                                LinkedHashMap::new
                        ),
                        Map::copyOf
                ));
    }

    static EquipmentSetTable loadDefault() {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = EquipmentSetTable.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing resource: " + RESOURCE_PATH);
            }
            JsonNode root = objectMapper.readTree(inputStream);
            return new EquipmentSetTable(definitions(root.path("sets")), luckyItems(root.path("luckyItems")));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load set effect table", exception);
        }
    }

    private static List<Definition> definitions(JsonNode sets) {
        if (sets == null || !sets.isArray()) {
            return List.of();
        }
        List<Definition> definitions = new ArrayList<>();
        for (JsonNode set : sets) {
            String name = set.path("name").asText("");
            if (name.isBlank()) {
                continue;
            }
            definitions.add(new Definition(name, items(set.path("items")), effects(set.path("effects"))));
        }
        return definitions;
    }

    private static List<Item> items(JsonNode items) {
        if (items == null || !items.isArray()) {
            return List.of();
        }
        List<Item> values = new ArrayList<>();
        for (JsonNode item : items) {
            String name;
            String slot;
            List<String> aliases = List.of();
            if (item.isTextual()) {
                name = item.asText("");
                slot = "";
            } else {
                name = item.path("name").asText("");
                slot = item.path("slot").asText("");
                aliases = textArray(item.path("aliases"));
            }
            if (!name.isBlank()) {
                values.add(new Item(name, slot, aliases));
            }
        }
        return values;
    }

    private static List<Effect> effects(JsonNode effects) {
        if (effects == null || !effects.isArray()) {
            return List.of();
        }
        List<Effect> values = new ArrayList<>();
        for (JsonNode effect : effects) {
            int count = effect.path("count").asInt(0);
            String option = effect.path("option").asText("");
            if (count > 0 && !option.isBlank()) {
                values.add(new Effect(count, option));
            }
        }
        return values;
    }

    private static List<LuckyItem> luckyItems(JsonNode luckyItems) {
        if (luckyItems == null || !luckyItems.isArray()) {
            return List.of();
        }
        List<LuckyItem> values = new ArrayList<>();
        int priority = 0;
        for (JsonNode luckyItem : luckyItems) {
            String name = luckyItem.path("name").asText("");
            String slot = luckyItem.path("slot").asText("");
            if (name.isBlank()) {
                continue;
            }
            values.add(new LuckyItem(
                    name,
                    slot,
                    textArray(luckyItem.path("aliases")),
                    textArray(luckyItem.path("setNames")),
                    priority++
            ));
        }
        return values;
    }

    private static List<String> textArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            String text = value.asText("");
            if (!text.isBlank()) {
                values.add(text);
            }
        }
        return values;
    }

    Definition definition(String setName) {
        return definitionsByName.get(setName);
    }

    List<Definition> definitions() {
        return List.copyOf(definitionsByName.values());
    }

    Optional<LuckyItem> luckyItem(String itemName, String slot) {
        String normalizedItemName = normalizeName(itemName);
        LuckyItem exact = luckyItemsByName.get(normalizedItemName);
        if (exact != null && exact.matches(normalizedItemName, slot)) {
            return Optional.of(exact);
        }
        return luckyItemsByName.values().stream()
                .filter(luckyItem -> luckyItem.matches(normalizedItemName, slot))
                .findFirst();
    }

    static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s:,'\"·.()\\-]", "");
    }

    record Definition(
            String name,
            List<Item> items,
            List<Effect> effects
    ) {
        boolean matches(String itemName, String slot) {
            return items.stream().anyMatch(item -> item.matches(itemName, slot));
        }

        boolean supportsLuckySlot(String slot) {
            return items.stream().anyMatch(item -> item.slot().equals(slot));
        }
    }

    record Item(
            String name,
            String slot,
            List<String> aliases
    ) {
        List<String> normalizedNames() {
            List<String> names = new ArrayList<>();
            names.add(EquipmentSetTable.normalizeName(name));
            aliases.forEach(alias -> names.add(EquipmentSetTable.normalizeName(alias)));
            return names;
        }

        boolean matches(String itemName, String slot) {
            String normalizedItemName = EquipmentSetTable.normalizeName(itemName);
            if (this.slot != null && !this.slot.isBlank() && slot != null && !slot.isBlank() && !this.slot.equals(slot)) {
                return false;
            }
            return normalizedNames().stream().anyMatch(normalizedItemName::startsWith);
        }
    }

    record Effect(
            int count,
            String option
    ) {
    }

    record LuckyItem(
            String name,
            String slot,
            List<String> aliases,
            List<String> setNames,
            int priority
    ) {
        boolean appliesTo(String setName) {
            return setNames.isEmpty() || setNames.contains(setName);
        }

        boolean matches(String normalizedItemName, String slot) {
            if (this.slot != null && !this.slot.isBlank() && slot != null && !slot.isBlank() && !this.slot.equals(slot)) {
                return false;
            }
            if (EquipmentSetTable.normalizeName(name).equals(normalizedItemName)) {
                return true;
            }
            return aliases.stream()
                    .map(EquipmentSetTable::normalizeName)
                    .anyMatch(normalizedItemName::startsWith);
        }
    }
}

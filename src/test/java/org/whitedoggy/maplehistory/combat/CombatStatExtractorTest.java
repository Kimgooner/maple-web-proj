package org.whitedoggy.maplehistory.combat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.whitedoggy.maplehistory.nexon.NexonEndpoint;
import tools.jackson.databind.ObjectMapper;

class CombatStatExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CombatStatExtractor extractor = new CombatStatExtractor();

    @Test
    void splitsCommonAndPresetStats() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "preset_no": 2,
                  "item_equipment_preset_1": [
                    {"item_total_option": {"str": "100", "attack_power": "10"}}
                  ],
                  "item_equipment_preset_2": [
                    {"item_total_option": {"str": "200", "attack_power": "20"}}
                  ],
                  "item_equipment": [
                    {"item_total_option": {"str": "9999", "attack_power": "9999"}}
                  ]
                }
                """);
        var unionArtifact = objectMapper.readTree("""
                {
                  "union_artifact_effect": [
                    {"name": "보스 몬스터 공격 시 데미지", "level": 5, "stat_value": "15%"}
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(
                NexonEndpoint.ITEM_EQUIPMENT, itemEquipment,
                NexonEndpoint.UNION_ARTIFACT, unionArtifact
        ));

        assertThat(bundle.merged(Map.of("ITEM_EQUIPMENT", 1)).flat(CombatStatKey.STR)).isEqualTo(100);
        assertThat(bundle.merged(Map.of("ITEM_EQUIPMENT", 2)).flat(CombatStatKey.STR)).isEqualTo(200);
        assertThat(bundle.merged(Map.of("ITEM_EQUIPMENT", 1)).percent(CombatStatKey.BOSS_DAMAGE)).isEqualTo(15);
        assertThat(bundle.merged(Map.of("ITEM_EQUIPMENT", 2)).percent(CombatStatKey.BOSS_DAMAGE)).isEqualTo(15);
    }
}

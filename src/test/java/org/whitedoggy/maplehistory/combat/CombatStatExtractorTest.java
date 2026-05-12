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

    @Test
    void includesDragonEquipmentInCommonItemStats() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_total_option": {"int": "100", "magic_power": "10"}
                    }
                  ],
                  "dragon_equipment": [
                    {
                      "item_total_option": {"int": "14", "magic_power": "7"}
                    },
                    {
                      "item_total_option": {"int": "9", "luk": "18"}
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.ITEM_EQUIPMENT, itemEquipment), "에반");

        assertThat(bundle.common().flat(CombatStatKey.INT)).isEqualTo(123);
        assertThat(bundle.common().flat(CombatStatKey.LUK)).isEqualTo(18);
        assertThat(bundle.common().flat(CombatStatKey.MAGIC_ATTACK)).isEqualTo(17);
    }

    @Test
    void excludesExpiredTitleStats() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "title": {
                    "title_name": "Infinite Flame",
                    "title_description": "사용 기간이 만료되었습니다.\\n\\n올스탯 +30\\n공격력/마력+30\\n보스 몬스터 데미지+20%"
                  }
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.ITEM_EQUIPMENT, itemEquipment), "렌");

        assertThat(bundle.common().flat(CombatStatKey.ALL_STAT)).isZero();
        assertThat(bundle.common().flat(CombatStatKey.ATTACK_POWER)).isZero();
        assertThat(bundle.common().percent(CombatStatKey.BOSS_DAMAGE)).isZero();
    }

    @Test
    void excludesSpecificSkillDamageFromGenericDamage() throws Exception {
        var unionArtifact = objectMapper.readTree("""
                {
                  "union_artifact_effect": [
                    {"name": "파이널 어택류 스킬의 데미지", "level": 5, "stat_value": "30%"},
                    {"name": "데미지", "level": 5, "stat_value": "15%"}
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.UNION_ARTIFACT, unionArtifact));

        assertThat(bundle.common().percent(CombatStatKey.DAMAGE)).isEqualTo(15);
    }

    @Test
    void beginnerSkillFilterKeepsEventAndPetSetButExcludesPermanentGenericSkill() throws Exception {
        var skill0 = objectMapper.readTree("""
                {
                  "character_skill": [
                    {
                      "skill_name": "연합의 의지",
                      "skill_effect": "영구적으로 힘 5, 민첩 5, 지능 5, 행운 5, 공격력 5, 마력 5 증가"
                    },
                    {
                      "skill_name": "새 이벤트 버프",
                      "skill_effect": "공격력/마력 20 증가, 보스 몬스터 공격 시 데미지 40% 증가, 올스탯 30 증가"
                    },
                    {
                      "skill_name": "보스 전용 이벤트 버프",
                      "skill_effect": "보스 몬스터 공격 시 데미지 40% 증가, 몬스터파크 퇴장 시 획득하는 경험치 50% 증가"
                    },
                    {
                      "skill_name": "어떤 펫 세트 Lv.2",
                      "skill_effect": "공격력 10, 마력 10증가"
                    },
                    {
                      "skill_name": "어떤 펫 세트 Lv.3",
                      "skill_effect": "공격력 18, 마력 18증가"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.SKILL_0, skill0));

        assertThat(bundle.common().flat(CombatStatKey.STR)).isZero();
        assertThat(bundle.common().flat(CombatStatKey.ATTACK_POWER)).isEqualTo(48);
        assertThat(bundle.common().flat(CombatStatKey.MAGIC_ATTACK)).isEqualTo(48);
        assertThat(bundle.common().flat(CombatStatKey.ALL_STAT)).isEqualTo(30);
        assertThat(bundle.common().percent(CombatStatKey.BOSS_DAMAGE)).isEqualTo(80);
    }

    @Test
    void appliesProjectileFallbackForJobsThatUseProjectiles() {
        assertThat(extractor.extract(Map.of(), "윈드브레이커").common().flat(CombatStatKey.ATTACK_POWER)).isEqualTo(9);
        assertThat(extractor.extract(Map.of(), "신궁").common().flat(CombatStatKey.ATTACK_POWER)).isEqualTo(9);
        assertThat(extractor.extract(Map.of(), "나이트로드").common().flat(CombatStatKey.ATTACK_POWER)).isEqualTo(29);
        assertThat(extractor.extract(Map.of(), "캡틴").common().flat(CombatStatKey.ATTACK_POWER)).isEqualTo(22);
        assertThat(extractor.extract(Map.of(), "비숍").common().flat(CombatStatKey.ATTACK_POWER)).isZero();
    }

    @Test
    void parsesGroupedBasicStatsIndividually() throws Exception {
        var unionRaider = objectMapper.readTree("""
                {
                  "union_raider_stat": [
                    "STR, DEX, INT, LUK 40 증가",
                    "STR, DEX, LUK 30 증가"
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.UNION_RAIDER, unionRaider));

        assertThat(bundle.common().finalFlat(CombatStatKey.STR)).isEqualTo(70);
        assertThat(bundle.common().finalFlat(CombatStatKey.DEX)).isEqualTo(70);
        assertThat(bundle.common().finalFlat(CombatStatKey.INT)).isEqualTo(40);
        assertThat(bundle.common().finalFlat(CombatStatKey.LUK)).isEqualTo(70);
    }

    @Test
    void separatesUnionRaiderMemberAndOccupiedStatApplication() throws Exception {
        var unionRaider = objectMapper.readTree("""
                {
                  "union_raider_stat": [
                    "STR 100 증가",
                    "DEX 80 증가",
                    "STR, DEX, INT, LUK 50 증가",
                    "보스 몬스터 공격 시 데미지 6% 증가",
                    "공격력/마력 25 증가",
                    "공격 시 20%의 확률로 데미지 20% 증가",
                    "크리티컬 확률 5% 증가",
                    "방어율 무시 6% 증가"
                  ],
                  "union_occupied_stat": [
                    "STR 75 증가",
                    "DEX 5 증가",
                    "크리티컬 데미지 20.00% 증가",
                    "공격력 15 증가",
                    "보스 몬스터 공격 시 데미지 40% 증가"
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.UNION_RAIDER, unionRaider));
        CombatStatBag bag = bundle.common();

        assertThat(bag.finalFlat(CombatStatKey.STR)).isEqualTo(150);
        assertThat(bag.finalFlat(CombatStatKey.DEX)).isEqualTo(130);
        assertThat(bag.finalFlat(CombatStatKey.INT)).isEqualTo(50);
        assertThat(bag.finalFlat(CombatStatKey.LUK)).isEqualTo(50);
        assertThat(bag.flat(CombatStatKey.STR)).isEqualTo(75);
        assertThat(bag.flat(CombatStatKey.DEX)).isEqualTo(5);
        assertThat(bag.flat(CombatStatKey.ATTACK_POWER)).isEqualTo(40);
        assertThat(bag.flat(CombatStatKey.MAGIC_ATTACK)).isEqualTo(25);
        assertThat(bag.percent(CombatStatKey.BOSS_DAMAGE)).isEqualTo(46);
        assertThat(bag.percent(CombatStatKey.CRITICAL_DAMAGE)).isEqualTo(20);
        assertThat(bag.percent(CombatStatKey.DAMAGE)).isZero();
    }

    @Test
    void parsesActualBossUnionPresetMemberAndOccupiedEffects() throws Exception {
        var unionRaider = objectMapper.readTree("""
                {
                  "union_raider_stat": [
                    "크리티컬 확률 5% 증가",
                    "방어율 무시 6% 증가",
                    "상태 이상 내성 4 증가",
                    "INT 100 증가",
                    "LUK 80 증가",
                    "STR 100 증가",
                    "경험치 획득량 12% 증가",
                    "DEX 80 증가",
                    "메소 획득량 4% 증가",
                    "보스 몬스터 공격 시 데미지 6% 증가",
                    "최대 HP 6% 증가",
                    "최대 HP 2000 증가",
                    "최대 MP 5% 증가",
                    "최대 HP 2000 증가",
                    "DEX 80 증가",
                    "적 공격마다 70%의 확률로 순수 MP의 8% 회복",
                    "크리티컬 데미지 6% 증가",
                    "STR 100 증가",
                    "소환수 지속시간 10% 증가",
                    "STR 100 증가",
                    "버프 지속시간 25% 증가",
                    "INT 80 증가",
                    "STR 100 증가",
                    "공격 시 20%의 확률로 데미지 20% 증가",
                    "DEX 80 증가",
                    "INT 80 증가",
                    "STR 100 증가",
                    "적 공격마다 70%의 확률로 순수 HP의 10% 회복",
                    "STR 100 증가",
                    "STR 100 증가",
                    "LUK 80 증가",
                    "STR 100 증가",
                    "DEX 80 증가",
                    "LUK 80 증가",
                    "이동속도, 최대 이동속도 10 증가. 최대 이동속도 170 이상 시 초과분의 20% 적용, 렌 공격대원 효과로 증가하는 최대 이동속도는 190 초과 불가",
                    "공격력/마력 25 증가",
                    "INT 80 증가",
                    "STR, DEX, LUK 50 증가",
                    "스킬 재사용 대기시간 6% 감소",
                    "DEX 80 증가",
                    "크리티컬 확률 5% 증가"
                  ],
                  "union_occupied_stat": [
                    "DEX 5 증가",
                    "STR 75 증가",
                    "크리티컬 데미지 20.00% 증가",
                    "공격력 15 증가",
                    "보스 몬스터 공격 시 데미지 40% 증가",
                    "크리티컬 확률 3% 증가",
                    "버프 지속시간 33% 증가",
                    "방어율 무시 40% 증가"
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.UNION_RAIDER, unionRaider));
        CombatStatBag bag = bundle.common();

        assertThat(bag.flat(CombatStatKey.STR)).isEqualTo(75);
        assertThat(bag.flat(CombatStatKey.DEX)).isEqualTo(5);
        assertThat(bag.flat(CombatStatKey.ATTACK_POWER)).isEqualTo(40);
        assertThat(bag.flat(CombatStatKey.MAGIC_ATTACK)).isEqualTo(25);
        assertThat(bag.finalFlat(CombatStatKey.STR)).isEqualTo(850);
        assertThat(bag.finalFlat(CombatStatKey.DEX)).isEqualTo(450);
        assertThat(bag.finalFlat(CombatStatKey.INT)).isEqualTo(340);
        assertThat(bag.finalFlat(CombatStatKey.LUK)).isEqualTo(290);
        assertThat(bag.percent(CombatStatKey.BOSS_DAMAGE)).isEqualTo(46);
        assertThat(bag.percent(CombatStatKey.CRITICAL_DAMAGE)).isEqualTo(26);
        assertThat(bag.percent(CombatStatKey.DAMAGE)).isZero();
    }

    @Test
    void usesActiveHexaStatCoresInsteadOfDuplicatedPresetCores() throws Exception {
        var hexa = objectMapper.readTree("""
                {
                  "character_hexa_stat_core": [
                    {
                      "main_stat_name": "주력 스탯 증가",
                      "sub_stat_name_1": "보스 데미지 증가",
                      "sub_stat_name_2": "크리티컬 데미지 증가",
                      "main_stat_level": 10,
                      "sub_stat_level_1": 4,
                      "sub_stat_level_2": 6
                    }
                  ],
                  "preset_hexa_stat_core": [
                    {
                      "slot_id": "0",
                      "main_stat_name": "주력 스탯 증가",
                      "sub_stat_name_1": "보스 데미지 증가",
                      "sub_stat_name_2": "크리티컬 데미지 증가",
                      "main_stat_level": 10,
                      "sub_stat_level_1": 4,
                      "sub_stat_level_2": 6
                    },
                    {
                      "slot_id": "1",
                      "main_stat_name": "주력 스탯 증가",
                      "sub_stat_name_1": "보스 데미지 증가",
                      "sub_stat_name_2": "크리티컬 데미지 증가",
                      "main_stat_level": 10,
                      "sub_stat_level_1": 4,
                      "sub_stat_level_2": 6
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.HEXA_MATRIX_STAT, hexa), "아델");

        assertThat(bundle.common().finalFlat(CombatStatKey.STR)).isEqualTo(2000);
        assertThat(bundle.common().percent(CombatStatKey.BOSS_DAMAGE)).isEqualTo(4);
        assertThat(bundle.common().percent(CombatStatKey.CRITICAL_DAMAGE)).isEqualTo(2.1);
    }

    @Test
    void parsesMagicHexaStatLinesForMagicJobs() throws Exception {
        var hexa = objectMapper.readTree("""
                {
                  "character_hexa_stat_core": [
                    {
                      "main_stat_name": "주력 스탯 증가",
                      "sub_stat_name_1": "크리티컬 데미지 증가",
                      "sub_stat_name_2": "마력 증가",
                      "main_stat_level": 7,
                      "sub_stat_level_1": 6,
                      "sub_stat_level_2": 7
                    }
                  ],
                  "character_hexa_stat_core_2": [
                    {
                      "main_stat_name": "크리티컬 데미지 증가",
                      "sub_stat_name_1": "주력 스탯 증가",
                      "sub_stat_name_2": "마력 증가",
                      "main_stat_level": 3,
                      "sub_stat_level_1": 10,
                      "sub_stat_level_2": 7
                    }
                  ],
                  "character_hexa_stat_core_3": [
                    {
                      "main_stat_name": "마력 증가",
                      "sub_stat_name_1": "주력 스탯 증가",
                      "sub_stat_name_2": "크리티컬 데미지 증가",
                      "main_stat_level": 6,
                      "sub_stat_level_1": 8,
                      "sub_stat_level_2": 6
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.HEXA_MATRIX_STAT, hexa), "일리움");

        assertThat(bundle.common().finalFlat(CombatStatKey.INT)).isEqualTo(2800);
        assertThat(bundle.common().flat(CombatStatKey.MAGIC_ATTACK)).isEqualTo(110);
        assertThat(bundle.common().flat(CombatStatKey.ATTACK_POWER)).isZero();
        assertThat(bundle.common().percent(CombatStatKey.CRITICAL_DAMAGE)).isEqualTo(5.25);
    }

    @Test
    void parsesLevelScalingPotentialOptionsWithCharacterLevel() throws Exception {
        var basic = objectMapper.readTree("""
                {"character_level": 285}
                """);
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_total_option": {"int": "100"},
                      "additional_potential_option_1": "캐릭터 기준 9레벨 당 INT +2"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(
                NexonEndpoint.BASIC, basic,
                NexonEndpoint.ITEM_EQUIPMENT, itemEquipment
        ), "일리움");

        assertThat(bundle.common().flat(CombatStatKey.INT)).isEqualTo(162);
    }

    @Test
    void structuredEquipmentAllStatIsPercentAndSeparateFromPotentialPercent() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_total_option": {
                        "str": "10",
                        "all_stat": "7",
                        "attack_power": "5"
                      },
                      "potential_option_1": "올스탯 +7%"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.ITEM_EQUIPMENT, itemEquipment), "아델");

        assertThat(bundle.common().flat(CombatStatKey.ALL_STAT)).isZero();
        assertThat(bundle.common().percent(CombatStatKey.ALL_STAT)).isEqualTo(14);
    }

    @Test
    void itemExceptionalOptionIsAddedSeparatelyFromItemTotalOption() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_total_option": {
                        "str": "279",
                        "attack_power": "250",
                        "max_hp": "0"
                      },
                      "item_base_option": {
                        "str": "10",
                        "attack_power": "10",
                        "max_hp": "0"
                      },
                      "item_add_option": {
                        "str": "114",
                        "attack_power": "0",
                        "max_hp": "0"
                      },
                      "item_etc_option": {
                        "str": "24",
                        "attack_power": "36",
                        "max_hp": "0"
                      },
                      "item_starforce_option": {
                        "str": "131",
                        "attack_power": "204",
                        "max_hp": "0"
                      },
                      "item_exceptional_option": {
                        "str": "15",
                        "dex": "15",
                        "int": "15",
                        "luk": "15",
                        "max_hp": "750",
                        "attack_power": "10",
                        "magic_power": "10",
                        "exceptional_upgrade": 1
                      }
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.ITEM_EQUIPMENT, itemEquipment), "아델");

        assertThat(bundle.common().flat(CombatStatKey.STR)).isEqualTo(294);
        assertThat(bundle.common().flat(CombatStatKey.ATTACK_POWER)).isEqualTo(260);
        assertThat(bundle.common().flat(CombatStatKey.MAX_HP)).isEqualTo(750);
        assertThat(bundle.common().flat(CombatStatKey.DEX)).isEqualTo(15);
    }

    @Test
    void setEffectParsesSeparateStrDexAndHpValues() throws Exception {
        var setEffect = objectMapper.readTree("""
                {
                  "set_effect": [
                    {
                      "set_name": "루타비스 세트(해적)",
                      "set_effect_info": [
                        {
                          "set_count": 2,
                          "set_option": "STR  +20, DEX  +20, 최대 HP  +1000, 최대 MP  +1000"
                        }
                      ]
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.SET_EFFECT, setEffect), "캐논슈터");

        assertThat(bundle.common().flat(CombatStatKey.STR)).isEqualTo(20);
        assertThat(bundle.common().flat(CombatStatKey.DEX)).isEqualTo(20);
        assertThat(bundle.common().flat(CombatStatKey.MAX_HP)).isEqualTo(1000);
    }

    @Test
    void skipsLevelScalingPotentialOptionsWhenCharacterLevelIsMissing() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "additional_potential_option_1": "캐릭터 기준 9레벨 당 STR +2"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(NexonEndpoint.ITEM_EQUIPMENT, itemEquipment), "아델");

        assertThat(bundle.common().flat(CombatStatKey.STR)).isZero();
    }

    @Test
    void arkUsesStrAsMainStat() {
        CharacterStatProfile profile = CharacterStatProfile.from("아크");

        assertThat(profile.mainStats()).containsExactly(CombatStatKey.STR);
        assertThat(profile.subStats()).containsExactly(CombatStatKey.DEX);
    }

    @Test
    void destinyWeaponAppliesTranscendentWillOnlyAsFixedFinalDamage() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_equipment_slot": "무기",
                      "item_name": "데스티니 튜너"
                    }
                  ]
                }
                """);
        var skill0 = objectMapper.readTree("""
                {
                  "character_skill": [
                    {
                      "skill_name": "파괴의 얄다바오트",
                      "skill_effect": "12명의 적을 1500%의 데미지로 공격"
                    },
                    {
                      "skill_name": "초월 : 결전의 의지",
                      "skill_effect": "영구적으로 최종 데미지 10% 증가"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(
                NexonEndpoint.ITEM_EQUIPMENT, itemEquipment,
                NexonEndpoint.SKILL_0, skill0
        ));

        assertThat(bundle.common().percent(CombatStatKey.FINAL_DAMAGE)).isEqualTo(10);
        assertThat(bundle.common().percent(CombatStatKey.DAMAGE)).isZero();
    }

    @Test
    void genesisWeaponAppliesYaldabaothOnlyAsFixedFinalDamage() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_equipment_slot": "무기",
                      "item_name": "제네시스 튜너"
                    }
                  ]
                }
                """);
        var skill0 = objectMapper.readTree("""
                {
                  "character_skill": [
                    {
                      "skill_name": "파괴의 얄다바오트",
                      "skill_effect": "12명의 적을 1500%의 데미지로 공격"
                    },
                    {
                      "skill_name": "초월 : 결전의 의지",
                      "skill_effect": "영구적으로 최종 데미지 10% 증가"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(
                NexonEndpoint.ITEM_EQUIPMENT, itemEquipment,
                NexonEndpoint.SKILL_0, skill0
        ));

        assertThat(bundle.common().percent(CombatStatKey.FINAL_DAMAGE)).isEqualTo(10);
        assertThat(bundle.common().percent(CombatStatKey.DAMAGE)).isZero();
    }

    @Test
    void transcendentFinalDamageRequiresMatchingBeginnerSkill() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_equipment_slot": "무기",
                      "item_name": "데스티니 튜너"
                    }
                  ]
                }
                """);
        var skill0 = objectMapper.readTree("""
                {
                  "character_skill": [
                    {
                      "skill_name": "파괴의 얄다바오트",
                      "skill_effect": "12명의 적을 1500%의 데미지로 공격"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(
                NexonEndpoint.ITEM_EQUIPMENT, itemEquipment,
                NexonEndpoint.SKILL_0, skill0
        ));

        assertThat(bundle.common().percent(CombatStatKey.FINAL_DAMAGE)).isZero();
        assertThat(bundle.common().percent(CombatStatKey.DAMAGE)).isZero();
    }

    @Test
    void transcendentFinalDamageFollowsSelectedItemPreset() throws Exception {
        var itemEquipment = objectMapper.readTree("""
                {
                  "item_equipment_preset_1": [
                    {
                      "item_equipment_slot": "무기",
                      "item_name": "데스티니 튜너"
                    }
                  ],
                  "item_equipment_preset_2": [
                    {
                      "item_equipment_slot": "무기",
                      "item_name": "아케인셰이드 튜너"
                    }
                  ]
                }
                """);
        var skill0 = objectMapper.readTree("""
                {
                  "character_skill": [
                    {
                      "skill_name": "초월 : 결전의 의지",
                      "skill_effect": "영구적으로 최종 데미지 10% 증가"
                    }
                  ]
                }
                """);

        PresetStatBundle bundle = extractor.extract(Map.of(
                NexonEndpoint.ITEM_EQUIPMENT, itemEquipment,
                NexonEndpoint.SKILL_0, skill0
        ));

        assertThat(bundle.merged(Map.of("ITEM_EQUIPMENT", 1)).percent(CombatStatKey.FINAL_DAMAGE)).isEqualTo(10);
        assertThat(bundle.merged(Map.of("ITEM_EQUIPMENT", 2)).percent(CombatStatKey.FINAL_DAMAGE)).isZero();
    }
}

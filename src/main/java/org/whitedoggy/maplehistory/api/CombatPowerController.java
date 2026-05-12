package org.whitedoggy.maplehistory.api;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.whitedoggy.maplehistory.combat.CombatPowerService;
import org.whitedoggy.maplehistory.combat.CombatPowerDebugResponse;
import org.whitedoggy.maplehistory.combat.CombatPowerTrendResponse;
import org.whitedoggy.maplehistory.combat.PresetSelectionMode;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/characters")
public class CombatPowerController {

    private final CombatPowerService combatPowerService;

    public CombatPowerController(CombatPowerService combatPowerService) {
        this.combatPowerService = combatPowerService;
    }

    @GetMapping("/{characterName}/combat-power/trend")
    public Mono<CombatPowerTrendResponse> trend(
            @PathVariable String characterName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "BATTLE") PresetSelectionMode mode
    ) {
        return combatPowerService.trend(characterName, from, to, mode);
    }

    @GetMapping("/{characterName}/combat-power/debug")
    public Mono<CombatPowerDebugResponse> debug(
            @PathVariable String characterName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "CURRENT") PresetSelectionMode mode
    ) {
        return combatPowerService.debug(characterName, date, mode);
    }
}

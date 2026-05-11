package org.whitedoggy.maplehistory.nexon;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NexonMapleClient {

    private final WebClient webClient;

    public NexonMapleClient(WebClient nexonWebClient) {
        this.webClient = nexonWebClient;
    }

    public Mono<CharacterIdentity> findCharacter(String characterName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(NexonEndpoint.CHARACTER.path())
                        .queryParam("character_name", characterName)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(body -> new CharacterIdentity(requiredText(body, "ocid")));
    }

    public Mono<NexonCharacterSnapshot> fetchSnapshot(String ocid, LocalDate date) {
        return Mono.zip(
                        get(NexonEndpoint.BASIC, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.STAT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.ITEM_EQUIPMENT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.CASH_ITEM_EQUIPMENT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.SET_EFFECT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.SYMBOL_EQUIPMENT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.ANDROID_EQUIPMENT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.PET_EQUIPMENT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode())
                )
                .zipWith(Mono.zip(
                        get(NexonEndpoint.HYPER_STAT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.ABILITY, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        getSkill0(ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.HEXA_MATRIX_STAT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.UNION, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.UNION_RAIDER, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.UNION_CHAMPION, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode()),
                        get(NexonEndpoint.UNION_ARTIFACT, ocid, date).onErrorResume(throwable -> Mono.empty()).defaultIfEmpty(nullNode())
                ))
                .map(tuple -> {
                    Map<NexonEndpoint, JsonNode> documents = new EnumMap<>(NexonEndpoint.class);
                    documents.put(NexonEndpoint.BASIC, tuple.getT1().getT1());
                    documents.put(NexonEndpoint.STAT, tuple.getT1().getT2());
                    documents.put(NexonEndpoint.ITEM_EQUIPMENT, tuple.getT1().getT3());
                    documents.put(NexonEndpoint.CASH_ITEM_EQUIPMENT, tuple.getT1().getT4());
                    documents.put(NexonEndpoint.SET_EFFECT, tuple.getT1().getT5());
                    documents.put(NexonEndpoint.SYMBOL_EQUIPMENT, tuple.getT1().getT6());
                    documents.put(NexonEndpoint.ANDROID_EQUIPMENT, tuple.getT1().getT7());
                    documents.put(NexonEndpoint.PET_EQUIPMENT, tuple.getT1().getT8());
                    documents.put(NexonEndpoint.HYPER_STAT, tuple.getT2().getT1());
                    documents.put(NexonEndpoint.ABILITY, tuple.getT2().getT2());
                    documents.put(NexonEndpoint.SKILL_0, tuple.getT2().getT3());
                    documents.put(NexonEndpoint.HEXA_MATRIX_STAT, tuple.getT2().getT4());
                    documents.put(NexonEndpoint.UNION, tuple.getT2().getT5());
                    documents.put(NexonEndpoint.UNION_RAIDER, tuple.getT2().getT6());
                    documents.put(NexonEndpoint.UNION_CHAMPION, tuple.getT2().getT7());
                    documents.put(NexonEndpoint.UNION_ARTIFACT, tuple.getT2().getT8());
                    return new NexonCharacterSnapshot(ocid, date, documents);
                });
    }

    private Mono<JsonNode> get(NexonEndpoint endpoint, String ocid, LocalDate date) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint.path())
                        .queryParam("ocid", ocid)
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    private Mono<JsonNode> getSkill0(String ocid, LocalDate date) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(NexonEndpoint.SKILL_0.path())
                        .queryParam("ocid", ocid)
                        .queryParam("date", date)
                        .queryParam("character_skill_grade", "0")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    private static String requiredText(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            throw new NexonApiException("Nexon API response did not include required field: " + field);
        }
        return node.get(field).asText();
    }

    private static JsonNode nullNode() {
        return tools.jackson.databind.node.NullNode.getInstance();
    }
}

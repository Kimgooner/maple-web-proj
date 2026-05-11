package org.whitedoggy.maplehistory.nexon;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Map;

public record NexonCharacterSnapshot(
        String ocid,
        LocalDate date,
        Map<NexonEndpoint, JsonNode> documents
) {
    public JsonNode document(NexonEndpoint endpoint) {
        return documents.get(endpoint);
    }
}

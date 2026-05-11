package org.whitedoggy.maplehistory.combat;

public enum PresetSelectionMode {
    MAX,
    CURRENT,
    PRESET_1,
    PRESET_2,
    PRESET_3;

    public int presetNoOrZero() {
        return switch (this) {
            case PRESET_1 -> 1;
            case PRESET_2 -> 2;
            case PRESET_3 -> 3;
            default -> 0;
        };
    }
}

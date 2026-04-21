package model;

public enum Major {

    CS("Computer Science"),
    CPIS("Computer Info Systems"),
    ENGLISH("English"),
    BUSINESS("Business Administration"),
    MATH("Mathematics"),
    BIOLOGY("Biology"),
    PSYCHOLOGY("Psychology"),
    HISTORY("History"),
    NURSING("Nursing"),
    UNDECIDED("Undecided");

    private final String displayName;

    Major(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the human-readable display name shown in the UI. */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Renders as "CODE – Display Name" in ComboBox dropdown.
     */
    @Override
    public String toString() {
        return name() + " – " + displayName;
    }

    /**
     * Case-insensitive lookup by enum name or display name.
     * Falls back to UNDECIDED when value is unrecognised.
     */
    public static Major fromString(String value) {
        if (value == null || value.isBlank()) return UNDECIDED;
        String trimmed = value.trim();
        for (Major m : values()) {
            if (m.name().equalsIgnoreCase(trimmed)) return m;
        }
        for (Major m : values()) {
            if (m.displayName.equalsIgnoreCase(trimmed)) return m;
        }
        for (Major m : values()) {
            if (trimmed.startsWith(m.name())) return m;
        }
        return UNDECIDED;
    }
}
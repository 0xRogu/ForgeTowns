package dev.rogu.forgetowns.data;

public enum GovernmentType {
    DEMOCRACY("Democracy", "Elections determine leadership"),
    MONARCHY("Monarchy", "Ruled by a single leader"),
    ANARCHY("Anarchy", "No formal government");

    private final String name;
    private final String description;

    GovernmentType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

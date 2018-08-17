package com.grelobites.romgenerator.util.winape.model;

public enum PokeValueType {
    DECIMAL(0, "Decimal"),
    HEX(1, "Hex"),
    BCD(2, "BCD"),
    NUMERIC(3, "Numeric (Zero based"),
    NUMERIC_ASCII(4, "Numeric (ASCII)"),
    LONG(5, "Long"),
    STRING(6, "String");

    private int id;
    private String name;

    PokeValueType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static PokeValueType fromId(int id) {
        for (PokeValueType candidate : PokeValueType.values()) {
            if (candidate.id == id) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("No ValueType for id " + id);
    }



}

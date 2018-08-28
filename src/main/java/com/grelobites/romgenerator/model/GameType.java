package com.grelobites.romgenerator.model;


public enum GameType {
    ROM(0, "Lower ROM"),
    UPPER_ROM(1, "Upper ROM"),
    LOWER_UPPER_ROM(2, "Lower and Upper ROM"),
    RAM64(4, "64K"),
    RAM128(8, "128K"),
    RAM64_MLD(0x84, "MLD 64K"),
    RAM128_MLD(0x88, "MLD 128K");

    private static final int MLD_MASK = 0x80;

    private int typeId;
    private String screenName;

    GameType(int typeId, String screenName) {
        this.typeId = typeId;
        this.screenName = screenName;
    }

    public int typeId() {
        return typeId;
    }

    public String screenName() {
        return screenName;
    }

    public static GameType byTypeId(int id) {
        for (GameType type: GameType.values()) {
            if (type.typeId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown typeid " + id);
    }

    public static boolean isMLD(GameType gameType) {
        return (gameType.typeId & MLD_MASK) != 0;
    }
}

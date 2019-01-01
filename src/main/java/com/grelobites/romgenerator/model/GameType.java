package com.grelobites.romgenerator.model;


public enum GameType {
    ROM(0, 16, "Lower ROM"),
    UPPER_ROM(1, 16, "Upper ROM"),
    LOWER_UPPER_ROM(2, 32, "Lower and Upper ROM"),
    RAM64(4, 64, "64K"),
    RAM128(8, 128, "128K"),
    RAM64_MLD(0x84, 64, "MLD 64K"),
    RAM128_MLD(0x88, 128, "MLD 128K");

    private static final int MLD_MASK = 0x80;

    private int typeId;
    private int sizeInKBytes;
    private String screenName;

    GameType(int typeId, int sizeInKbytes, String screenName) {
        this.typeId = typeId;
        this.sizeInKBytes = sizeInKbytes;
        this.screenName = screenName;
    }

    public int typeId() {
        return typeId;
    }

    public String screenName() {
        return screenName;
    }

    public int sizeInKBytes() {
        return sizeInKBytes;
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

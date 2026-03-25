package com.grelobites.romgenerator.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;

public class MLAInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(MLAInfo.class);

    private static final String MLA_SIGNATURE = "MLA";
    public static final int MLA_HEADER_OFFSET = 16362;
    private static final int MLA_SIGNATURE_OFFSET = 16380;
    private static final int MLA_HEADER_SIZE = 22;
    public static final int MLA_DEFAULT_SCREENMODE = 0;

    private int headerSlot;
    private int baseSlot;
    private int mlaType;
    private int tableOffset;
    private int tableRowSize;
    private int tableRows;
    private int rowSlotOffset;
    private int compressedScreenOffset;
    private int compressedScreenSize;
    private int mlaVersion;

    private int mlaPokeAddress;

    public int getMlaType() {
        return mlaType;
    }

    public void setMlaType(int mlaType) {
        this.mlaType = mlaType;
    }

    public int getHeaderSlot() {
        return headerSlot;
    }

    public void setHeaderSlot(int headerSlot) {
        this.headerSlot = headerSlot;
    }

    public int getCompressedScreenOffset() {
        return compressedScreenOffset;
    }

    public void setCompressedScreenOffset(int compressedScreenOffset) {
        this.compressedScreenOffset = compressedScreenOffset;
    }

    public int getCompressedScreenSize() {
        return compressedScreenSize;
    }

    public void setCompressedScreenSize(int compressedScreenSize) {
        this.compressedScreenSize = compressedScreenSize;
    }

    public int getTableOffset() {
        return tableOffset;
    }

    public void setTableOffset(int tableOffset) {
        this.tableOffset = tableOffset;
    }

    public int getTableRowSize() {
        return tableRowSize;
    }

    public void setTableRowSize(int tableRowSize) {
        this.tableRowSize = tableRowSize;
    }

    public int getTableRows() {
        return tableRows;
    }

    public void setTableRows(int tableRows) {
        this.tableRows = tableRows;
    }

    public int getRowSlotOffset() {
        return rowSlotOffset;
    }

    public void setRowSlotOffset(int rowSlotOffset) {
        this.rowSlotOffset = rowSlotOffset;
    }

    public int getMlaVersion() {
        return mlaVersion;
    }

    public void setMlaVersion(int mlaVersion) {
        this.mlaVersion = mlaVersion;
    }

    public int getMlaPokeAddress() {
        return mlaPokeAddress;
    }

    public void setMlaPokeAddress(int mlaPokeAddress) {
        this.mlaPokeAddress = mlaPokeAddress;
    }

    public GameType getGameType() {
        return GameType.byTypeId((mlaType));
    }

    public HardwareMode getHardwareMode() {
        switch (getGameType()) {
            case RAM64_MLA:
                return HardwareMode.HW_CPC464;
            case RAM128_MLA:
                return HardwareMode.HW_CPC6128;
            default:
                return HardwareMode.HW_UNKNOWN;
        }
    }

    public int getBaseSlot() {
        return baseSlot;
    }

    public void setBaseSlot(int baseSlot) {
        this.baseSlot = baseSlot;
    }

    private static Optional<MLAInfo> fromGameSlotByteArray(byte[] data) {
        LOGGER.debug("Got signature as " + new String(data, MLA_SIGNATURE_OFFSET, MLA_SIGNATURE.length()));
        if (MLA_SIGNATURE.equals(new String(data, MLA_SIGNATURE_OFFSET, MLA_SIGNATURE.length()))) {
            ByteBuffer buffer = ByteBuffer.wrap(data, MLA_HEADER_OFFSET, MLA_HEADER_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            MLAInfo mlaInfo = new MLAInfo();
            mlaInfo.setBaseSlot(buffer.get() & 0xff);
            mlaInfo.setMlaType(buffer.get() & 0xff);
            mlaInfo.setMlaPokeAddress(buffer.getShort() & 0xffff);
            buffer.get();
            buffer.getShort(); //Skip five bytes
            mlaInfo.setTableOffset(buffer.getShort() & 0xffff);
            mlaInfo.setTableRowSize(buffer.getShort() & 0xffff);
            mlaInfo.setTableRows(buffer.getShort() & 0xffff);
            mlaInfo.setRowSlotOffset(buffer.get() & 0xff);
            mlaInfo.setCompressedScreenOffset(buffer.getShort() & 0xffff);
            mlaInfo.setCompressedScreenSize(buffer.getShort() & 0xffff);
            LOGGER.debug("MLAInfo is {}", mlaInfo);
            return Optional.of(mlaInfo);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<MLAInfo> fromGameByteArray(List<byte[]> data) {
        LOGGER.debug("Analyzing game with " + data.size() + " slots");
        for (int i = 0; i < data.size(); i++) {
            Optional<MLAInfo> mlaInfoOpt = fromGameSlotByteArray(data.get(i));
            if (mlaInfoOpt.isPresent()) {
                mlaInfoOpt.get().setHeaderSlot(i);
                return mlaInfoOpt;
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "MLAInfo{" +
                "mlaType=0x" + Integer.toHexString(mlaType & 0xFF) +
                ", mlaPokeAddress=0x" + Integer.toHexString(mlaPokeAddress) +
                ", tableOffset=0x" + Integer.toHexString(tableOffset) +
                ", tableRowSize=" + tableRowSize +
                ", tableRows=" + tableRows +
                ", rowSlotOffset=" + rowSlotOffset +
                ", compressedScreenOffset=" + compressedScreenOffset +
                ", compressedScreenSize=" + compressedScreenSize +
                ", mlaVersion=" + mlaVersion +
                ", headerSlot=" + headerSlot +
                ", baseSlot=" + baseSlot +
                '}';
    }
}

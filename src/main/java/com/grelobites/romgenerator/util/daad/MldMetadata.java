package com.grelobites.romgenerator.util.daad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class MldMetadata {
    public static final int MLD_DATA_ROWS = 256;
    public static final int MLD_VERSION = 0;
    public static final int MLD_DATAROW_LENGTH = 4;
    public static final int MLD_SLOTROW_OFFSET = 0;
    public static final int TAP_TABLE_OFFSET = 1024;
    public static final int BASE_SLOT_OFFSET = TAP_TABLE_OFFSET + 40;

    public static class Builder {
        private MldMetadata metadata = new MldMetadata();

        public Builder withBaseSlot(int baseSlot) {
            metadata.setBaseSlot(baseSlot);
            return this;
        }

        public Builder withMldType(MldType mldType) {
            metadata.setMldType(mldType);
            return this;
        }

        public Builder withAllocatedSectors(int allocatedSectors) {
            metadata.setAllocatedSectors(allocatedSectors);
            return this;
        }

        public Builder withTableOffset(int tableOffset) {
            metadata.setTableOffset(tableOffset);
            return this;
        }

        public Builder withDataRowLength(int dataRowLength) {
            metadata.setDataRowLength(dataRowLength);
            return this;
        }

        public Builder withDataRows(int dataRows) {
            metadata.setDataRows(dataRows);
            return this;
        }

        public Builder withSlotRowOffset(int slotRowOffset) {
            metadata.setSlotRowOffset(slotRowOffset);
            return this;
        }

        public Builder withVersion(int version) {
            metadata.setVersion(version);
            return this;
        }

        public Builder withDAADScreen(DaadScreen daadScreen) {
            metadata.setDaadScreen(daadScreen);
            return this;
        }

        public Builder withDAADResources(List<DaadResource> daadResources) {
            metadata.setDaadResources(daadResources);
            return this;
        }

        public Builder withDAADBinaries(DaadBinary[] daadBinaries) {
            metadata.setDaadBinaries(daadBinaries);
            return this;
        }

        public MldMetadata build() {
            return metadata;
        }
    }

    private int baseSlot;
    private MldType mldType = MldType.MLD_48K;
    private int allocatedSectors = 0;
    private int tableOffset = DaadConstants.METADATA_OFFSET;
    private int dataRowLength = MLD_DATAROW_LENGTH;
    private int dataRows = MLD_DATA_ROWS;
    private int slotRowOffset = MLD_SLOTROW_OFFSET;
    private int version = MLD_VERSION;
    private DaadScreen daadScreen;
    private List<DaadResource> daadResources;
    private DaadBinary[] daadBinaries;

    public static Builder newBuilder() {
        return new Builder();
    }

    public int getBaseSlot() {
        return baseSlot;
    }

    public void setBaseSlot(int baseSlot) {
        this.baseSlot = baseSlot;
    }

    public MldType getMldType() {
        return mldType;
    }

    public void setMldType(MldType mldType) {
        this.mldType = mldType;
    }

    public int getAllocatedSectors() {
        return allocatedSectors;
    }

    public void setAllocatedSectors(int allocatedSectors) {
        this.allocatedSectors = allocatedSectors;
    }

    public int getTableOffset() {
        return tableOffset;
    }

    public void setTableOffset(int tableOffset) {
        this.tableOffset = tableOffset;
    }

    public int getDataRowLength() {
        return dataRowLength;
    }

    public void setDataRowLength(int dataRowLength) {
        this.dataRowLength = dataRowLength;
    }

    public int getDataRows() {
        return dataRows;
    }

    public void setDataRows(int dataRows) {
        this.dataRows = dataRows;
    }

    public int getSlotRowOffset() {
        return slotRowOffset;
    }

    public void setSlotRowOffset(int slotRowOffset) {
        this.slotRowOffset = slotRowOffset;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public DaadScreen getDaadScreen() {
        return daadScreen;
    }

    public void setDaadScreen(DaadScreen daadScreen) {
        this.daadScreen = daadScreen;
    }

    public List<DaadResource> getDaadResources() {
        return daadResources;
    }

    public void setDaadResources(List<DaadResource> daadResources) {
        this.daadResources = daadResources;
    }

    public DaadBinary[] getDaadBinaries() {
        return daadBinaries;
    }

    public void setDaadBinaries(DaadBinary[] daadBinaries) {
        this.daadBinaries = daadBinaries;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(DaadConstants.METADATA_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (DaadResource resource : daadResources) {
            int offset = resource.getIndex() * MLD_DATAROW_LENGTH;
            DaadTableEntry.newBuilder()
                    .withSlot(resource.getSlot())
                    .withOffset(resource.getSlotOffset())
                    .withCompression(0)
                    .build()
                    .toBuffer(buffer, offset);
        }
        int offset = TAP_TABLE_OFFSET;
        for (DaadBinary binary: daadBinaries) {
            TapTableEntry.newBuilder()
                    .withSlot(binary.getSlot())
                    .withOffset(binary.getSlotOffset())
                    .withLoadAddress(binary.getLoadAddress())
                    .build()
                    .toBuffer(buffer, offset);
            offset += 5;
        }

        buffer.position(BASE_SLOT_OFFSET);
        buffer.put(Integer.valueOf(baseSlot).byteValue())
                .put(Integer.valueOf(mldType.id()).byteValue())
                .put(Integer.valueOf(allocatedSectors).byteValue())
                .putInt(0)
                .putShort(Integer.valueOf(tableOffset).shortValue())
                .putShort(Integer.valueOf(dataRowLength).shortValue())
                .putShort(Integer.valueOf(dataRows).shortValue())
                .put(Integer.valueOf(slotRowOffset).byteValue())
                .putShort(
                        daadScreen != null ?
                                Integer.valueOf(daadScreen.getSlotOffset())
                                        .shortValue() : 0)
                .putShort(
                        daadScreen != null ?
                                Integer.valueOf(daadScreen.getData().length)
                                        .shortValue() : 0)
                .put(DaadConstants.MLD_SIGNATURE.getBytes())
                .put(Integer.valueOf(version).byteValue());

        return buffer.array();
    }
}

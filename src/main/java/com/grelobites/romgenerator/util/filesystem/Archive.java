package com.grelobites.romgenerator.util.filesystem;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class Archive {
    private static AtomicLong ID_GENERATOR = new AtomicLong(0);
    private final long id;
    private String name;
    private String extension;
    private int userArea;
    private byte[] data;
    private EnumSet<ArchiveFlags> flags = EnumSet.noneOf(ArchiveFlags.class);

    public Archive(String name, String extension, int userArea, byte[] data) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name;
        this.extension = extension;
        this.userArea = userArea;
        this.data = data;
    }

    public Archive(String name, String extension, int userArea, byte[] data, EnumSet<ArchiveFlags> flags) {
        this(name, extension, userArea, data);
        this.flags = flags;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getUserArea() {
        return userArea;
    }

    public void setUserArea(int userArea) {
        this.userArea = userArea;
    }

    public EnumSet<ArchiveFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<ArchiveFlags> flags) {
        this.flags = flags;
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return data.length;
    }


    @Override
    public String toString() {
        return "Archive{" +
                "id=" + id +
                ", name=" + name +
                ", extension=" + extension +
                ", userArea=" + userArea +
                ", flags=" + flags +
                ", size=" + data.length +
                '}';
    }
}

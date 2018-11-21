package com.grelobites.romgenerator.util.tape;

public class Binary {
    private String name;
    private byte[] data;
    private int loadAddress;
    private int execAddress;

    public static class Builder {
        private Binary binary = new Binary();

        public Builder withName(String name) {
            binary.name = name;
            return this;
        }

        public Builder withData(byte[] data) {
            binary.data = data;
            return this;
        }

        public Builder withLoadAddress(int loadAddress) {
            binary.loadAddress = loadAddress;
            return this;
        }

        public Builder withExecAddress(int execAddress) {
            binary.execAddress = execAddress;
            return this;
        }

        public Binary build() {
            return binary;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getLoadAddress() {
        return loadAddress;
    }

    public void setLoadAddress(int loadAddress) {
        this.loadAddress = loadAddress;
    }

    public int getExecAddress() {
        return execAddress;
    }

    public void setExecAddress(int execAddress) {
        this.execAddress = execAddress;
    }
}

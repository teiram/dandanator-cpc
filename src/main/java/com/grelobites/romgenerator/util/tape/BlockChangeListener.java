package com.grelobites.romgenerator.util.tape;

import java.io.IOException;

@FunctionalInterface
public interface BlockChangeListener {

    void onBlockChange(int blockNumber) throws IOException;
}

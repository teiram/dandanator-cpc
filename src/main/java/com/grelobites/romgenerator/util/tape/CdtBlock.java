package com.grelobites.romgenerator.util.tape;

import java.io.IOException;
import java.io.OutputStream;

public interface CdtBlock {

    void dump(OutputStream os) throws IOException;
}

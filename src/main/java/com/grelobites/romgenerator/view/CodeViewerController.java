package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.tape.Binary;
import com.grelobites.romgenerator.util.tape.BinaryBasicLoader;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class CodeViewerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeViewerController.class);

    @FXML
    private TextArea codeTextArea;

    @FXML
    void initialize() throws IOException {
        codeTextArea.setPrefColumnCount(40);
        BinaryBasicLoader loader = new BinaryBasicLoader(
                Binary.builder()
                        .withData(Constants.getRescueLoader())
                        .withLoadAddress(Constants.RESCUE_LOADER_ADDRESS)
                        .withExecAddress(Constants.RESCUE_LOADER_ADDRESS)
                        .withName(Constants.RESCUE_LOADER_NAME)
                        .build());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        loader.dump(new PrintStream(bos));
        codeTextArea.setText(bos.toString());
    }

}

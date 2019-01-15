package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.arduino.*;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CpldProgrammerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpldProgrammerController.class);

    private ApplicationContext applicationContext;

    @FXML
    private Circle arduinoDetectedLed;

    @FXML
    private Circle arduinoValidatedLed;

    @FXML
    private Circle arduinoUpdatedLed;

    @FXML
    private Circle dandanatorDetectedLed;

    @FXML
    private Circle dandanatorUpdatedLed;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button programButton;

    private BooleanProperty programming;

    private DoubleProperty progress;

    private Animation currentLedAnimation;

    private SerialPort serialPort;
    private Stk500Programmer arduinoProgrammer;
    private XsvfUploader xsvfUploader;

    public CpldProgrammerController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.programming = new SimpleBooleanProperty(false);
        this.progress = new SimpleDoubleProperty(0.0);
    }

    public Animation createLedAnimation(Circle led) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), led);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(false);
        return ft;
    }

    private void setLedOKStatus(Circle led) {
        led.setFill(Color.GREEN);
    }

    private void setLedWorkingStatus(Circle led) {
        led.setFill(Color.WHITE);
    }

    private void setLedErrorStatus(Circle led) {
        led.setFill(Color.RED);
    }

    private void resetLedStatus(Circle... leds) {
        for (Circle led: leds) {
            led.setFill(Color.DARKGREY);
        }
    }

    public void onProgrammingEnd() {
        Platform.runLater(() -> {
            programming.set(false);
        });
    }

    public void onProgrammingStart() {
        Platform.runLater(() -> {
            programming.set(true);
            progress.set(0.0);
            resetLedStatus(arduinoDetectedLed, arduinoValidatedLed, arduinoUpdatedLed,
                    dandanatorDetectedLed, dandanatorUpdatedLed);
        });
    }

    private void onStartOperation(Circle led) {
        Platform.runLater(() -> {
            setLedWorkingStatus(led);
            currentLedAnimation = createLedAnimation(led);
            currentLedAnimation.play();
        });
    }

    private void onFailedOperation(Circle led) {
        Platform.runLater(() -> {
            currentLedAnimation.stop();
            setLedErrorStatus(led);
        });
    }

    private void onSuccessfulOperation(Circle led, double currentProgress) {
        Platform.runLater(() -> {
            currentLedAnimation.stop();
            setLedOKStatus(led);
            progress.set(currentProgress);
        });
    }

    private static void validateSignature(byte[] signature) {
        throw new IllegalArgumentException("Unsupported arduino signature");
    }

    @FXML
    void initialize() throws IOException {
        programButton.disableProperty().bind(programming.or(
                EepromWriterConfiguration.getInstance().serialPortProperty().isEmpty()));
        progressBar.progressProperty().bind(progress);

        programButton.setOnAction(c -> {

            applicationContext.addBackgroundTask(() -> {
                onProgrammingStart();
                try {
                    try {
                        LOGGER.debug("Starting arduino detection");
                        onStartOperation(arduinoDetectedLed);
                        serialPort = new SerialPort(EepromWriterConfiguration
                                .getInstance().getSerialPort());
                        serialPort.openPort();
                        serialPort.setParams(SerialPort.BAUDRATE_115200,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);

                        arduinoProgrammer = new Stk500Programmer(serialPort);
                        arduinoProgrammer.initialize();
                        arduinoProgrammer.sync();
                        onSuccessfulOperation(arduinoDetectedLed, 0.1);
                    } catch (Exception e) {
                        LOGGER.error("Trying to detect and sync on arduino");
                        onFailedOperation(arduinoDetectedLed);
                        throw e;
                    }

                    try {
                        LOGGER.debug("Starting arduino validation");
                        onStartOperation(arduinoValidatedLed);

                        byte[] signature = arduinoProgrammer.getDeviceSignature();
                        validateSignature(signature);
                        arduinoProgrammer.enterProgramMode();
                        onSuccessfulOperation(arduinoValidatedLed, 0.3);
                    } catch (Exception e) {
                        LOGGER.error("During arduino validation", e);
                        onFailedOperation(arduinoValidatedLed);
                        throw e;
                    }

                    try {
                        LOGGER.debug("Starting arduino update");
                        onStartOperation(arduinoUpdatedLed);
                        List<Binary> binaries = HexUtil.toBinaryList(ArduinoConstants.hexResource());
                        for (Binary binary : binaries) {
                            arduinoProgrammer.programBinary(binary, true, true);
                        }
                        onSuccessfulOperation(arduinoUpdatedLed, 0.6);
                    } catch (Exception e) {
                        LOGGER.error("During arduino update", e);
                        onFailedOperation(arduinoUpdatedLed);
                        throw e;
                    }

                    onSuccessfulOperation(dandanatorDetectedLed, 0.7);
                    try {
                        LOGGER.debug("Starting dandanator update");
                        onStartOperation(dandanatorUpdatedLed);
                        xsvfUploader = new XsvfUploader(serialPort);
                        xsvfUploader.upload(ArduinoConstants.xsvfResource());
                        onSuccessfulOperation(dandanatorUpdatedLed, 1.0);
                    } catch (Exception e) {
                        LOGGER.error("During dandanator detection");
                        onFailedOperation(dandanatorUpdatedLed);
                    }

                    return OperationResult.successResult();
                } catch (Exception e) {
                    LOGGER.error("During programming operation", e);
                    return OperationResult.errorResult("foo", "During Programming Operation");
                } finally {
                    onProgrammingEnd();
                }
            });
        });
    }
}

package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConfiguration;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.eewriter.DataProducer;
import com.grelobites.romgenerator.util.eewriter.SerialBlockService;
import com.grelobites.romgenerator.util.eewriter.SerialDataProducer;
import com.grelobites.romgenerator.util.player.SampledAudioDataPlayer;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.Future;

public class EepromWriterController {
    private static final Logger LOGGER = LoggerFactory.getLogger(EepromWriterController.class);

    private static final String PLAY_BUTTON_STYLE = "button-send";
    private static final String STOP_BUTTON_STYLE = "button-stop";
    private static final int ROMSET_SIZE = Constants.SLOT_SIZE * 32;

    private static final String UNDEFINED_STRING = "--";
    private static EepromWriterConfiguration configuration = EepromWriterConfiguration
            .getInstance();

    @FXML
    private ProgressBar blockProgress;

    @FXML
    private ProgressBar overallProgress;

    @FXML
    private Label requestedBlock;

    @FXML
    private Circle txLed;

    @FXML
    private Circle rxLed;

    @FXML
    private Button rescueLoaderPlayButton;

    @FXML
    private Button rescueLoaderShowButton;

    @FXML
    private Button usbLoaderSendButton;

    private ApplicationContext applicationContext;

    private BooleanProperty playing;

    private BooleanProperty rescuePlaying;

    private BooleanProperty usbRescueSending;

    private byte[] romsetByteArray;

    private IntegerProperty currentBlock;

    private SerialBlockService serialBlockService;

    private ObjectProperty<SampledAudioDataPlayer> rescuePlayer;

    private Stage codeViewerStage;
    private ScrollPane codeViewerPane;
    private CodeViewerController codeViewerController;

    public void onPageLeave() {
        LOGGER.debug("Executing PlayerController onPageLeave");
        if (configuration.getSerialPort() != null) {
            stopSerialBlockService();
        }
    }

    public void onPageEnter() {
        if (configuration.getSerialPort() != null) {
            startSerialBlockService(configuration.getSerialPort());
        }
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private void startSerialBlockService(String serialPort) {
        LOGGER.debug("Initializing Serial Consumer");
        if (serialBlockService != null) {
            stopSerialBlockService();
        }
        serialBlockService = new SerialBlockService(this);
        serialBlockService.setOnDataReceived(this::ledAnimationOnDataReceived);
        serialBlockService.start(serialPort);
    }

    private void stopSerialBlockService() {
        LOGGER.debug("Resetting Serial Consumer");
        if (serialBlockService != null) {
            serialBlockService.stop();
            serialBlockService.close();
        }
    }

    private void ledAnimationOnDataSent() {
        txLed.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(500), txLed);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setCycleCount(1);
        ft.setAutoReverse(false);
        ft.play();
    }

    private void ledAnimationOnDataReceived() {
        rxLed.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(500), rxLed);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setCycleCount(1);
        ft.setAutoReverse(false);
        ft.play();
    }

    public EepromWriterController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        playing = new SimpleBooleanProperty(false);
        rescuePlaying = new SimpleBooleanProperty(false);
        usbRescueSending = new SimpleBooleanProperty(false);
        currentBlock = new SimpleIntegerProperty(-1);
        rescuePlayer = new SimpleObjectProperty<>();
        try {
            rescuePlayer.set(new SampledAudioDataPlayer(applicationContext));
        } catch (Exception e) {
            LOGGER.error("Initializing rescue Player", e);
        }
    }

    private void onEndOfMedia() {
        try {
            txLed.setVisible(false);
            usbRescueSending.set(false);
            playing.set(false);
            unbindDataProducer();
        } catch (Exception e) {
            LOGGER.error("Setting next player", e);
        }
    }

    public void onCommunicationClosed() {
        blockProgress.progressProperty().set(0);
        currentBlock.set(-1);
    }

    private void unbindDataProducer() {
        LOGGER.debug("Unbinding data producer");
        blockProgress.progressProperty().unbind();
        blockProgress.progressProperty().set(0);
    }

    public void bindDataProducer(DataProducer producer) {
        LOGGER.debug("Binding data producer " + producer);
        producer.onDataSent(this::ledAnimationOnDataSent);
        producer.onFinalization(this::onEndOfMedia);
        blockProgress.progressProperty().bind(producer.progressProperty());
        currentBlock.set(producer.id());
        playing.set(true);
    }

    private Future<OperationResult> asyncSend(DataProducer producer) {
        txLed.setVisible(true);
        playing.set(true);
        return applicationContext.addBackgroundTask(() -> {
            try {
                producer.send();
            } catch (Exception e) {
                LOGGER.error("Sending serial data", e);
            }
            return OperationResult.successResult();
        });
    }

    private String getCurrentBlockString() {
        int blockNumber = currentBlock.get();
        int totalBlocks = ROMSET_SIZE / configuration.getBlockSize();
        if (blockNumber >= 0) {
            if (blockNumber < totalBlocks) {
                return String.format("%d/%d", blockNumber + 1, totalBlocks);
            }
        }
        return UNDEFINED_STRING;
    }

    private void sendUsbRescue() {
        try {
            if (!usbRescueSending.get()) {
                usbRescueSending.set(true);
                byte[] data = new byte[Constants.RESCUE_EEWRITER_SIZE];
                byte[] eewriter = Constants.getRescueEewriter();
                LOGGER.debug("Got rescue eewriter of size {}", eewriter.length);
                System.arraycopy(eewriter, 0, data, 0,
                        Math.min(eewriter.length, Constants.RESCUE_EEWRITER_SIZE));
                DataProducer producer = new SerialDataProducer(serialBlockService.serialPort(),
                        Util.reverseByteArray(data));
                bindDataProducer(producer);
                asyncSend(producer);
            } else {
                LOGGER.warn("USB rescue send already in progress");
            }
        } catch (Exception e) {
            LOGGER.error("Preparing Data Producer", e);
        }
    }

    private void onRescuePlayerStart() {
        blockProgress.progressProperty().bind(rescuePlayer.get().progressProperty());
        rescuePlaying.set(true);
        rescueLoaderPlayButton.getStyleClass().removeAll(PLAY_BUTTON_STYLE);
        rescueLoaderPlayButton.getStyleClass().add(STOP_BUTTON_STYLE);
    }

    private void onRescuePlayerStop() {
        blockProgress.progressProperty().unbind();
        blockProgress.progressProperty().set(0);
        rescueLoaderPlayButton.getStyleClass().removeAll(STOP_BUTTON_STYLE);
        rescueLoaderPlayButton.getStyleClass().add(PLAY_BUTTON_STYLE);
        rescuePlaying.set(false);
    }

    private void playRescueLoader() {
        rescuePlayer.get().onFinalization(this::onRescuePlayerStop);
        onRescuePlayerStart();
        rescuePlayer.get().send();
    }

    private void stopRescueLoader() {
        rescuePlayer.get().stop();
        onRescuePlayerStop();
    }

    private CodeViewerController getCodeViewerController() {
        if (codeViewerController == null) {
            codeViewerController = new CodeViewerController();
        }
        return codeViewerController;
    }

    private ScrollPane getCodeViewerPane() {
        try {
            if (codeViewerPane == null) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(MainAppController.class.getResource("codeViewer.fxml"));
                loader.setController(getCodeViewerController());
                loader.setResources(LocaleUtil.getBundle());
                codeViewerPane = loader.load();
            }
            return codeViewerPane;
        } catch (Exception e) {
            LOGGER.error("Creating CodeViewerPane", e);
            throw new RuntimeException(e);
        }
    }

    private Stage getCodeViewerStage() {
        if (codeViewerStage == null) {
            codeViewerStage = new Stage();
            Scene codeViewerScene = new Scene(getCodeViewerPane());
            codeViewerScene.getStylesheets().add(Constants.getThemeResourceUrl());
            codeViewerStage.setScene(codeViewerScene);
            codeViewerStage.setTitle("");
            codeViewerStage.initModality(Modality.APPLICATION_MODAL);
            codeViewerStage.initOwner(blockProgress.getScene().getWindow());
            codeViewerStage.setResizable(true);
        }
        return codeViewerStage;
    }

    @FXML
    void initialize() throws IOException {
        txLed.setVisible(false);
        rxLed.setVisible(false);

        //React to changes in the game list
        applicationContext.getGameList().addListener((InvalidationListener) e -> {
            serialBlockService.resetRomset();
        });

        DandanatorCpcConfiguration.getInstance().extraRomPathProperty()
                .addListener(e -> {
                    if (serialBlockService != null) {
                        serialBlockService.resetRomset();
                    }
                });

        Configuration.getInstance().backgroundImagePathProperty().addListener(e -> {
            if (serialBlockService != null) {
                serialBlockService.resetRomset();
            }
        });

        configuration.customRomSetPathProperty().addListener(e -> {
            if (serialBlockService != null) {
                serialBlockService.resetRomset();
            }
        });

        if (configuration.getSerialPort() != null) {
            if (serialBlockService != null) {
                startSerialBlockService(configuration.getSerialPort());
            }
        }

        overallProgress.progressProperty().bind(Bindings.createDoubleBinding(() ->
                Math.max(0, currentBlock.doubleValue() / (ROMSET_SIZE /
                        configuration.getBlockSize())), currentBlock));

        requestedBlock.textProperty().bind(Bindings
                .createStringBinding(this::getCurrentBlockString, currentBlock));

        configuration.serialPortProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                stopSerialBlockService();
            }
            if (newValue != null) {
                startSerialBlockService(newValue);
            }
        });


        rescueLoaderPlayButton.disableProperty().bind(playing.or(rescuePlayer.isNull()));
        rescueLoaderShowButton.disableProperty().bind(playing);

        rescueLoaderPlayButton.setOnAction(c -> {
            if (rescuePlaying.get()) {
                stopRescueLoader();
            } else {
                playRescueLoader();
            }
        });

        rescueLoaderShowButton.setOnAction(c -> {
            getCodeViewerStage().show();
        });

        usbLoaderSendButton.disableProperty().bind(playing.or(rescuePlaying));
        usbLoaderSendButton.setOnAction(c -> {
           if (!usbRescueSending.get()) {
               sendUsbRescue();
           }
        });
    }

}

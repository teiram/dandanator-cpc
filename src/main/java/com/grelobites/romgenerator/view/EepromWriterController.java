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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

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

    private ObjectProperty<DataProducer> currentDataProducer;

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
        currentDataProducer = new SimpleObjectProperty<>();
        rescuePlayer = new SimpleObjectProperty<>();
        try {
            rescuePlayer.set(new SampledAudioDataPlayer());
        } catch (Exception e) {
            LOGGER.error("Initializing rescue Player", e);
        }
    }


    private Optional<byte[]> getRomsetByteArray() {
        try {
            if (romsetByteArray == null) {
                if (configuration.getCustomRomSetPath() != null) {
                    try (FileInputStream fis = new FileInputStream(configuration.getCustomRomSetPath())) {
                        romsetByteArray = Util.fromInputStream(fis);
                    }
                } else {
                    if (applicationContext.getRomSetHandler().generationAllowedProperty().get()) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        applicationContext.getRomSetHandler().exportRomSet(bos);
                        romsetByteArray = bos.toByteArray();
                    } else {
                        LOGGER.info("Generation of romset byte array currently unavailable");
                    }
                }
            }
            return Optional.ofNullable(romsetByteArray);
        } catch (IOException ioe) {
            LOGGER.error("Trying to get Romset byte array", ioe);
            return Optional.empty();
        }
    }

    private Optional<DataProducer> getBlockDataProducer(int block) {
        int blockSize = configuration.getBlockSize();
        byte[] buffer = new byte[blockSize];
        Optional<byte[]> romsetByteArray = getRomsetByteArray();
        if (romsetByteArray.isPresent()) {
            System.arraycopy(romsetByteArray.get(), block * blockSize, buffer, 0, blockSize);
            return Optional.of(new SerialDataProducer(serialBlockService.serialPort(), block, buffer));
        } else {
            return Optional.empty();
        }
    }

    public void sendCurrentBlock() {
        LOGGER.debug("sendCurrentBlock with block " + currentBlock + " requested");
        if (!playing.get()) {
            Optional<DataProducer> dataProducer = getBlockDataProducer(currentBlock.get());
            if (dataProducer.isPresent()) {
                playing.set(true);
                prepareDataProducer(dataProducer.get());
                send();
            } else {
                LOGGER.warn("Unable to create data producer for current block");
            }
        }
    }

    private void onEndOfMedia() {
        try {
            txLed.setVisible(false);
            usbRescueSending.set(false);
        } catch (Exception e) {
            LOGGER.error("Setting next player", e);
        }
    }

    private void unbindProducer(DataProducer producer) {
        LOGGER.debug("Unbinding producer " + producer);
        blockProgress.progressProperty().unbind();
        blockProgress.progressProperty().set(0);
    }

    private void bindProducer(DataProducer producer) {
        LOGGER.debug("Binding producer " + producer);
        blockProgress.progressProperty().bind(producer.progressProperty());
    }

    private void prepareDataProducer(DataProducer producer) {
        this.currentDataProducer.set(producer);
        producer.onDataSent(this::ledAnimationOnDataSent);
        producer.onFinalization(this::onEndOfMedia);
    }

    private void send() {
        txLed.setVisible(true);
        applicationContext.addBackgroundTask(() -> {
            try {
                currentDataProducer.get().send();
            } catch (Exception e) {
                LOGGER.error("Sending serial data", e);
            }
            return OperationResult.successResult();
        });
        playing.set(true);
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

    private void resetDataProducerAndRomSet() {
        LOGGER.debug("Resetting player and RomSet on invalidating changes");
        romsetByteArray = null;
        currentBlock.set(0);
    }

    public int getCurrentBlock() {
        return currentBlock.get();
    }

    public IntegerProperty currentBlockProperty() {
        return currentBlock;
    }

    public void setCurrentBlock(int currentBlock) {
        this.currentBlock.set(currentBlock);
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
                prepareDataProducer(producer);
                send();
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
            resetDataProducerAndRomSet();
        });

        DandanatorCpcConfiguration.getInstance().extraRomPathProperty()
                .addListener(e -> {
                    resetDataProducerAndRomSet();
                });

        Configuration.getInstance().backgroundImagePathProperty().addListener(e -> {
            resetDataProducerAndRomSet();
        });

        configuration.customRomSetPathProperty().addListener(e -> {
            resetDataProducerAndRomSet();
        });

        if (configuration.getSerialPort() != null) {
            startSerialBlockService(configuration.getSerialPort());
        }

        overallProgress.progressProperty().bind(Bindings.createDoubleBinding(() ->
                Math.max(0, currentBlock.doubleValue() / (ROMSET_SIZE /
                        configuration.getBlockSize())), currentBlock));

        requestedBlock.textProperty().bind(Bindings
                .createStringBinding(this::getCurrentBlockString, currentBlock));

        currentDataProducer.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                unbindProducer(oldValue);
            }
            if (newValue != null) {
                bindProducer(newValue);
            }
        });

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

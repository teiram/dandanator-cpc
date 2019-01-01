package com.grelobites.romgenerator.handlers.dandanatorcpc.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.MainApp;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.RomSetUtil;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.util.winape.view.WinApePokesController;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.PokeEntityTreeCell;
import com.grelobites.romgenerator.view.util.RecursiveTreeItem;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

public class DandanatorCpcFrameController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorCpcFrameController.class);

    private static final String BLUE_BAR_STYLE = "blue-bar";
    private static final String RED_BAR_STYLE = "red-bar";

    private static final String HW_MODE_SUPPORTED = "white-text";
    private static final String HW_MODE_UNSUPPORTED = "red-text";

    @FXML
    private TreeView<PokeViewable> pokeView;

    @FXML
    private Button addPokeButton;

    @FXML
    private Button removeSelectedPokeButton;

    @FXML
    private Button removeAllGamePokesButton;

    @FXML
    private Button importPokesButton;

    @FXML
    private ProgressBar pokesCurrentSizeBar;

    private Tooltip pokeUsageDetail;

    @FXML
    private TabPane gameInfoTabPane;

    @FXML
    private Tab gameInfoTab;

    @FXML
    private Tab pokesTab;

    @FXML
    private AnchorPane gameInfoPane;

    @FXML
    private TextField gameName;

    @FXML
    private Label gameType;

    @FXML
    private Label gameHardware;

    @FXML
    private CheckBox gameHoldScreenAttribute;

    @FXML
    private CheckBox gameCompressedAttribute;

    @FXML
    private Label compressedSize;

    @FXML
    private ProgressBar romUsage;

    private Stage winApePokesStage;

    private Pane winApePokesPane;

    private ApplicationContext applicationContext;

    private InvalidationListener currentGameCompressedChangeListener;

    private InvalidationListener getCurrentGameCompressedChangeListener() {
        return currentGameCompressedChangeListener;
    }

    private void setCurrentGameCompressedChangeListener(InvalidationListener currentGameCompressedChangeListener) {
        this.currentGameCompressedChangeListener = currentGameCompressedChangeListener;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private Pane getWinApePokesPane() throws IOException {
        if (winApePokesPane == null) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(DandanatorCpcFrameController.class
                    .getResource("winapepokes.fxml"));
            loader.setResources(LocaleUtil.getBundle());
            loader.setController(new WinApePokesController(applicationContext));
            winApePokesPane = loader.load();
        }
        return winApePokesPane;
    }

    private Stage getWinApePokesStage() throws IOException {
        if (winApePokesStage == null) {
            winApePokesStage = new Stage();
            Scene winApePokesScene = new Scene(getWinApePokesPane());
            winApePokesScene.getStylesheets().add(Constants.getThemeResourceUrl());
            winApePokesStage.setScene(winApePokesScene);
            winApePokesStage.setTitle("Poke Import Tool");
            //winApePokesStage.initModality(Modality.APPLICATION_MODAL);
            winApePokesStage.initOwner(importPokesButton.getScene().getWindow());
            winApePokesStage.setResizable(true);
        }
        return winApePokesStage;
    }

    @FXML
    private void initialize() {
        LOGGER.debug("Initializing DandanatorCpcFrameController");
        romUsage.progressProperty().bind(applicationContext.romUsageProperty());
        Tooltip romUsageDetail = new Tooltip();
        romUsage.setTooltip(romUsageDetail);
        romUsageDetail.textProperty().bind(applicationContext.romUsageDetailProperty());
        romUsage.progressProperty().addListener(
                (observable, oldValue, newValue) -> {
                    LOGGER.debug("Changing bar style on romUsage change to " + newValue.doubleValue());
                    romUsage.getStyleClass().removeAll(BLUE_BAR_STYLE, RED_BAR_STYLE);
                    romUsage.getStyleClass().add(
                            (newValue.doubleValue() > 1.0 ||
                                    applicationContext.getGameList().size() > DandanatorCpcConstants.MAX_GAMES) ?
                                    RED_BAR_STYLE : BLUE_BAR_STYLE);

                });

        gameInfoTabPane.setVisible(false);

        pokeView.setDisable(false);
        pokeView.setEditable(true);
        pokeView.setCellFactory(p -> {
            TreeCell<PokeViewable> cell = new PokeEntityTreeCell();
            cell.setOnMouseClicked(e -> {
                if (cell.isEmpty()) {
                    pokeView.getSelectionModel().clearSelection();
                }
            });
            return cell;
        });

        pokeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        removeSelectedPokeButton.setDisable(false);
                    } else {
                        removeSelectedPokeButton.setDisable(true);
                    }
                });


        addPokeButton.setOnAction(c -> {
            if (pokeView.getSelectionModel().getSelectedItem() != null) {
                pokeView.getSelectionModel().getSelectedItem().getValue()
                        .addNewChild();
            } else {
                pokeView.getRoot().getValue().addNewChild();
            }
        });

        removeSelectedPokeButton.setOnAction(c -> {
            if (pokeView.getSelectionModel().getSelectedItem() != null) {
                TreeItem<PokeViewable> selected = pokeView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    int selectedIndex = pokeView.getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0) {
                        pokeView.getSelectionModel().select(selectedIndex - 1);
                    } else {
                        pokeView.getSelectionModel().select(pokeView.getRoot());
                    }
                    selected.getValue().getParent().removeChild(selected.getValue());
                }
            }
        });

        removeAllGamePokesButton.setOnAction(c -> {
            Game game = applicationContext.selectedGameProperty().get();
            if (game != null) {
                Optional<ButtonType> result = DialogUtil
                        .buildAlert(LocaleUtil.i18n("pokeSetDeletionConfirmTitle"),
                                LocaleUtil.i18n("pokeSetDeletionConfirmHeader"),
                                LocaleUtil.i18n("pokeSetDeletionConfirmContent"))
                        .showAndWait();

                if (result.orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    ((SnapshotGame) game).getTrainerList().getChildren().clear();
                }
            }
        });

        importPokesButton.setOnAction(c -> {
            try {
                getWinApePokesStage().show();
            } catch (Exception e) {
                LOGGER.error("Trying to show WinApePokes importer", e);
            }
        });

        pokeView.setOnDragOver(event -> {
            if (event.getGestureSource() != pokeView &&
                    event.getDragboard().hasFiles() &&
                    event.getDragboard().getFiles().size() == 1) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        pokeView.setOnDragEntered(Event::consume);

        pokeView.setOnDragExited(Event::consume);

        pokeView.setOnDragDropped(event -> {
            LOGGER.debug("onDragDropped");
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && db.getFiles().size() == 1) {
                try {
                    Game game = applicationContext.selectedGameProperty().get();
                    ImportContext ctx = new ImportContext(db.getFiles().get(0));
                    GameUtil.importPokesFromFile((SnapshotGame) game, ctx);
                    if (ctx.hasErrors()) {
                        LOGGER.debug("Detected errors in pokes import operation");
                        DialogUtil.buildWarningAlert(LocaleUtil.i18n("importPokesWarning"),
                                LocaleUtil.i18n("importPokesWarningHeader"),
                                ctx.getImportErrors().stream()
                                        .distinct()
                                        .collect(Collectors.joining("\n"))).showAndWait();
                    }
                    success = true;
                } catch (IOException ioe) {
                    LOGGER.warn("Adding poke files", ioe);
                }
            }
            /* let the source know whether the files were successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });

        pokeUsageDetail = new Tooltip();
        pokesCurrentSizeBar.setTooltip(pokeUsageDetail);
        applicationContext.getGameList().addListener((InvalidationListener) c -> {
            double pokeUsage = GameUtil.getOverallPokeUsage(applicationContext.getGameList());
            pokesCurrentSizeBar.setProgress(pokeUsage);
            String pokeUsageDetailString = String.format(LocaleUtil.i18n("pokeUsageDetail"),
                    pokeUsage * 100,
                    DandanatorCpcConstants.POKE_ZONE_SIZE);
            pokeUsageDetail.setText(pokeUsageDetailString);
        });

        gameName.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        if (!RomSetUtil.isValidGameName(newValue)) {
                            LOGGER.debug("Resetting name from {} -> {}", oldValue, newValue);
                            gameName.textProperty().setValue(oldValue);
                        }
                    }
                });

        applicationContext.selectedGameProperty().addListener(
                (observable, oldValue, newValue) -> onGameSelection(oldValue, newValue));
        onGameSelection(applicationContext.selectedGameProperty().get(),
                applicationContext.selectedGameProperty().get());

    }

    private void unbindInfoPropertiesFromGame(Game game) {
        if (game != null) {
            LOGGER.debug("Unbinding bidirectionally name property from game " + game);
            gameName.textProperty().unbindBidirectional(game.nameProperty());
            compressedSize.textProperty().unbind();

            if (game instanceof SnapshotGame) {
                SnapshotGame snapshotGame = (SnapshotGame) game;
                gameHoldScreenAttribute.selectedProperty().unbindBidirectional(snapshotGame.holdScreenProperty());
                gameCompressedAttribute.selectedProperty().unbindBidirectional(snapshotGame.compressedProperty());
                pokeView.setRoot(null);
                gameCompressedAttribute.selectedProperty().removeListener(getCurrentGameCompressedChangeListener());
            }
        }
    }

    private static HardwareMode getGameHardwareMode(Game game) {
        return game instanceof SnapshotGame ?
                ((SnapshotGame) game).getHardwareMode() : HardwareMode.HW_UNKNOWN;
    }
    private void bindInfoPropertiesToGame(Game game) {
        if (game != null) {
            gameName.textProperty().bindBidirectional(game.nameProperty());
            gameType.textProperty().set(game.getType().screenName());
            gameHardware.textProperty().set(LocaleUtil.i18n(
                    getGameHardwareMode(game).displayName()));
            compressedSize.textProperty().bind(getGameSizeProperty(game).asString());
            if (game instanceof SnapshotGame) {
                SnapshotGame snapshotGame = (SnapshotGame) game;
                gameHoldScreenAttribute.selectedProperty().bindBidirectional(snapshotGame.holdScreenProperty());

                pokeView.setRoot(new RecursiveTreeItem<>(snapshotGame.getTrainerList(), PokeViewable::getChildren,
                        this::computePokeChange));
                gameCompressedAttribute.selectedProperty().bindBidirectional(snapshotGame.compressedProperty());
                setCurrentGameCompressedChangeListener((c) -> {
                    compressedSize.textProperty().unbind();
                    compressedSize.textProperty().bind(getGameSizeProperty(game).asString());
                });
                snapshotGame.compressedProperty().addListener(getCurrentGameCompressedChangeListener());
            }
        }
    }

    private void computePokeChange(PokeViewable f) {
        LOGGER.debug("New poke ocupation is " + GameUtil.getOverallPokeUsage(applicationContext.getGameList()));
        double pokeUsage = GameUtil.getOverallPokeUsage(applicationContext.getGameList());
        pokesCurrentSizeBar.setProgress(pokeUsage);
        String pokeUsageDetailString = String.format(LocaleUtil.i18n("pokeUsageDetail"),
                pokeUsage * 100,
                DandanatorCpcConstants.POKE_ZONE_SIZE);
        pokeUsageDetail.setText(pokeUsageDetailString);
        if (applicationContext.selectedGameProperty().get() == f.getOwner()) {
            removeAllGamePokesButton.setDisable(!GameUtil.gameHasPokes(f.getOwner()));
        }
    }

    private void onGameSelection(Game oldGame, Game newGame) {
        LOGGER.debug("onGameSelection oldGame=" + oldGame + ", newGame=" + newGame);
        unbindInfoPropertiesFromGame(oldGame);
        bindInfoPropertiesToGame(newGame);
        if (newGame == null) {
            addPokeButton.setDisable(true);
            removeAllGamePokesButton.setDisable(true);
            removeSelectedPokeButton.setDisable(true);
            gameInfoTabPane.setVisible(false);
        } else {
            if (newGame instanceof RamGame) {
                RamGame ramGame = (RamGame) newGame;
                if (newGame instanceof SnapshotGame) {
                    SnapshotGame snapshotGame = (SnapshotGame) newGame;
                    addPokeButton.setDisable(false);
                    pokesTab.setDisable(false);
                    gameHoldScreenAttribute.setVisible(true);
                    gameCompressedAttribute.setVisible(true);
                    if (snapshotGame.getTrainerList().getChildren().size() > 0) {
                        removeAllGamePokesButton.setDisable(false);
                    } else {
                        removeAllGamePokesButton.setDisable(true);
                    }
                } else {
                    pokesTab.setDisable(true);
                    gameHoldScreenAttribute.setVisible(false);
                    gameCompressedAttribute.setVisible(false);
                }
            } else {
                pokesTab.setDisable(true);
                gameHoldScreenAttribute.setVisible(false);
                gameCompressedAttribute.setVisible(false);
            }
            gameInfoTabPane.setVisible(true);
        }
    }


    private IntegerProperty getGameSizeProperty(Game game) {
        try {
            if (game instanceof SnapshotGame) {
                SnapshotGame snapshotGame = (SnapshotGame) game;
                if (snapshotGame.getCompressed()) {
                    return snapshotGame.compressedSizeProperty();
                }
            }
            return new SimpleIntegerProperty(game.getSize());
        } catch (Exception e) {
            LOGGER.error("Calculating game compressed size", e);
        }
        return null;
    }

}

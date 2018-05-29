package com.grelobites.romgenerator.handlers.dandanatorcpc.view;

import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.PokeEntityTreeCell;
import com.grelobites.romgenerator.view.util.RecursiveTreeItem;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

public class DandanatorMiniFrameController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniFrameController.class);

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
    private CheckBox gameHoldScreenAttribute;

    @FXML
    private CheckBox gameCompressedAttribute;

    @FXML
    private Label compressedSize;

    @FXML
    private ProgressBar romUsage;

    @FXML
    private Label hardwareMode;

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

    @FXML
    private void initialize() {
        LOGGER.debug("Initializing DandanatorMiniFrameController");
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

    private void bindInfoPropertiesToGame(Game game) {
        if (game != null) {
            gameName.textProperty().bindBidirectional(game.nameProperty());
            gameType.textProperty().set(game.getType().screenName());
            compressedSize.textProperty().bind(getGameSizeProperty(game).asString());
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                LOGGER.debug("Binding hardware mode to " + ramGame.getHardwareMode().displayName());
                hardwareMode.textProperty().set(ramGame.getHardwareMode().displayName());
            }
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

    private void styleHardwareMode(RamGame game) {
        hardwareMode.getStyleClass().removeAll(
                game.getHardwareMode().supported() ? HW_MODE_UNSUPPORTED : HW_MODE_SUPPORTED);
        hardwareMode.getStyleClass().add(game.getHardwareMode().supported() ?
            HW_MODE_SUPPORTED : HW_MODE_UNSUPPORTED);
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
                hardwareMode.setVisible(true);
                styleHardwareMode(ramGame);

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
                hardwareMode.setVisible(false);
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

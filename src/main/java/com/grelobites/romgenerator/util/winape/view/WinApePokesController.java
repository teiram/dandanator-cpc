package com.grelobites.romgenerator.util.winape.view;


import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.winape.WinApePokeFormatFactory;
import com.grelobites.romgenerator.util.winape.model.WinApeGame;
import com.grelobites.romgenerator.util.winape.model.WinApePoke;
import com.grelobites.romgenerator.util.winape.model.WinApePokeDatabase;
import com.grelobites.romgenerator.util.winape.model.WinApePokeFormat;
import com.grelobites.romgenerator.util.winape.model.WinApePokeValue;
import com.grelobites.romgenerator.util.winape.model.WinApeTrainer;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class WinApePokesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApePokesController.class);

    @FXML
    private TableView<WinApeGame> gameTable;

    @FXML
    private TableView<WinApeTrainer> trainerTable;

    @FXML
    private TableView<WinApePoke> pokeTable;

    @FXML
    private TextArea searchBox;

    @FXML
    private Button importButton;

    @FXML
    private TableColumn<WinApeGame, String> gameNameColumn;

    @FXML
    private TableColumn<WinApeGame, Shape> matchGameColumn;

    @FXML
    private TableColumn<WinApeTrainer, String> trainerDescriptionColumn;

    @FXML
    private TableColumn<WinApeTrainer, Shape> trainerStatusColumn;

    @FXML
    private TableColumn<WinApePoke, Shape> pokeStatusColumn;

    @FXML
    private TableColumn<WinApePoke, String> pokeAddressColumn;

    @FXML
    private TableColumn<WinApePoke, String> pokeValueColumn;

    @FXML
    private Label selectedPokeComment;

    @FXML
    private Label selectedPokeSize;

    private ObservableList<WinApeGame> games = FXCollections.observableArrayList();
    private ObservableList<WinApeTrainer> trainers = FXCollections.observableArrayList();
    private ObservableList<WinApePoke> pokes = FXCollections.observableArrayList();

    private WinApePokeDatabase database;
    private ApplicationContext applicationContext;

    private static Shape getOKStatusShape() {
        return new Circle(5, Color.GREEN);
    }

    private static Shape getErrorStatusShape() {
        return new Circle(5, Color.RED);
    }

    private static List<Pair<Integer, Integer>> pokePairs(WinApeTrainer trainer) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        trainer.getPokes().forEach(p -> {
            int address = p.getAddress();
            for (Integer value : p.getValue().values()) {
                result.add(new Pair<>(address++, value));
            }
        });
        return result;
    }

    private boolean trainerDisableConditions(WinApeTrainer trainer) {
        return trainer == null ||
                !trainer.exportable() ||
                pokePairs(trainer).size() > Trainer.MAX_POKES_PER_TRAINER;
    }

    private boolean selectedGameDisableConditions() {
        Game selectedGame = applicationContext.selectedGameProperty().get();
        if (selectedGame instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) selectedGame;
            return snapshotGame.getTrainerList().getChildren().size()
                    == TrainerList.MAX_TRAINERS_PER_GAME;
        } else {
            return true;
        }
    }

    private WinApeGame getNearestCatalogGame(String name) {
        return database.search(name);
    }

    private String parsePokeValue(WinApePoke poke) {
        return WinApePokeFormatFactory.forPokeValueType(poke.getTrainer().getValueType())
            .render(poke);
    }

    public WinApePokesController(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        this.database = WinApePokeDatabase.fromInputStream(WinApePokesController.class
                .getResourceAsStream("/winape.pok"));

    }

    private ObservableBooleanValue getDisableButtonObservable() {
        Game selectedGame = applicationContext.selectedGameProperty().get();
        Observable[] observables;
        if (selectedGame instanceof SnapshotGame) {
            observables = new Observable[] {applicationContext.selectedGameProperty(),
                    ((SnapshotGame) selectedGame).getTrainerList().getChildren()};
        } else {
            observables = new Observable[] {applicationContext.selectedGameProperty()};
        }

        return trainerTable.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> {
                    WinApeTrainer trainer = trainerTable.getSelectionModel().selectedItemProperty().getValue();
                    return trainerDisableConditions(trainer);
                }, trainerTable.getSelectionModel().selectedItemProperty()))
                .or(Bindings.createBooleanBinding(this::selectedGameDisableConditions,
                        observables));
    }

    private boolean checkGameId(SnapshotGame selectedGame, WinApeGame candidate) {
        int address = candidate.getIdAddr();
        final byte[] data = candidate.getIdData();
        for (int i = 0; i < candidate.getIdData().length; i++) {
            int slot = selectedGame.getSlotForMappedRam(address);
            int offset = address % Constants.SLOT_SIZE;
            if (data[i] != selectedGame.getSlot(slot)[offset]) {
                return false;
            }
            address++;
        }
        return true;
    }

    private void updateCheckStatus(Game selectedGame) {
        for (WinApeGame winApeGame : games) {
            winApeGame.matchedProperty().set(checkGameId((SnapshotGame) selectedGame, winApeGame));
        }
    }

    @FXML
    private void initialize() throws IOException {
        gameNameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        matchGameColumn.setCellValueFactory(
                cellData -> Bindings.createObjectBinding(
                        () -> cellData.getValue().matchedProperty().get() ?
                                getOKStatusShape() : getErrorStatusShape(),
                        cellData.getValue().matchedProperty())
        );
        trainerDescriptionColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        pokeAddressColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        String.format("0x%04x", cellData.getValue().getAddress())));

        pokeValueColumn.setEditable(true);
        pokeTable.getSelectionModel().cellSelectionEnabledProperty().set(true);

        pokeValueColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        WinApePokeFormatFactory.forPokeValueType(cellData.getValue()
                        .getTrainer().getValueType()).render(cellData.getValue())));

        pokeStatusColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getValue().exportable() ?
                                getOKStatusShape() : getErrorStatusShape())
                );

        trainerStatusColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().exportable() ?
                                getOKStatusShape() : getErrorStatusShape())
        );

        pokeValueColumn.setCellFactory(TextFieldTableCell.forTableColumn(
                new StringConverter<String>() {
                    @Override
                    public String toString(String value) {
                        return value;
                    }

                    @Override
                    public String fromString(String value) {
                        return value;
                    }
                }));

        pokeValueColumn.setOnEditStart(e -> {
           LOGGER.debug("On Edit Start {}", e);
        });

        pokeValueColumn.setOnEditCommit(e -> {
            LOGGER.debug("On Edit Commit {}", e);
            WinApePoke poke = e.getRowValue();
            WinApePokeFormat format = WinApePokeFormatFactory
                    .forPokeValueType(poke.getTrainer().getValueType());
            WinApePokeValue newValue = format.generate(poke, e.getNewValue());
            if (format.validate(poke, newValue)) {
                poke.setValue(newValue);
                LOGGER.debug("Values committed");
            } else {
                LOGGER.debug("Failure committing values");
            }
            e.getTableView().getItems().set(e.getTablePosition().getRow(), poke);
            e.getTableView().getSelectionModel().clearSelection();
            trainerTable.getItems().set(trainerTable.getSelectionModel().getSelectedIndex(),
                    trainerTable.getSelectionModel().getSelectedItem());

            LOGGER.debug("After edit poke is {}", e.getRowValue());
        });

        games.setAll(database.games());
        gameTable.setItems(games);
        trainerTable.setItems(trainers);
        trainerTable.setPlaceholder(new Label("Select a Game"));
        pokeTable.setItems(pokes);
        pokeTable.setPlaceholder(new Label("Select a Trainer"));

        gameTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    LOGGER.debug("New game selected: {}", newValue);
                    if (newValue != null) {
                        trainers.setAll(newValue.getTrainers());
                    } else {
                        trainers.clear();
                    }
                });

        trainerTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    LOGGER.debug("New trainer selected: {}", newValue);
                    if (newValue != null) {
                        pokes.setAll(newValue.getPokes());
                        selectedPokeComment.textProperty().set(
                                newValue.getComment());
                    } else {
                        pokes.clear();
                        selectedPokeComment.textProperty().set("");
                    }
        });

        pokeTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    LOGGER.debug("New poke selected: {}", newValue);
                    if (newValue != null) {
                        selectedPokeSize.textProperty().set(
                                Integer.toString(newValue.getRequiredSize()));
                    } else {
                        selectedPokeSize.textProperty().set("-");
                    }

        });

        gameTable.getSelectionModel().select(getNearestCatalogGame(applicationContext
                .selectedGameProperty().get().getName()));
        gameTable.scrollTo(gameTable.getSelectionModel().getSelectedIndex());

        updateCheckStatus(applicationContext.selectedGameProperty().get());

        applicationContext.selectedGameProperty().addListener((observable, oldValue, newValue) -> {
            importButton.disableProperty().unbind();
            importButton.disableProperty().bind(getDisableButtonObservable());
            if (newValue != null) {
                gameTable.getSelectionModel().select(getNearestCatalogGame(newValue.getName()));
                gameTable.scrollTo(gameTable.getSelectionModel().getSelectedIndex());
                updateCheckStatus(newValue);
            } else {
                gameTable.getSelectionModel().clearSelection();
            }
        });

        importButton.disableProperty().bind(getDisableButtonObservable());

        importButton.setOnAction(e -> {
            WinApeTrainer selectedTrainer = trainerTable.getSelectionModel()
                    .getSelectedItem();
           LOGGER.debug("Running import of trainer {}", selectedTrainer);
           TrainerList list = ((SnapshotGame) applicationContext.selectedGameProperty()
                   .get()).getTrainerList();
           final List<Pair<Integer, Integer>> pokePairs = pokePairs(selectedTrainer);
           if (pokePairs.size() <= Trainer.MAX_POKES_PER_TRAINER) {
               list.addTrainerNode(Util
                       .substring(selectedTrainer.getDescription(),
                               DandanatorCpcConstants.POKE_EFFECTIVE_NAME_SIZE))
                       .ifPresent(t -> {
                   pokePairs.forEach(p -> {
                       t.addPoke(p.getKey(), p.getValue());
                   });
               });
           }
        });
    }
}

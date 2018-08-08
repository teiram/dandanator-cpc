package com.grelobites.romgenerator.pok.view;

import com.grelobites.romgenerator.pok.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PokAppController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokAppController.class);
    @FXML
    private TableView<WinApeGame> gameTable;

    @FXML
    private TableView<WinApeTrainer> trainerTable;

    @FXML
    private TableView<TrainerExporter.PokeContainer> pokeTable;

    @FXML
    private TextArea searchBox;

    @FXML
    private Button importButton;

    @FXML
    private TableColumn<WinApeGame, String> gameName;

    @FXML
    private TableColumn<WinApeTrainer, String> trainerDescription;

    @FXML
    private TableColumn<WinApePoke, String> pokeAddress;

    @FXML
    private TableColumn<TrainerExporter.PokeContainer, String> pokeValues;

    private ObservableList<WinApeGame> games = FXCollections.observableArrayList();
    private ObservableList<WinApeTrainer> trainers = FXCollections.observableArrayList();
    private TrainerExporter exporter = new TrainerExporter();

    private WinApePokeDatabase database;

    private String parsePokeValue(TrainerExporter.PokeContainer poke) {
        int valueCount = poke.getValues().length;
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < valueCount; i++) {
            value.append(poke.getValues()[i] == null ? "?" : poke.getValues()[i]);
            if (i < (valueCount - 1)) {
                value.append(", ");
            }
        }
        return value.toString();
    }

    @FXML
    private void initialize() throws IOException {
        database = WinApePokeDatabase.fromInputStream(PokAppController.class
                .getResourceAsStream("/winape.pok"));

        gameName.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        trainerDescription.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        pokeAddress.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        String.format("0x%04x", cellData.getValue().getAddress())));

        pokeValues.setCellValueFactory(
                cellData -> new SimpleStringProperty(parsePokeValue(cellData.getValue())));

        games.setAll(database.games());
        gameTable.setItems(games);
        trainerTable.setItems(trainers);
        trainerTable.setPlaceholder(new Label("Select a Game"));
        pokeTable.setItems(exporter.getPokes());
        pokeTable.setPlaceholder(new Label("Select a Trainer"));

        gameTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    LOGGER.debug("New game selected: {}", newValue);
                    trainers.setAll(newValue.getTrainers());
                    exporter.bind(null);
                });
        trainerTable.selectionModelProperty().addListener(e -> {
            exporter.bind(null);
        });
        trainerTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    LOGGER.debug("New trainer selected: {}", newValue);
                    if (newValue != null) {
                        exporter.bind(newValue);
                    } else {
                        exporter.bind(null);
                    }
        });
    }

}

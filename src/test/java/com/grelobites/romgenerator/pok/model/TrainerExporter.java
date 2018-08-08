package com.grelobites.romgenerator.pok.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TrainerExporter {

    public class PokeContainer extends WinApePoke {
        public PokeContainer(WinApePoke poke) {
            super(poke);
        }
        public WinApeTrainer trainer() {
            return TrainerExporter.this.trainer.get();
        }
    }
    private ObjectProperty<WinApeTrainer> trainer;
    private ObservableList<PokeContainer> pokes;

    public TrainerExporter() {
        this.trainer = new SimpleObjectProperty<>();
        this.pokes = FXCollections.observableArrayList();
    }

    public void bind(WinApeTrainer trainer) {
        this.trainer.set(trainer);
        this.pokes.clear();
        if (trainer != null) {
            for (WinApePoke poke : trainer.getPokes()) {
                pokes.add(new PokeContainer(poke));
            }
        }
    }

    public ObservableList<PokeContainer> getPokes() {
        return pokes;
    }

    public WinApeTrainer getTrainer() {
        return trainer.get();
    }

    public ObjectProperty<WinApeTrainer> trainerProperty() {
        return trainer;
    }
}

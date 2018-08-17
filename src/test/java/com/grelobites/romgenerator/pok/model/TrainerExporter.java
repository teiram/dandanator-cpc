package com.grelobites.romgenerator.pok.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.List;

public class TrainerExporter {

    private ObjectProperty<WinApeTrainer> trainer;
    private ObservableList<WinApePoke> pokes;

    public TrainerExporter() {
        this.trainer = new SimpleObjectProperty<>();
        pokes = FXCollections.observableArrayList();
    }

    public void bind(WinApeTrainer trainer) {
        this.trainer.set(trainer);
        if (trainer != null) {
            pokes.setAll(trainer.getPokes());
        } else {
            pokes.clear();
        }
    }

    public WinApeTrainer getTrainer() {
        return trainer.get();
    }

    public ObservableList<WinApePoke> getPokes() {
        return pokes;
    }

    public ObjectProperty<WinApeTrainer> trainerProperty() {
        return trainer;
    }
}

package com.grelobites.romgenerator.util.winape.model;

public interface WinApePokeFormat {
    String render(WinApePoke poke);
    boolean validate(WinApePoke poke, WinApePokeValue value);
    WinApePokeValue generate(WinApePoke poke, String value);
}

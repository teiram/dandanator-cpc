package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.VirtualGame;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.PreferencesProvider;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;

public class DandanatorCpcConstants {
    public static final int SLOT_COUNT = 10;
    public static final int MAX_GAMES = 25;
    public static final int EEPROM_SLOTS = 32;
    public static final int GAME_SLOTS = EEPROM_SLOTS - 2; //Total ROM Size is 32 slots (minus 1 slot (code) and last (test rom))
    public static final int POKE_HEADER_SIZE = 3 * SLOT_COUNT;
    public static final int GAMENAME_SIZE = 41;
    public static final int GAMENAME_EFFECTIVE_SIZE = GAMENAME_SIZE - 4;
    public static final int POKE_ENTRY_SIZE = 3;
    public static final int POKE_NAME_SIZE = 24;
    public static final int POKE_EFFECTIVE_NAME_SIZE = 20;
    public static final int POKE_ZONE_SIZE = 3200;
    public static final int MAX_POKES_PER_TRAINER = 6;
    public static final int MAX_TRAINERS_PER_GAME = 8;
    public static final String DEFAULT_EXTRAROMKEY_MESSAGE = LocaleUtil.i18n("extraRomDefaultMessage");
    public static final String DEFAULT_TOGGLEPOKESKEY_MESSAGE = LocaleUtil.i18n("togglePokesDefaultMessage");
    public static final String DEFAULT_LAUNCHGAME_MESSAGE = LocaleUtil.i18n("launchGameDefaultMessage");
    public static final String DEFAULT_SELECTPOKE_MESSAGE = LocaleUtil.i18n("selectPokesDefaultMessage");
    public static final int TOGGLE_POKES_MESSAGE_MAXLENGTH = 41;
    public static final int EXTRA_ROM_MESSAGE_MAXLENGTH = 41;
    public static final int LAUNCH_GAME_MESSAGE_MAXLENGTH = 41;
    public static final int SELECT_POKE_MESSAGE_MAXLENGTH = 41;
    private static final String DANDANATOR_ROM_RESOURCE = "dandanator-cpc/dandanator-mini.rom";
    private static final String EXTRA_ROM_RESOURCE = "dandanator-cpc/test.rom";

    public static final int POKE_TARGET_ADDRESS = 49284;
    public static final int GAME_CHUNK_SIZE = 32;
    public static final int GAME_CHUNK_SLOT = 2;
    public static final int VERSION_SIZE = 8;
    public static final int FILLER_BYTE = 0xFF;
    public static final int EXTENDED_CHARSET_SIZE = 896;
    public static final int BASEROM_SIZE = 3584;

    private static byte[] DANDANATOR_ROM;
    private static byte[] EXTRA_ROM;

    private static PreferencesProvider providerRegister = new PreferencesProvider("Dandanator CPC",
            "/com/grelobites/romgenerator/handlers/dandanatorcpc/view/dandanatorcpcpreferences.fxml",
            PreferencesProvider.PRECEDENCE_HANDLERS);

    public static byte[] getDandanatorRom() throws IOException {
        if (DANDANATOR_ROM == null) {
            DANDANATOR_ROM = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(DANDANATOR_ROM_RESOURCE),
                    BASEROM_SIZE);
        }
        return DANDANATOR_ROM;
    }

    public static byte[] getExtraRom() throws IOException {
        if (EXTRA_ROM == null) {
            EXTRA_ROM = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(EXTRA_ROM_RESOURCE),
                    Constants.SLOT_SIZE);
        }
        return EXTRA_ROM;
    }

}

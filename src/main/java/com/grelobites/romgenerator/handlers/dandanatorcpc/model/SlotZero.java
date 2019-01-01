package com.grelobites.romgenerator.handlers.dandanatorcpc.model;

import com.grelobites.romgenerator.handlers.dandanatorcpc.v1.SlotZeroV1;
import com.grelobites.romgenerator.handlers.dandanatorcpc.v2.SlotZeroV2;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;


public interface SlotZero {
    Logger LOGGER = LoggerFactory.getLogger(SlotZero.class);
    Class<?>[] implementations =
            new Class<?>[] {
                    SlotZeroV1.class,
                    SlotZeroV2.class
    };

    static Optional<SlotZero> getImplementation(byte[] data) {
        for (Class<?> implementationClass : implementations) {
            try {
                Class<? extends SlotZero> slotZeroClass = implementationClass.asSubclass(SlotZero.class);
                Constructor<? extends SlotZero> constructor = slotZeroClass.getConstructor(byte[].class);
                SlotZero slotZeroInstance = constructor.newInstance((Object) data);
                if (slotZeroInstance.validate()) {
                    return Optional.of(slotZeroInstance);
                }
            } catch (Exception e) {
                LOGGER.error("Instantiating class " + implementationClass);
            }
        }
        return Optional.empty();
    }

    boolean validate();

    void parse() throws IOException;

    byte[] getCharSet();

    byte[] getScreen();

    byte[] getScreenPalette();

    void populateGameSlots(PositionAwareInputStream is) throws IOException;

    List<? extends GameMapper> getGameMappers();

    DandanatorCpcImporter getImporter();

    InputStream data();

    String getExtraRomMessage();

    String getTogglePokesMessage();

    String getLaunchGameMessage();

    String getSelectPokesMessage();

    boolean getDisableBorderEffect();

    default boolean getAutoboot() {
        return false;
    }

    default boolean getExtraRomPresent() { return true; }

    default boolean getEnforceFollowRom() { return false; }
}

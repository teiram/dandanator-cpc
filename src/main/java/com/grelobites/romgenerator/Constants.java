package com.grelobites.romgenerator;

import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.PreferencesProvider;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;

public class Constants {

    public static final String ROMSET_PROVIDED = "__ROMSET_PROVIDED__";

    public static final int SLOT_SIZE = 0x4000;

    private static final String DEFAULT_VERSION = "1.0";

    public static final int CHARSET_SIZE = 768;

	public static final int CPC_SCREEN_WIDTH = 640;
	public static final int CPC_SCREEN_HEIGHT = 400;
	public static final int CPC_SCREEN_SIZE = 16384;
	public static final int CPC_PALETTE_SIZE = 17;
	public static final int ICONS_SIZE = 256;
    public static final int CPC_SCREEN_WITH_PALETTE_SIZE = CPC_SCREEN_SIZE +
            CPC_PALETTE_SIZE;

    private static final String DEFAULT_MENU_SCREEN_RESOURCE = "menu.scr";
    private static final String SINCLAIR_SCREEN_RESOURCE = "cpc6128.scr";
    private static final String DEFAULT_CHARSET_RESOURCE = "charset.rom";
    private static final String ICONS_RESOURCE = "icons.bin";
    private static final String THEME_RESOURCE = "view/theme.css";

    public static final byte[] ZEROED_SLOT = new byte[SLOT_SIZE];

    public static final byte B_01 = 1;
	public static final byte B_00 = 0;
    public static final byte B_FF = -1;
    public static final int MENU_SCREEN_MODE = 0;


    private static byte[] DEFAULT_DANDANATOR_SCREEN;
    private static byte[] SINCLAIR_SCREEN;
    private static byte[] DEFAULT_CHARSET;
    private static byte[] ICONS;

    private static String THEME_RESOURCE_URL;

    //This is just to register a preferences provider
    private static PreferencesProvider providerRegister = new PreferencesProvider(
            LocaleUtil.i18n("preferencesGeneralTab"),
            "/com/grelobites/romgenerator/view/preferences.fxml", PreferencesProvider.PRECEDENCE_GLOBAL);

    private static PreferencesProvider eepromwriterPreferences = new PreferencesProvider(
            LocaleUtil.i18n("loader"),
            "/com/grelobites/romgenerator/view/eepromwriterconfiguration.fxml",
            PreferencesProvider.PRECEDENCE_OTHER);

    public static String currentVersion() {
        String version = Constants.class.getPackage()
                .getImplementationVersion();
        if (version == null) {
            version = DEFAULT_VERSION;
        }
        return version;
    }

    public static byte[] getDefaultMenuScreen() throws IOException {
        if (DEFAULT_DANDANATOR_SCREEN == null) {
            DEFAULT_DANDANATOR_SCREEN = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_MENU_SCREEN_RESOURCE),
                    Constants.CPC_SCREEN_WITH_PALETTE_SIZE);
        }
        return DEFAULT_DANDANATOR_SCREEN;
    }

    public static byte[] getSinclairScreen() throws IOException {
        if (SINCLAIR_SCREEN == null) {
            SINCLAIR_SCREEN = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(SINCLAIR_SCREEN_RESOURCE),
                    Constants.CPC_SCREEN_WITH_PALETTE_SIZE);
        }
        return SINCLAIR_SCREEN;
    }

    public static InputStream getScreenFromResource(String resource) throws IOException {
        return DandanatorCpcConstants.class.getClassLoader()
                .getResourceAsStream(resource);
    }

    public static byte[] getDefaultCharset() throws IOException {
        if (DEFAULT_CHARSET == null) {
            DEFAULT_CHARSET = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_CHARSET_RESOURCE),
                    CHARSET_SIZE);
        }
        return DEFAULT_CHARSET;
    }

    public static byte[] getIcons() throws IOException {
        if (ICONS == null) {
            ICONS = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(ICONS_RESOURCE),
                    ICONS_SIZE);
        }
        return ICONS;
    }


    public static String getThemeResourceUrl() {
        if (THEME_RESOURCE_URL == null) {
            THEME_RESOURCE_URL = Constants.class.getResource(THEME_RESOURCE)
                    .toExternalForm();
        }
        return THEME_RESOURCE_URL;
    }

}

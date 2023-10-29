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

    private static final String DEFAULT_VERSION = "2.6.1";

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

    public static final int RESCUE_LOADER_ADDRESS = 0xA000;
    public static final int RESCUE_EEWRITER_SIZE = 0x1005;
    public static final String RESCUE_LOADER_NAME = "Rescue Loader";
    private static final String RESCUE_LOADER_RESOURCE = "eewriter/rescue-loader.bin";
    private static final String RESCUE_EEWRITER_RESOURCE = "eewriter/rescue-eewriter.bin";

    public static final byte[] ZEROED_SLOT = new byte[SLOT_SIZE];

    private static final String USB_LAUNCHCODE_HEADER_RESOURCE = "dandanator-cpc/usb-launchcode-header.bin";


    public static final String MOJONOS_V1 = "f265a4f066bfcf01183e37332d71f1a4";
    public static final String MOJONOS_SCR_RESOURCE = "images/mojonos.scr";

    public static final String R4MHZ_V1 = "aa120d9043fc3aaa70b4994abf803403";
    public static final String R4MHZ_SCR_RESOURCE = "images/4mhz.scr";

    public static final String[][] KNOWN_ROMS = {
            {MOJONOS_V1, MOJONOS_SCR_RESOURCE},
            {R4MHZ_V1, R4MHZ_SCR_RESOURCE}
    };

    public static final byte B_01 = 1;
	public static final byte B_00 = 0;
    public static final byte B_FF = -1;
    public static final int MENU_SCREEN_MODE = 0;


    private static byte[] DEFAULT_DANDANATOR_SCREEN;
    private static byte[] SINCLAIR_SCREEN;
    private static byte[] DEFAULT_CHARSET;
    private static byte[] ICONS;
    private static byte[] RESCUE_LOADER;
    private static byte[] RESCUE_EEWRITER;
    private static byte[] USB_LAUNCHCODE_HEADER;


    private static String THEME_RESOURCE_URL;

    //This is just to register a preferences provider
    private static PreferencesProvider providerRegister = new PreferencesProvider(
            LocaleUtil.i18n("preferencesGeneralTab"),
            "/com/grelobites/romgenerator/view/preferences.fxml", PreferencesProvider.PRECEDENCE_GLOBAL);

    private static PreferencesProvider eepromwriterPreferences = new PreferencesProvider(
            LocaleUtil.i18n("loader"),
            "/com/grelobites/romgenerator/view/eepromwriterconfiguration.fxml",
            PreferencesProvider.PRECEDENCE_OTHER);

    /*
    private static PreferencesProvider emulatorPreferences = new PreferencesProvider(
            "Emulador",
            "/com/grelobites/romgenerator/view/emulatorConfiguration.fxml",
            PreferencesProvider.PRECEDENCE_OTHER);
    */

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

    public static byte[] getRescueLoader() throws IOException {
        if (RESCUE_LOADER == null) {
            RESCUE_LOADER = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(RESCUE_LOADER_RESOURCE));
        }
        return RESCUE_LOADER;
    }

    public static byte[] getUsbLaunchcodeHeader() throws IOException {
        if (USB_LAUNCHCODE_HEADER == null) {
            USB_LAUNCHCODE_HEADER = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(USB_LAUNCHCODE_HEADER_RESOURCE));
        }
        return USB_LAUNCHCODE_HEADER;
    }

    public static byte[] getRescueEewriter() throws IOException {
        if (RESCUE_EEWRITER == null) {
            RESCUE_EEWRITER = Util.fromInputStream(
                    DandanatorCpcConstants.class.getClassLoader()
                            .getResourceAsStream(RESCUE_EEWRITER_RESOURCE));
        }
        return RESCUE_EEWRITER;
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

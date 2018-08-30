package com.grelobites.romgenerator.util.gamerenderer.renderers;


import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.gamerenderer.GameRenderer;
import com.grelobites.romgenerator.util.gamerenderer.PassiveGameRendererBase;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ScreenshotGameRenderer extends PassiveGameRendererBase implements GameRenderer  {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotGameRenderer.class);
    private WritableImage defaultGameImage;
    private WritableImage romGameImage;
    private ImageView targetView;

    private void initializeImages() throws IOException {
        setDefaultImage(Configuration.getInstance().getTapeLoaderTarget(),false);
        romGameImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(), 1,
                ScreenshotGameRenderer.class.getClassLoader()
                        .getResourceAsStream("cpc6128.scr"));
    }

    private void loadDefaultImage(String imageResource) {
        LOGGER.debug("Loading default image " + imageResource);
        try {
            defaultGameImage = ImageUtil.scrLoader(
                    ImageUtil.newScreenshot(), 1,
                    ScreenshotGameRenderer.class.getClassLoader()
                            .getResourceAsStream(imageResource));
        } catch (IOException ioe) {
            LOGGER.error("Loading screenshot image resource", ioe);
        }
    }

    private void setDefaultImage(String tapeLoaderTarget, boolean update) {
        switch (HardwareMode.valueOf(tapeLoaderTarget)) {
            case HW_CPC464:
                loadDefaultImage("cpc464.scr");
                break;
            case HW_CPC6128:
                loadDefaultImage("cpc6128.scr");
                break;
            default:
                loadDefaultImage("cpc6128.scr");
        }
        if (update) {
            targetView.setImage(defaultGameImage);
        }
    }

    public ScreenshotGameRenderer() throws IOException {
        initializeImages();
        Configuration.getInstance().tapeLoaderTargetProperty()
                .addListener((observable, oldValue, newValue) ->
                        setDefaultImage(newValue,
                                targetView.getImage() == defaultGameImage));
    }

    @Override
    public void setTarget(ImageView imageView) {
        this.targetView = imageView;
        previewGame(null);
    }

    @Override
    public void previewGame(Game game) {
        if (game != null) {
            if (game instanceof RamGame) {
                targetView.setImage(((RamGame) game).getScreenshot());
            } else if (game.getType() == GameType.ROM) {
                targetView.setImage(romGameImage);
            } else {
                targetView.setImage(defaultGameImage);
            }
        } else {
            targetView.setImage(defaultGameImage);
        }
    }

    @Override
    public void loadImage(InputStream resource) throws IOException {
        targetView.setImage(ImageUtil.scrLoader(
                ImageUtil.newScreenshot(), 1, resource));
    }
}

package io.github.marcuzapl.coregnition.backend.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextureColorClassifierTest {
    private TextureColorClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new TextureColorClassifier();
    }

    @Test
    void classifiesDarkImageAsCarbonaceousShale() {
        BufferedImage darkImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = darkImage.createGraphics();
        g.setColor(new Color(35, 38, 40));
        g.fillRect(0, 0, 100, 100);
        // Add subtle horizontal laminae
        g.setColor(new Color(50, 52, 55));
        for (int y = 0; y < 100; y += 4) {
            g.drawLine(0, y, 100, y);
        }
        g.dispose();

        PredictionResult result = classifier.classify(darkImage);
        assertNotNull(result);
        assertEquals("carbonaceous shale", result.suggestedLabel());
        assertFalse(result.isUnknown());
        assertTrue(result.confidence() > 0.45);
        assertEquals(TextureColorClassifier.MODEL_ID, result.modelId());
        assertEquals(TextureColorClassifier.PREPROCESSING_VERSION, result.preprocessingVersion());
    }

    @Test
    void classifiesLightImageAsLimestone() {
        BufferedImage lightImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = lightImage.createGraphics();
        g.setColor(new Color(210, 212, 215));
        g.fillRect(0, 0, 100, 100);
        g.setColor(new Color(190, 192, 195));
        for (int i = 0; i < 50; i++) {
            g.drawOval(i * 2, i * 2, 8, 8);
        }
        g.dispose();

        PredictionResult result = classifier.classify(lightImage);
        assertNotNull(result);
        assertEquals("limestone", result.suggestedLabel());
        assertFalse(result.isUnknown());
        assertTrue(result.confidence() > 0.45);
    }

    @Test
    void classifiesWarmImageAsDolostone() {
        BufferedImage warmImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = warmImage.createGraphics();
        // Buff / tan / brown tone
        g.setColor(new Color(135, 110, 85));
        g.fillRect(0, 0, 100, 100);
        g.setColor(new Color(120, 95, 75));
        for (int i = 0; i < 30; i++) {
            g.drawRect(i * 3, i * 3, 10, 10);
        }
        g.dispose();

        PredictionResult result = classifier.classify(warmImage);
        assertNotNull(result);
        assertEquals("dolostone", result.suggestedLabel());
        assertFalse(result.isUnknown());
        assertTrue(result.confidence() > 0.40);
    }

    @Test
    void abstainsOnUniformFlatMonochromaticImage() {
        BufferedImage flatImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flatImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        PredictionResult result = classifier.classify(flatImage);
        assertNotNull(result);
        assertTrue(result.isUnknown());
        assertEquals("unassessable", result.suggestedLabel());
    }
}

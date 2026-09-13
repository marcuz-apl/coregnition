package io.github.marcuzapl.coregnition.backend.analysis;

import io.github.marcuzapl.coregnition.backend.image.OpenCvProbe;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TextureColorClassifier implements LithologyClassifier {
    public static final String MODEL_ID = "coregnition-texture-color-baseline-v1";
    public static final String MODEL_CHECKSUM = "sha256:6e1b6f0e49a6df78b88d3d9cb95e54d31cb09c488562d981882d9212ad7a0491";
    public static final String PREPROCESSING_VERSION = "v1.0-rgb-luminance-grad";

    private static final double ABSTENTION_THRESHOLD = 0.40;

    @Override
    public PredictionResult classify(BufferedImage image) {
        if (image == null || image.getWidth() < 4 || image.getHeight() < 4) {
            return abstainedResult("unassessable", 1.0);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        double sumR = 0;
        double sumG = 0;
        double sumB = 0;
        double sumLum = 0;
        double sumLumSq = 0;

        int[] pixels = new int[totalPixels];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        for (int p : pixels) {
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;
            double lum = 0.299 * r + 0.587 * g + 0.114 * b;

            sumR += r;
            sumG += g;
            sumB += b;
            sumLum += lum;
            sumLumSq += lum * lum;
        }

        double meanR = sumR / totalPixels;
        double meanG = sumG / totalPixels;
        double meanB = sumB / totalPixels;
        double meanLum = sumLum / totalPixels;
        double varianceLum = (sumLumSq / totalPixels) - (meanLum * meanLum);
        double stdLum = Math.sqrt(Math.max(0, varianceLum));

        // Compute edge / gradient energy via horizontal & vertical Sobel differences
        double gradientEnergy = 0.0;
        int gradCount = 0;
        for (int y = 1; y < height - 1; y += 2) {
            for (int x = 1; x < width - 1; x += 2) {
                int pLeft = pixels[y * width + (x - 1)];
                int pRight = pixels[y * width + (x + 1)];
                int pTop = pixels[(y - 1) * width + x];
                int pBottom = pixels[(y + 1) * width + x];

                double lumL = 0.299 * ((pLeft >> 16) & 0xff) + 0.587 * ((pLeft >> 8) & 0xff) + 0.114 * (pLeft & 0xff);
                double lumR = 0.299 * ((pRight >> 16) & 0xff) + 0.587 * ((pRight >> 8) & 0xff) + 0.114 * (pRight & 0xff);
                double lumT = 0.299 * ((pTop >> 16) & 0xff) + 0.587 * ((pTop >> 8) & 0xff) + 0.114 * (pTop & 0xff);
                double lumB = 0.299 * ((pBottom >> 16) & 0xff) + 0.587 * ((pBottom >> 8) & 0xff) + 0.114 * (pBottom & 0xff);

                double dx = lumR - lumL;
                double dy = lumB - lumT;
                gradientEnergy += Math.sqrt(dx * dx + dy * dy);
                gradCount++;
            }
        }
        double meanGradient = gradCount > 0 ? gradientEnergy / gradCount : 0.0;

        // If essentially monochromatic flat / zero contrast, abstain as unassessable
        if (stdLum < 2.5 && meanGradient < 1.0) {
            return abstainedResult("unassessable", 0.95);
        }

        // Geological heuristics against confirmed vocabulary:
        // "limestone", "dolostone", "carbonaceous shale", "unknown", "mixed", "unassessable"
        double scoreShale = 0.05;
        double scoreLimestone = 0.05;
        double scoreDolostone = 0.05;
        double scoreMixed = 0.05;
        double scoreUnknown = 0.05;
        double scoreUnassessable = 0.05;

        // 1. Carbonaceous shale: dark organic-rich lithology, low luminance (meanLum < 95)
        if (meanLum < 95.0) {
            double darkFactor = (95.0 - meanLum) / 95.0; // 0 to 1
            scoreShale += 0.50 + 0.40 * darkFactor;
            if (meanGradient > 5.0 && stdLum > 10.0) {
                // Laminated shale
                scoreShale += 0.10;
            }
        }

        // 2. Limestone: light carbonate, high luminance (meanLum > 135)
        if (meanLum > 135.0) {
            double lightFactor = Math.min(1.0, (meanLum - 135.0) / 90.0);
            scoreLimestone += 0.50 + 0.35 * lightFactor;
            // Often neutral or slightly cool gray
            if (Math.abs(meanR - meanB) < 15.0) {
                scoreLimestone += 0.10;
            }
        }

        // 3. Dolostone: typically intermediate to buff/tan/brownish (meanLum 90..145, warm tint: R > B + 8)
        if (meanLum >= 85.0 && meanLum <= 150.0) {
            double warmTint = Math.max(0, meanR - meanB);
            if (warmTint > 8.0) {
                scoreDolostone += 0.45 + Math.min(0.40, warmTint * 0.02);
            } else {
                scoreDolostone += 0.20;
            }
        }

        // 4. Mixed lithology: high texture variance / bimodality
        if (stdLum > 35.0 && meanGradient > 15.0) {
            scoreMixed += 0.30 + Math.min(0.40, (stdLum - 35.0) * 0.015);
        }

        // Normalize raw scores into probability distribution
        double total = scoreShale + scoreLimestone + scoreDolostone + scoreMixed + scoreUnknown + scoreUnassessable;
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("carbonaceous shale", round(scoreShale / total));
        scores.put("limestone", round(scoreLimestone / total));
        scores.put("dolostone", round(scoreDolostone / total));
        scores.put("mixed", round(scoreMixed / total));
        scores.put("unknown", round(scoreUnknown / total));
        scores.put("unassessable", round(scoreUnassessable / total));

        // Find top class
        String topLabel = "unknown";
        double maxScore = -1.0;
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                topLabel = entry.getKey();
            }
        }

        // Abstention check: if confidence below threshold, mark as unknown
        boolean isUnknown = false;
        if (maxScore < ABSTENTION_THRESHOLD || "unknown".equals(topLabel) || "unassessable".equals(topLabel)) {
            isUnknown = true;
            if (maxScore < ABSTENTION_THRESHOLD) {
                topLabel = "unknown";
            }
        }

        return new PredictionResult(
            topLabel,
            round(maxScore),
            scores,
            isUnknown,
            MODEL_ID,
            MODEL_CHECKSUM,
            PREPROCESSING_VERSION
        );
    }

    private PredictionResult abstainedResult(String label, double confidence) {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("carbonaceous shale", 0.0);
        scores.put("limestone", 0.0);
        scores.put("dolostone", 0.0);
        scores.put("mixed", 0.0);
        scores.put("unknown", "unknown".equals(label) ? round(confidence) : 0.0);
        scores.put("unassessable", "unassessable".equals(label) ? round(confidence) : 0.0);

        return new PredictionResult(
            label,
            round(confidence),
            scores,
            true,
            MODEL_ID,
            MODEL_CHECKSUM,
            PREPROCESSING_VERSION
        );
    }

    private static double round(double val) {
        return BigDecimal.valueOf(val).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}

package io.github.marcuzapl.coregnition.backend.analysis;

import java.awt.image.BufferedImage;

public interface LithologyClassifier {
    PredictionResult classify(BufferedImage image);
}

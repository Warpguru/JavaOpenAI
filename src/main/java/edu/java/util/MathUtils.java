package edu.java.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility math methods used by tutorial examples.
 */
public class MathUtils {

    private static final Logger logger = LogManager.getLogger(MathUtils.class);

    /**
     * Hidden constructor.
     */
    private MathUtils() {
    }

    /**
     * Computes the cosine similarity between two float vectors.
     *
     * @param a first vector
     * @param b second vector (must be the same length as {@code a})
     * @return cosine similarity in the range [-1.0, 1.0]
     * @throws IllegalArgumentException if the vectors have different lengths or are empty
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            String msg = "Vectors must have equal length: " + a.length + " vs " + b.length;
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (a.length == 0) {
            String msg = "Vectors must not be empty";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0.0 ? 0.0 : dot / denominator;
    }

}

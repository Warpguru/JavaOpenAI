package edu.java.examples;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.openai.client.OpenAIClient;
import com.openai.models.moderations.Moderation;
import com.openai.models.moderations.ModerationCreateParams;
import com.openai.models.moderations.ModerationCreateResponse;

import edu.java.api.ClientFactory;
import edu.java.api.Config;

/**
 * Content moderation example.
 *
 * <p>
 * Demonstrates the OpenAI Moderation endpoint, which classifies text into harmful categories (hate, harassment, violence,
 * self-harm, sexual, etc.) and returns a per-category score.
 *
 * <p>
 * This endpoint is available on OpenAI cloud only. Local LLM servers do not support moderation; the example exits with a clear
 * message if the endpoint returns an error.
 *
 * <p>
 * Run via: {@code java -jar JavaOpenAI-x.y.z.jar moderate}
 */
public class ModerationExample {

    private static final Logger logger = LogManager.getLogger(ModerationExample.class);

    /** Sample inputs submitted to the moderation endpoint - one benign, one mildly flagged. */
    private static final String[] SAMPLE_INPUTS = {
            "I love sunny days and going for walks.",
            "I am feeling very frustrated and want to punch a wall."
    };

    /**
     * Runs the moderation example.
     *
     * <p>
     * Submits two sample sentences - one benign and one mildly negative - to the moderation endpoint configured by
     * {@code OPENAI_MODERATION_MODEL} and prints which content categories were flagged. Demonstrates how to integrate
     * content-safety checks into an application.
     *
     * <p>
     * Note: {@link Moderation#categories()} returns a {@link Moderation.Categories} object directly (not wrapped in
     * {@link java.util.Optional}), and each individual category accessor returns a primitive {@code boolean}.
     */
    public void run() {
        String model = Config.getModerationModel();
        logger.info("Moderation model : {}", model);
        logger.info("NOTE: This endpoint requires an OpenAI cloud API key. Local LLM servers do not support moderation.");

        try {
            OpenAIClient client = ClientFactory.create();

            for (String input : SAMPLE_INPUTS) {
                //@formatter:off
                ModerationCreateParams params = ModerationCreateParams
                        .builder()
                        .input(input)
                        .model(model)
                        .build();

                ModerationCreateResponse response = client
                        .moderations()
                        .create(params);
                //@formatter:on

                logger.info("");
                logger.info("Input   : \"{}\"", input);
                response.results().forEach(result -> {
                    logger.info("Flagged : {}", result.flagged());
                    // categories() returns Moderation.Categories directly (not Optional).
                    // Each individual category accessor returns primitive boolean.
                    Moderation.Categories cats = result.categories();
                    boolean hate = cats.hate();
                    boolean harassment = cats.harassment();
                    boolean violence = cats.violence();
                    if (hate)
                        logger.info("  hate: true");
                    if (harassment)
                        logger.info("  harassment: true");
                    if (violence)
                        logger.info("  violence: true");
                    if (!hate && !harassment && !violence && !result.flagged())
                        logger.info("  (all categories: false)");
                });
            }

        } catch (Exception e) {
            logger.info("Moderation request failed: {} - this endpoint requires OpenAI cloud access.", e.getMessage());
            logger.error("Moderation request failed", e);
        }
    }
    
}

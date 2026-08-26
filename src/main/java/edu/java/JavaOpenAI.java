package edu.java;

import edu.java.api.Config;
import edu.java.examples.AudioSpeechExample;
import edu.java.examples.AudioTranscriptionExample;
import edu.java.examples.ChatExample;
import edu.java.examples.EmbeddingsExample;
import edu.java.examples.ImageGenerationExample;
import edu.java.examples.ModerationExample;
import edu.java.examples.ReasoningExample;
import edu.java.examples.StreamingExample;
import edu.java.examples.VisionExample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point and command dispatcher for the JavaOpenAI tutorial jar.
 *
 * <p>
 * Usage: {@code java -jar JavaOpenAI-x.y.z.jar <command>}
 *
 * <p>
 * Available commands:
 * <ul>
 * <li>{@code config} — print resolved configuration (API key masked)</li>
 * <li>{@code chat} — synchronous single-turn chat completion</li>
 * <li>{@code stream} — streaming (token-by-token) chat completion</li>
 * <li>{@code embed} — embeddings and cosine similarity</li>
 * <li>{@code vision} — multi-modal vision (image + text prompt)</li>
 * <li>{@code reason} — reasoning / chain-of-thought example</li>
 * <li>{@code tts} — text-to-speech synthesis (OpenAI cloud)</li>
 * <li>{@code stt} — speech-to-text transcription (OpenAI cloud)</li>
 * <li>{@code imagegen} — image generation / DALL·E (OpenAI cloud)</li>
 * <li>{@code moderate} — content moderation (OpenAI cloud)</li>
 * </ul>
 */
public class JavaOpenAI {

    private static final Logger logger = LogManager.getLogger(JavaOpenAI.class);
    
    /** JavaOpenAI version (keep in sync with pom.xml). */
    public static final String JAVAOPENAI_VERSION = "x.y.z";

    public static void main(String[] args) {
        new JavaOpenAI().process(args);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    private void process(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        switch (args[0]) {
        case "config" -> runConfig();
        case "chat" -> new ChatExample().run();
        case "stream" -> new StreamingExample().run();
        case "embed" -> new EmbeddingsExample().run();
        case "vision" -> new VisionExample().run(args);
        case "reason" -> new ReasoningExample().run();
        case "tts" -> new AudioSpeechExample().run();
        case "stt" -> new AudioTranscriptionExample().run();
        case "imagegen" -> new ImageGenerationExample().run();
        case "moderate" -> new ModerationExample().run();
        default -> {
            logger.info("Unknown command: {}", args[0]);
            printUsage();
        }
        }
    }

    private void runConfig() {
        String apiKey = Config.getApiKey();
        String maskedKey = maskApiKey(apiKey);

        logger.info("=== Resolved Configuration ===");
        logger.info("Base URL         : {}", Config.getBaseUrl());
        logger.info("API Key          : {}", maskedKey);
        logger.info("Model            : {}", Config.getModel());
        logger.info("Embedding model  : {}", Config.getEmbeddingModel());
        logger.info("Reasoning model  : {}", Config.getReasoningModel());
        logger.info("TTS model        : {}", Config.getTtsModel());
        logger.info("STT model        : {}", Config.getSttModel());
        logger.info("Image model      : {}", Config.getImageModel());
        logger.info("Moderation model : {}", Config.getModerationModel());
    }

    private void printUsage() {
        logger.info("Usage: java -jar JavaOpenAI-{}.jar <command>", JAVAOPENAI_VERSION);
        logger.info("");
        logger.info("Available commands:");
        logger.info("  config   Print resolved configuration (API key masked)");
        logger.info("  chat     Synchronous single-turn chat completion");
        logger.info("  stream   Streaming (token-by-token) chat completion");
        logger.info("  embed    Embeddings and cosine similarity");
        logger.info("  vision   Multi-modal vision: describe an image (Usage: vision [imageUrl])");
        logger.info("  reason   Reasoning / chain-of-thought example");
        logger.info("  tts      Text-to-speech synthesis");
        logger.info("  stt      Speech-to-text transcription");
        logger.info("  imagegen Image generation (e.g. DALL·E)");
        logger.info("  moderate Content moderation");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Masks an API key for safe display: shows the first 5 characters followed by {@code ****}, or just {@code ****} if the key
     * is shorter than 5 characters or not set.
     */
    String maskApiKey(String key) {
        if (key == null || key.isEmpty()) {
            return "(not set)";
        }
        if (key.length() <= 5) {
            return "****";
        }
        return key.substring(0, 5) + "****";
    }

}

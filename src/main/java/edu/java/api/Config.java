package edu.java.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised configuration loader.
 *
 * <p>
 * Resolution order for each key:
 * <ol>
 * <li>Environment variable</li>
 * <li>{@code config.properties} on the classpath</li>
 * <li>Hard-coded default</li>
 * </ol>
 *
 * <p>
 * Every getter always returns a non-null, non-empty string. {@code config.properties} is
 * gitignored — copy {@code config.properties.example} to create it. If the file is absent,
 * only environment variables and hard-coded defaults are used.
 */
public class Config {

    private static final Logger logger = LogManager.getLogger(Config.class);

    private static final String FILE_CONFIG_PROPERTIES = "config.properties";

    /** Lazily loaded; {@code null} means not yet initialised. */
    private static volatile Properties props;

    /**
     * Hidden constructor.
     */
    private Config() {
    }

    /**
     * Returns the value for {@code key}, or {@code null} if not found. Environment variables take precedence over
     * {@code config.properties}.
     */
    public static String get(String key) {
        // 1. Environment variable
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        // 2. config.properties
        return loadProperties().getProperty(key);
    }

    /**
     * Base URL for the OpenAI API endpoint. Defaults to the public OpenAI endpoint.
     * 
     * @return baseUrl
     */
    public static String getBaseUrl() {
        String v = get("OPENAI_BASE_URL");
        return (v != null && !v.isEmpty()) ? v : "https://api.openai.com/v1";
    }

    /**
     * API key. Defaults to an empty string when {@code OPENAI_API_KEY} is not set; most
     * providers will then reject requests with an authentication error, which is more helpful
     * than a {@link NullPointerException} inside the SDK.
     *
     * @return apiKey, never {@code null}
     */
    public static String getApiKey() {
        String v = get("OPENAI_API_KEY");
        return (v != null) ? v : "";
    }

    /**
     * Chat / vision model. Defaults to {@code gpt-4o-mini}.
     * 
     * @return model
     */
    public static String getModel() {
        String v = get("OPENAI_MODEL");
        return (v != null && !v.isEmpty()) ? v : "gpt-4o-mini";
    }

    /**
     * Embeddings model. Defaults to {@code text-embedding-3-small}.
     * 
     * @return embeddingModel
     */
    public static String getEmbeddingModel() {
        String v = get("OPENAI_EMBEDDING_MODEL");
        return (v != null && !v.isEmpty()) ? v : "text-embedding-3-small";
    }

    /**
     * Reasoning / thinking model (o1/o3/QwQ-style). Defaults to {@code o4-mini}.
     *
     * @return reasoningModel
     */
    public static String getReasoningModel() {
        String v = get("OPENAI_REASONING_MODEL");
        return (v != null && !v.isEmpty()) ? v : "o4-mini";
    }

    /**
     * Text-to-speech model. Defaults to {@code tts-1}.
     * 
     * @return ttsModel
     */
    public static String getTtsModel() {
        String v = get("OPENAI_TTS_MODEL");
        return (v != null && !v.isEmpty()) ? v : "tts-1";
    }

    /**
     * Speech-to-text (Whisper) model. Defaults to {@code whisper-1}.
     * 
     * @return sttModel
     */
    public static String getSttModel() {
        String v = get("OPENAI_STT_MODEL");
        return (v != null && !v.isEmpty()) ? v : "whisper-1";
    }

    /**
     * Image-generation model. Defaults to {@code dall-e-2}.
     * 
     * @return imageModel
     */
    public static String getImageModel() {
        String v = get("OPENAI_IMAGE_MODEL");
        return (v != null && !v.isEmpty()) ? v : "dall-e-2";
    }

    /**
     * Content moderation model. Defaults to {@code omni-moderation-latest}.
     * 
     * @return moderationModel
     */
    public static String getModerationModel() {
        String v = get("OPENAI_MODERATION_MODEL");
        return (v != null && !v.isEmpty()) ? v : "omni-moderation-latest";
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Loads {@code config.properties} from the classpath on first call and caches the result.
     *
     * <p>
     * Uses double-checked locking to ensure the file is read at most once even under concurrent access. If the file is absent
     * the returned {@link Properties} object is empty; callers fall back to hard-coded defaults.
     *
     * @return the loaded (possibly empty) {@link Properties} instance
     */
    private static Properties loadProperties() {
        if (props == null) {
            synchronized (Config.class) {
                if (props == null) {
                    Properties p = new Properties();
                    try (InputStream is = Config.class.getClassLoader().getResourceAsStream(FILE_CONFIG_PROPERTIES)) {
                        if (is != null) {
                            p.load(is);
                            logger.debug("Loaded {}", FILE_CONFIG_PROPERTIES);
                        } else {
                            logger.debug("{} not found on classpath — using env vars and defaults only",
                                    FILE_CONFIG_PROPERTIES);
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to read {}: {}", FILE_CONFIG_PROPERTIES, e.getMessage());
                    }
                    props = p;
                }
            }
        }
        return props;
    }
    
}

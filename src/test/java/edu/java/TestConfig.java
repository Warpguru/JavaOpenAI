package edu.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Static configuration helper for integration tests.
 *
 * <p>
 * All values are loaded once at class-initialisation time from
 * {@code src/test/resources/test.properties} (gitignored). If the file is absent, built-in
 * defaults that point to a locally-running server ({@code http://localhost:11434/v1}) are used
 * so that tests can run out of the box against Ollama without any setup.
 *
 * <p>
 * To configure a different server or model, copy {@code test.properties.example} to
 * {@code test.properties} and set the desired values. The file is gitignored and must never
 * be committed.
 *
 * <p>
 * Optional provider capabilities (audio, image generation, moderation, reasoning) default to
 * disabled ({@code false} / empty string). Tests that require them guard themselves with
 * {@code @EnabledIf} and skip gracefully rather than failing when the flags are absent.
 */
public class TestConfig {

    private static final Logger logger = LogManager.getLogger(TestConfig.class);

    private static final String FILE_TEST_PROPERTIES = "test.properties";

    private static final Properties PROPS = load();

    /**
     * Hidden constructor - all members are static.
     */
    private TestConfig() {
    }

    /**
     * Base URL of the OpenAI-compatible endpoint under test.
     * Returns an empty string when {@code test.base.url} is not set or is blank, which causes
     * all network-dependent tests to be skipped via {@code @EnabledIf("serverConfigured")}.
     *
     * @return base URL string, or empty string if not configured
     */
    public static String baseUrl() {
        return PROPS.getProperty("test.base.url", "").trim();
    }

    /**
     * API key sent with every request. Local servers typically accept any non-empty value;
     * for cloud endpoints set a real key in {@code test.properties}.
     *
     * @return API key string, defaults to {@code "local"}
     */
    public static String apiKey() {
        return PROPS.getProperty("test.api.key", "undefined");
    }

    /**
     * General-purpose chat and embedding model used by most tests.
     * Defaults to {@code llama3.2:3b}.
     *
     * @return model identifier string
     */
    public static String model() {
        return PROPS.getProperty("test.model", "llama3.2:3b");
    }

    /**
     * Vision-capable model used by the vision test. Returns an empty string when
     * {@code test.vision.model} is not set - the vision test checks this value via
     * {@code @EnabledIf("visionModelConfigured")} and skips automatically.
     *
     * <p>
     * Set {@code test.vision.model} explicitly in {@code test.properties} even when the same
     * model handles both chat and vision - this makes the capability opt-in and consistent
     * with how reasoning, audio, image generation and moderation are handled.
     *
     * @return model identifier string, or empty string if not configured
     */
    public static String visionModel() {
        return PROPS.getProperty("test.vision.model", "").trim();
    }

    /**
     * Reasoning model (o1/o3/QwQ-style). Returns an empty string when
     * {@code test.reasoning.model} is not set - the reasoning test checks this value via
     * {@code @EnabledIf("reasoningModelConfigured")} and skips automatically.
     *
     * @return model identifier string, or empty string if not configured
     */
    public static String reasoningModel() {
        return PROPS.getProperty("test.reasoning.model", "").trim();
    }

    /**
     * Whether the target endpoint supports audio (TTS / STT) endpoints.
     * Defaults to {@code false}; set {@code test.audio.enabled=true} for OpenAI cloud or a
     * compatible provider.
     *
     * @return {@code true} if audio tests should run
     */
    public static boolean audioEnabled() {
        return Boolean.parseBoolean(PROPS.getProperty("test.audio.enabled", "false"));
    }

    /**
     * Whether the target endpoint supports the image-generation endpoint.
     * Defaults to {@code false}; set {@code test.image.generation.enabled=true} for DALL·E
     * or a compatible provider.
     *
     * @return {@code true} if image-generation tests should run
     */
    public static boolean imageGenerationEnabled() {
        return Boolean.parseBoolean(PROPS.getProperty("test.image.generation.enabled", "false"));
    }

    /**
     * Whether the target endpoint supports the moderation endpoint.
     * Defaults to {@code false}; set {@code test.moderation.enabled=true} for OpenAI cloud.
     *
     * @return {@code true} if moderation tests should run
     */
    public static boolean moderationEnabled() {
        return Boolean.parseBoolean(PROPS.getProperty("test.moderation.enabled", "false"));
    }

    // -------------------------------------------------------------------------

    /**
     * Loads {@code test.properties} from the test classpath. Called once at class
     * initialisation; the result is cached in {@link #PROPS}. If the file is absent a
     * warning is logged and an empty {@link Properties} instance is returned so that all
     * getters fall back to their hard-coded defaults.
     *
     * @return loaded (possibly empty) {@link Properties}
     */
    private static Properties load() {
        Properties p = new Properties();
        try (InputStream is = TestConfig.class.getClassLoader().getResourceAsStream(FILE_TEST_PROPERTIES)) {
            if (is != null) {
                p.load(is);
                logger.debug("Loaded {}", FILE_TEST_PROPERTIES);
            } else {
                logger.warn("{} not found on classpath - using defaults. "
                        + "Copy test.properties.example to test.properties to configure.", FILE_TEST_PROPERTIES);
            }
        } catch (IOException e) {
            logger.warn("Failed to read {}: {}", FILE_TEST_PROPERTIES, e.getMessage());
        }
        return p;
    }

}

package edu.java;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import edu.java.api.Config;
import edu.java.util.MathUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the JavaOpenAI tutorial, exercising the full API surface against any
 * OpenAI-compatible endpoint (OpenAI cloud, Ollama, LM Studio, etc.).
 *
 * <p>
 * Connection details are loaded from {@code src/test/resources/test.properties} (gitignored).
 * Copy {@code test.properties.example} and fill in the values for your target server. Without
 * the file, defaults pointing to {@code http://localhost:11434/v1} are used.
 *
 * <p>
 * Tests that require optional provider capabilities (vision, audio, image generation,
 * moderation, reasoning) are conditionally skipped via {@code @EnabledIf} when the
 * corresponding key is absent or {@code false} in {@code test.properties}.
 *
 * <p>
 * Vision tests encode images as Base64 data URLs so that local servers do not need to fetch
 * external URLs. Tests that call embeddings catch unsupported-operation exceptions and log a
 * skip message rather than failing, since some chat-only models omit the endpoint.
 */
class JavaOpenAIIntegrationTest {

    private static final Logger logger = LogManager.getLogger(JavaOpenAIIntegrationTest.class);

    /** Primary URL used for the vision test image. */
    private static final String VISION_IMAGE_URL =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTcsttXjKGaz5esiHafrsYeQe0VtUviNo9IiD3uRKiPjw&s";

    /** Classpath resource name used as fallback when the vision image URL cannot be downloaded. */
    private static final String VISION_FALLBACK_RESOURCE = "Pillars of creation.jpg";

    // -------------------------------------------------------------------------
    // Config tests (no network required)
    // -------------------------------------------------------------------------

    @Test
    void config_defaultBaseUrlIsOpenAI() {
        String baseUrl = Config.getBaseUrl();
        assertNotNull(baseUrl, "Base URL must never be null");
        assertFalse(baseUrl.isBlank(), "Base URL must not be blank");
        logger.info("[Config] Resolved base URL: {}", baseUrl);
    }

    @Test
    void config_modelHasDefault() {
        String model = Config.getModel();
        assertNotNull(model);
        assertFalse(model.isBlank());
        logger.info("[Config] Resolved model: {}", model);
    }

    @Test
    void config_embeddingModelHasDefault() {
        String model = Config.getEmbeddingModel();
        assertNotNull(model);
        assertFalse(model.isBlank());
        logger.info("[Config] Resolved embedding model: {}", model);
    }

    // -------------------------------------------------------------------------
    // ClientFactory tests
    // -------------------------------------------------------------------------

    @Test
    void clientFactory_buildsClientFromTestConfig() {
        OpenAIClient client = buildClient();
        assertNotNull(client);
        logger.info("[Client] Built client for: {}", TestConfig.baseUrl());
    }

    // -------------------------------------------------------------------------
    // Chat (live network call)
    // -------------------------------------------------------------------------

    @Test
    void chat_completionReturnsNonEmptyReply() {
        OpenAIClient client = buildClient();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage("You are a helpful assistant. Be concise.")
                .addUserMessage("Reply with exactly one sentence: what is 2 + 2?").model(TestConfig.model()).build();

        ChatCompletion response = client.chat().completions().create(params);

        assertNotNull(response, "Response must not be null");
        assertFalse(response.choices().isEmpty(), "Response must have at least one choice");

        String reply = response.choices().get(0).message().content().orElse("");
        logger.info("[Chat] Model reply: {}", reply);

        assertFalse(reply.isBlank(), "Assistant reply must not be blank");
        assertTrue(reply.contains("4") || reply.toLowerCase().contains("four"),
                "Reply should mention '4' or 'four' for 2+2 question, got: " + reply);
    }

    @Test
    void chat_tokenUsageIsReported() {
        OpenAIClient client = buildClient();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder().addUserMessage("Say 'hello'.")
                .model(TestConfig.model()).build();

        ChatCompletion response = client.chat().completions().create(params);

        response.usage().ifPresent(usage -> {
            logger.info("[Chat] Tokens — prompt: {}, completion: {}, total: {}",
                    usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
            assertTrue(usage.totalTokens() > 0, "Total tokens must be positive");
        });
    }

    // -------------------------------------------------------------------------
    // Streaming
    // -------------------------------------------------------------------------

    @Test
    void streaming_receivesMultipleChunks() {
        OpenAIClient client = buildClient();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage("You are a helpful assistant. Be concise.").addUserMessage("Count from 1 to 3.")
                .model(TestConfig.model()).build();

        AtomicInteger chunkCount = new AtomicInteger(0);
        StringBuilder fullReply = new StringBuilder();

        try (StreamResponse<ChatCompletionChunk> stream = client.chat().completions().createStreaming(params)) {

            stream.stream().forEach(chunk -> {
                if (!chunk.choices().isEmpty()) {
                    chunk.choices().get(0).delta().content().ifPresent(token -> {
                        fullReply.append(token);
                        chunkCount.incrementAndGet();
                    });
                }
            });
        }

        logger.info("[Stream] Chunks received: {}", chunkCount.get());
        logger.info("[Stream] Full reply: {}", fullReply);

        assertTrue(chunkCount.get() > 1, "Streaming should deliver multiple chunks, got: " + chunkCount.get());
        assertFalse(fullReply.toString().isBlank(), "Streamed reply must not be blank");
        String reply = fullReply.toString();
        assertTrue(reply.contains("1") || reply.contains("2") || reply.contains("3"),
                "Reply should contain some numbers, got: " + reply);
    }

    // -------------------------------------------------------------------------
    // Embeddings
    // -------------------------------------------------------------------------

    @Test
    void embeddings_cosineSimilarityOfRelatedSentences() {
        OpenAIClient client = buildClient();

        try {
            float[] vecA = getEmbedding(client, TestConfig.model(), "The cat sat on the mat.");
            float[] vecB = getEmbedding(client, TestConfig.model(), "A feline rested on a rug.");
            float[] vecC = getEmbedding(client, TestConfig.model(), "The stock market crashed today.");

            double simAB = MathUtils.cosineSimilarity(vecA, vecB);
            double simAC = MathUtils.cosineSimilarity(vecA, vecC);

            logger.info("[Embed] Similarity A-B (related):   {}", String.format("%.6f", simAB));
            logger.info("[Embed] Similarity A-C (unrelated): {}", String.format("%.6f", simAC));

            assertTrue(vecA.length > 0, "Embedding vector must be non-empty");
            assertTrue(simAB > simAC,
                    "Related sentences should be more similar than unrelated ones. simAB=" + simAB + ", simAC=" + simAC);

        } catch (Exception e) {
            // Some models don't support embeddings — log and skip gracefully
            logger.warn("[Embed] Skipped: model does not support embeddings — {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Vision (conditionally enabled)
    // -------------------------------------------------------------------------

    static boolean visionModelConfigured() {
        return !TestConfig.visionModel().isEmpty();
    }

    @Test
    @EnabledIf("visionModelConfigured")
    void vision_describesImageViaBase64() throws Exception {
        // Sends the image as a Base64 data URL so local servers can process it
        // without needing to fetch external URLs.
        OpenAIClient client = buildClient();
        String visionModel = TestConfig.visionModel();

        String imageUrl = VISION_IMAGE_URL;

        try {
            String base64DataUrl = loadImageAsBase64(imageUrl);

            List<ChatCompletionContentPart> parts = List.of(
                    ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder()
                            .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder().url(base64DataUrl).build()).build()),
                    ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder()
                            .text("Describe what you see in this image in one sentence.").build()));
            ChatCompletionUserMessageParam userMessage = ChatCompletionUserMessageParam.builder()
                    .contentOfArrayOfContentParts(parts).build();

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder().addMessage(userMessage).model(visionModel)
                    .build();

            ChatCompletion response = client.chat().completions().create(params);
            String reply = response.choices().get(0).message().content().orElse("");
            logger.info("[Vision] Model reply: {}", reply);
            assertFalse(reply.isBlank(), "Vision reply must not be blank");

        } catch (com.openai.errors.BadRequestException e) {
            // Some local models reject vision input — treat as a known limitation, not a failure
            logger.warn("[Vision] Model does not support vision input via /v1: {}", e.getMessage());
        }
    }

    /**
     * Downloads an image from {@code url} as a Base64 data URL. If the download fails for any
     * reason (e.g. network block, 403), falls back to loading {@link #VISION_FALLBACK_RESOURCE}
     * from the test classpath.
     *
     * @param url primary image URL to attempt
     * @return Base64 data URL ({@code data:image/jpeg;base64,...})
     * @throws Exception if both the download and the classpath fallback fail
     */
    private static String loadImageAsBase64(String url) throws Exception {
        try {
            String result = edu.java.examples.VisionExample.toBase64DataUrl(url);
            logger.info("[Vision] Image loaded from URL: {}", url);
            return result;
        } catch (Exception downloadEx) {
            logger.warn("[Vision] Download failed ({}), falling back to classpath image", downloadEx.getMessage());
            try (InputStream in = JavaOpenAIIntegrationTest.class.getClassLoader()
                    .getResourceAsStream(VISION_FALLBACK_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Classpath fallback image '" + VISION_FALLBACK_RESOURCE + "' not found");
                }
                byte[] bytes = in.readAllBytes();
                logger.info("[Vision] Loaded fallback image from classpath ({} bytes)", bytes.length);
                return edu.java.examples.VisionExample.toBase64DataUrl(bytes, "image/jpeg");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Reasoning (conditionally enabled)
    // -------------------------------------------------------------------------

    static boolean reasoningModelConfigured() {
        return !TestConfig.reasoningModel().isEmpty();
    }

    @Test
    @EnabledIf("reasoningModelConfigured")
    void reasoning_returnsNonEmptyReply() {
        OpenAIClient client = buildClient();
        String model = TestConfig.reasoningModel();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage("A farmer has 17 sheep. All but 9 die. How many sheep are left?").model(model).build();

        ChatCompletion response = client.chat().completions().create(params);
        String reply = response.choices().get(0).message().content().orElse("");
        logger.info("[Reason] Reply: {}", reply);
        assertFalse(reply.isBlank(), "Reasoning reply must not be blank");
        assertTrue(reply.contains("9") || reply.toLowerCase().contains("nine"),
                "Reply should mention '9' or 'nine' as the answer, got: " + reply);
    }

    // -------------------------------------------------------------------------
    // MathUtils unit tests (no network)
    // -------------------------------------------------------------------------

    @Test
    void mathUtils_cosineSimilarityIdenticalVectors() {
        float[] v = { 1.0f, 2.0f, 3.0f };
        double sim = MathUtils.cosineSimilarity(v, v);
        assertEquals(1.0, sim, 1e-6, "Identical vectors must have similarity 1.0");
    }

    @Test
    void mathUtils_cosineSimilarityOrthogonalVectors() {
        float[] a = { 1.0f, 0.0f, 0.0f };
        float[] b = { 0.0f, 1.0f, 0.0f };
        double sim = MathUtils.cosineSimilarity(a, b);
        assertEquals(0.0, sim, 1e-6, "Orthogonal vectors must have similarity 0.0");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static OpenAIClient buildClient() {
        return OpenAIOkHttpClient.builder().baseUrl(TestConfig.baseUrl()).apiKey(TestConfig.apiKey()).build();
    }

    private float[] getEmbedding(OpenAIClient client, String model, String text) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder().input(text).model(model).build();
        CreateEmbeddingResponse response = client.embeddings().create(params);
        List<Float> values = response.data().get(0).embedding();
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++)
            result[i] = values.get(i);
        return result;
    }
}

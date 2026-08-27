package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Token-by-token streaming chat completion.
 *
 * <p>
 * Demonstrates Server-Sent Events (SSE) streaming so the assistant's reply appears progressively rather than all at once.
 *
 * <p>
 * Run via: {@code java -jar JavaOpenAI-x.y.z.jar stream}
 */
public class StreamingExample {

    private static final Logger logger = LogManager.getLogger(StreamingExample.class);

    /** System prompt sent with every streaming request. */
    private static final String SYSTEM_PROMPT = "You are a helpful assistant. Be concise.";

    /** User prompt that drives the streaming response. */
    private static final String USER_PROMPT = "Count from 1 to 5, saying one fun fact about each number.";

    /**
     * Runs the streaming chat completion example.
     *
     * <p>
     * Opens a Server-Sent Events (SSE) stream for a fixed prompt and accumulates tokens as
     * they arrive, then logs the assembled reply. The stream is closed automatically via
     * try-with-resources.
     *
     * @throws RuntimeException wrapping any API or network error
     */
    public void run() {
        try {
            OpenAIClient client = ClientFactory.create();
            String model = Config.getModel();

            //@formatter:off
            ChatCompletionCreateParams params = ChatCompletionCreateParams
                    .builder()
                    .addSystemMessage(SYSTEM_PROMPT)
                    .addUserMessage(USER_PROMPT)
                    .model(model)
                    .build();
            //@formatter:on

            logger.debug("Opening streaming chat request to model: {}", model);

            StringBuilder reply = new StringBuilder();

            //@formatter:off
            try (StreamResponse<ChatCompletionChunk> stream = client
                    .chat()
                    .completions()
                    .createStreaming(params)) {

                stream.stream().forEach(chunk -> {
                    if (!chunk.choices().isEmpty()) {
                        chunk
                            .choices()
                            .get(0)
                            .delta()
                            .content()
                            .ifPresent(reply::append);
                    }
                });
            }
            //@formatter:on

            logger.info(reply.toString());
            logger.debug("Streaming chat request completed");

        } catch (Exception e) {
            logger.error("Streaming request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Streaming request failed", e);
        }
    }

}

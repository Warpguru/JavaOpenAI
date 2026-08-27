package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.completions.CompletionUsage;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Synchronous single-turn chat completion example.
 *
 * <p>Demonstrates the most basic end-to-end API call: sends a system prompt and a user
 * question, receives the assistant's full reply in one blocking call, and logs token usage.
 *
 * <p>Run via: {@code java -jar JavaOpenAI-x.y.z.jar chat}
 */
public class ChatExample {

    private static final Logger logger = LogManager.getLogger(ChatExample.class);

    /** System prompt sent with every request. */
    private static final String SYSTEM_PROMPT = "You are a helpful assistant.";

    /** User question posed to the model. */
    private static final String USER_PROMPT =
            "Tell me a fun fact about Java (the programming language) in one sentence.";

    /**
     * Runs the synchronous chat completion example.
     *
     * <p>Sends a fixed question to the model configured by {@code OPENAI_MODEL}, prints the
     * assistant reply, and logs token usage.
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
                    .addUserMessage(USER_PROMPT).model(model)
                    .build();
            //@formatter:on

            logger.info("Sending chat request to model: {}", model);
            //@formatter:off
            ChatCompletion response = client
                    .chat()
                    .completions()
                    .create(params);
            //@formatter:on

            // Extract and print the assistant reply
            //@formatter:off
            String reply = response
                    .choices()
                    .get(0)
                    .message()
                    .content()
                    .orElse("");
            //@formatter:on
            logger.info(reply);

            // Log token usage at INFO (diagnostics channel)
            Optional<CompletionUsage> usageOpt = response.usage();
            usageOpt.ifPresent(usage -> logger.info("Token usage — prompt: {}, completion: {}, total: {}", usage.promptTokens(),
                    usage.completionTokens(), usage.totalTokens()));

        } catch (Exception e) {
            logger.error("Chat request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Chat request failed", e);
        }
    }
    
}

package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reasoning / Thinking example.
 *
 * <p>
 * Demonstrates extended reasoning (also known as "thinking" or "chain-of-thought") using reasoning-capable models such as
 * OpenAI o1/o3/o4, or locally available models like QwQ.
 *
 * <p>
 * The reasoning model is configured via {@code OPENAI_REASONING_MODEL} (defaults to {@code o4-mini}). If the configured model
 * does not support the reasoning effort parameter, the request falls back to a plain chat completion.
 *
 * <p>
 * Run via: {@code java -jar JavaOpenAI-x.y.z.jar reason}
 */
public class ReasoningExample {

    private static final Logger logger = LogManager.getLogger(ReasoningExample.class);

    /**
     * Runs the reasoning / chain-of-thought example.
     *
     * <p>
     * Sends a word problem to the model configured by {@code OPENAI_REASONING_MODEL} (defaults to {@code o4-mini}) and asks it
     * to show step-by-step reasoning. Reasoning-capable models (o1/o3/QwQ-style) produce an internal chain-of-thought before
     * delivering the final answer; standard models answer directly.
     */
    public void run() {
        String model = Config.getReasoningModel();
        logger.info("Reasoning model: {}", model);

        try {
            OpenAIClient client = ClientFactory.create();

            // Reasoning models use the same chat completion endpoint.
            // The "reasoning_effort" parameter (low/medium/high) hints to the model
            // how much internal chain-of-thought to apply before answering.
            // Not all models support this parameter — those that don't will simply ignore it.
            //@formatter:off
            ChatCompletionCreateParams params = ChatCompletionCreateParams
                    .builder()
                    .addUserMessage("A farmer has 17 sheep. All but 9 die. How many sheep does the farmer have left? "
                            + "Show your step-by-step reasoning before giving the final answer.")
                    .model(model)
                    .build();

            ChatCompletion response = client
                    .chat()
                    .completions()
                    .create(params);
            String reply = response
                    .choices()
                    .get(0)
                    .message()
                    .content()
                    .orElse("");
            //@formatter:on

            logger.info("");
            logger.info("Model reasoning + answer:");
            logger.info(reply);

            response.usage().ifPresent(u -> logger.info("Token usage — prompt: {}, completion: {}, total: {}", u.promptTokens(),
                    u.completionTokens(), u.totalTokens()));

        } catch (Exception e) {
            logger.info("Reasoning request failed: {} — ensure the configured reasoning model is available.", e.getMessage());
            logger.error("Reasoning request failed", e);
        }
    }
    
}

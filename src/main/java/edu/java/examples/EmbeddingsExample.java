package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import edu.java.util.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Embeddings and cosine similarity example.
 *
 * <p>Requests embedding vectors for two sentences from the configured embedding model and
 * computes their cosine similarity via {@link edu.java.util.MathUtils#cosineSimilarity}.
 * Demonstrates that semantically related sentences score higher than unrelated ones.
 *
 * <p>Run via: {@code java -jar JavaOpenAI-x.y.z.jar embed}
 */
public class EmbeddingsExample {

    private static final Logger logger = LogManager.getLogger(EmbeddingsExample.class);

    /** First sentence to embed. */
    private static final String SENTENCE_A = "The cat sat on the mat.";

    /** Second sentence to embed; semantically close to {@link #SENTENCE_A}. */
    private static final String SENTENCE_B = "A feline rested on a rug.";

    /**
     * Runs the embeddings example.
     *
     * <p>Embeds {@link #SENTENCE_A} and {@link #SENTENCE_B} and prints their cosine
     * similarity.
     */
    public void run() {
        String model = Config.getEmbeddingModel();
        logger.info("Embedding model: {}", model);

        try {
            OpenAIClient client = ClientFactory.create();

            float[] vecA = embed(client, model, SENTENCE_A);
            float[] vecB = embed(client, model, SENTENCE_B);

            double similarity = MathUtils.cosineSimilarity(vecA, vecB);

            logger.info("Sentence A : \"{}\"", SENTENCE_A);
            logger.info("Sentence B : \"{}\"", SENTENCE_B);
            logger.info("Dimensions : {}", vecA.length);
            logger.info("Cosine similarity : {}", String.format("%.6f", similarity));
        } catch (Exception e) {
            logger.info("Embeddings request failed: {} — the configured model or local server may not support embeddings.",
                    e.getMessage());
            logger.error("Embeddings request failed", e);
        }
    }

    /**
     * Requests an embedding vector for {@code text} from the API.
     *
     * @param client the {@link OpenAIClient} to use
     * @param model  the embedding model name (e.g. {@code text-embedding-3-small})
     * @param text   the input text to embed
     * @return a float array containing the embedding vector returned by the model
     */
    private float[] embed(OpenAIClient client, String model, String text) {
        //@formatter:off
        EmbeddingCreateParams params = EmbeddingCreateParams
                .builder()
                .input(text)
                .model(model)
                .build();

        CreateEmbeddingResponse response = client
                .embeddings()
                .create(params);
        List<Float> values = response
                .data()
                .get(0)
                .embedding();
        //@formatter:on

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }
    
}

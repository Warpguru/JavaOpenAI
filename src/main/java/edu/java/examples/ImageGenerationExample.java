package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Image generation (DALL·E) example.
 *
 * <p>Generates an image from a fixed text prompt using the OpenAI Images endpoint and
 * prints the URL of the result. The model is configured via {@code OPENAI_IMAGE_MODEL}
 * (default {@code dall-e-2}).
 *
 * <p>This endpoint is available on the OpenAI cloud only; local LLM servers do not support
 * image generation. The example exits with a clear message if the endpoint returns an error.
 *
 * <p>Run via: {@code java -jar JavaOpenAI-x.y.z.jar imagegen}
 */
public class ImageGenerationExample {

    private static final Logger logger = LogManager.getLogger(ImageGenerationExample.class);

    /**
     * Runs the image generation example.
     *
     * <p>Sends a hardcoded landscape prompt to the configured image model at size
     * {@code 512×512} and prints the returned image URL (or Base64 length if
     * {@code b64_json} format was requested). Note that {@code ImagesResponse.data()}
     * returns {@code Optional<List<Image>>}, so the result is unwrapped with
     * {@link java.util.Optional#ifPresent}.
     */
    public void run() {
        String model = Config.getImageModel();
        logger.info("Image model : {}", model);
        logger.info("NOTE: This endpoint requires an OpenAI cloud API key. Local LLM servers do not support image generation.");

        try {
            OpenAIClient client = ClientFactory.create();

            // ImageModel is a top-level class — there is no ImageGenerateParams.Model inner class.
            // ImagesResponse.data() returns Optional<List<Image>>, not List<Image>.
            //@formatter:off
            ImageGenerateParams params = ImageGenerateParams
                    .builder()
                    .prompt("A serene mountain lake at sunrise, photorealistic")
                    .model(model)
                    .n(1L)
                    .size("512x512")
                    .build();

            ImagesResponse response = client
                    .images()
                    .generate(params);
            //@formatter:on

            response.data().ifPresent(images -> images.forEach(image -> {
                image.url().ifPresent(url -> logger.info("Generated image URL: {}", url));
                image.b64Json().ifPresent(b64 -> logger.info("Generated image (Base64, {} chars)", b64.length()));
            }));
        } catch (Exception e) {
            logger.info("Image generation failed: {} — this endpoint requires OpenAI cloud access.", e.getMessage());
            logger.error("Image generation failed", e);
        }
    }
    
}

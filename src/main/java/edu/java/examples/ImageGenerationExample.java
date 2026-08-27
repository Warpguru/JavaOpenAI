package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Image generation example.
 *
 * <p>Generates an image from a fixed text prompt using the OpenAI Images endpoint and writes
 * the result to {@value #OUTPUT_FILE} in the current working directory. The model is
 * configured via {@code OPENAI_IMAGE_MODEL} (default {@code gpt-image-1}).
 *
 * <p>Models that return a URL (e.g. {@code dall-e-2}) have the URL logged; models that return
 * Base64 (e.g. {@code gpt-image-1}) have the image decoded and written to disk.
 *
 * <p>This endpoint is available on the OpenAI cloud only; local LLM servers do not support
 * image generation. The example exits with a clear message if the endpoint returns an error.
 *
 * <p>Run via: {@code java -jar JavaOpenAI-x.y.z.jar imagegen}
 */
public class ImageGenerationExample {

    private static final Logger logger = LogManager.getLogger(ImageGenerationExample.class);

    /** Name of the JPEG file written to the current working directory. */
    private static final String OUTPUT_FILE = "ImageGenerationExample.jpg";

    /** Text prompt describing the image to generate. */
    private static final String IMAGE_PROMPT = "A serene mountain lake at sunrise, photorealistic";

    /**
     * Image size requested from the API.
     * {@code 1024x1024} is supported by both {@code dall-e-2} and {@code gpt-image-1}.
     */
    private static final String IMAGE_SIZE = "1024x1024";

    /**
     * Runs the image generation example.
     *
     * <p>Sends {@link #IMAGE_PROMPT} to the configured image model at {@link #IMAGE_SIZE}.
     * If the response contains a URL it is logged; if it contains Base64 the bytes are
     * decoded and written to {@value #OUTPUT_FILE}. Note that {@code ImagesResponse.data()}
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
            // Size "1024x1024" is supported by both dall-e-2 and gpt-image-1.
            //@formatter:off
            ImageGenerateParams params = ImageGenerateParams
                    .builder()
                    .prompt(IMAGE_PROMPT)
                    .model(model)
                    .n(1L)
                    .size(IMAGE_SIZE)
                    .build();

            ImagesResponse response = client
                    .images()
                    .generate(params);
            //@formatter:on

            response.data().ifPresent(images -> images.forEach(image -> {
                // URL response (e.g. dall-e-2): log the temporary CDN link
                image.url().ifPresent(url -> logger.info("Generated image URL: {}", url));

                // Base64 response (e.g. gpt-image-1): decode and save to disk
                image.b64Json().ifPresent(b64 -> {
                    try {
                        byte[] bytes = Base64.getDecoder().decode(b64);
                        Path outputPath = Path.of(OUTPUT_FILE);
                        Files.write(outputPath, bytes);
                        logger.info("Image written to: {}", outputPath.toAbsolutePath());
                        logger.info("File size: {} bytes", bytes.length);
                    } catch (IOException e) {
                        logger.info("Failed to write image file: {}", e.getMessage());
                        logger.error("Image file write failed", e);
                    }
                });
            }));
        } catch (Exception e) {
            logger.info("Image generation failed: {} — this endpoint requires OpenAI cloud access.", e.getMessage());
            logger.error("Image generation failed", e);
        }
    }

}

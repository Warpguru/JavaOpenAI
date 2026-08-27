package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;

/**
 * Multi-modal vision example (image input + text prompt).
 *
 * <p>
 * Downloads an image from a URL and sends it as a Base64-encoded data URL alongside a text prompt asking the model to describe
 * the image. Encoding as Base64 is required for local LLM servers, which cannot fetch external URLs themselves.
 *
 * <p>
 * Works with any vision-capable model (e.g. a local vision LLM or {@code gpt-4o-mini}).
 *
 * <p>
 * Run via:
 * 
 * <pre>
 *   java -jar JavaOpenAI-x.y.z.jar vision
 *   java -jar JavaOpenAI-x.y.z.jar vision https://example.com/image.jpg
 * </pre>
 */
public class VisionExample {

    private static final Logger logger = LogManager.getLogger(VisionExample.class);

    /** Default image URL used when no argument is supplied. */
    private static final String DEFAULT_IMAGE_URL =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTcsttXjKGaz5esiHafrsYeQe0VtUviNo9IiD3uRKiPjw&s";

    /**
     * Runs the vision example.
     *
     * @param args the command-line arguments passed to the application; {@code args[1]}, if present, overrides the default
     *             image URL
     */
    public void run(String[] args) {
        String imageUrl = (args.length > 1 && !args[1].isBlank()) ? args[1] : DEFAULT_IMAGE_URL;
        String model = Config.getModel();

        logger.info("Image URL : {}", imageUrl);
        logger.info("Model     : {}", model);

        try {
            // Download and Base64-encode the image so any local server can process it
            String base64DataUrl = toBase64DataUrl(imageUrl);
            logger.info("Image downloaded and encoded ({} bytes Base64)", base64DataUrl.length());

            OpenAIClient client = ClientFactory.create();

            //@formatter:off
            List<ChatCompletionContentPart> parts = List.of(
                    ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage
                            .builder()
                            .imageUrl(ChatCompletionContentPartImage
                                    .ImageUrl
                                    .builder()
                                    .url(base64DataUrl) // data:image/...;base64,...
                                    .build())
                            .build()),
                    ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder()
                            .text("Describe what you see in this image in two or three sentences.").build()));

            ChatCompletionUserMessageParam userMessage = ChatCompletionUserMessageParam
                    .builder()
                    .contentOfArrayOfContentParts(parts).build();

            ChatCompletionCreateParams params = ChatCompletionCreateParams
                    .builder()
                    .addMessage(userMessage)
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
            logger.info("Model response:");
            logger.info(reply);
        } catch (Exception e) {
            logger.info("Vision request failed: {} — ensure the configured model supports vision input.", e.getMessage());
            logger.error("Vision request failed", e);
        }
    }

    /**
     * Downloads an image from {@code url} and returns a Base64 data URL of the form
     * {@code data:image/<mime>;base64,<data>} that can be embedded directly in an API request.
     *
     * <p>
     * The MIME type is inferred from the URL path: {@code .png} maps to {@code image/png};
     * all other extensions default to {@code image/jpeg}.
     *
     * @param url the HTTP(S) URL of the image to download
     * @return a Base64 data URL string ready to use as an image part in a chat request
     * @throws Exception if the image cannot be downloaded or read
     */
    public static String toBase64DataUrl(String url) throws Exception {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            byte[] bytes = in.readAllBytes();
            // Infer MIME type from URL path; default to jpeg
            String mime = url.toLowerCase().contains(".png") ? "image/png" : "image/jpeg";
            return toBase64DataUrl(bytes, mime);
        }
    }

    /**
     * Encodes raw image bytes as a Base64 data URL of the form
     * {@code data:<mime>;base64,<data>}.
     *
     * @param bytes the raw image bytes
     * @param mime  the MIME type (e.g. {@code image/jpeg}, {@code image/png})
     * @return a Base64 data URL string ready to use as an image part in a chat request
     */
    public static String toBase64DataUrl(byte[] bytes, String mime) {
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

}

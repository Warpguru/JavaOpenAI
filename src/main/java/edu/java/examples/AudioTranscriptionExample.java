package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Speech-to-Text (STT / Whisper transcription) example.
 *
 * <p>Downloads a short public-domain audio clip from {@link #SAMPLE_AUDIO_URL}, saves it
 * to a temporary file, and transcribes it using the OpenAI Whisper endpoint.
 *
 * <p>This endpoint is available on the OpenAI cloud only; local LLM servers do not support
 * it. The example exits with a clear message if the endpoint returns an error. The temporary
 * file is always deleted in the {@code finally} block.
 *
 * <p>Run via: {@code java -jar JavaOpenAI-x.y.z.jar stt}
 */
public class AudioTranscriptionExample {

    private static final Logger logger = LogManager.getLogger(AudioTranscriptionExample.class);

    /** URL of the short public-domain audio sample used for transcription (approx. 5 s, ~80 KB). */
    private static final String SAMPLE_AUDIO_URL = "https://www.kozco.com/tech/LRMonoPhase4.mp3";

    /**
     * Runs the STT example.
     *
     * <p>Downloads {@link #SAMPLE_AUDIO_URL} to a temp file, sends it to the Whisper endpoint
     * configured by {@code OPENAI_STT_MODEL}, and prints the recognised transcript.
     * The {@link com.openai.models.audio.transcriptions.TranscriptionCreateResponse} is a sealed
     * union; this example extracts the plain {@code Transcription} variant which is returned for
     * the default JSON response format.
     */
    public void run() {
        String model = Config.getSttModel();
        logger.info("STT model : {}", model);
        logger.info("NOTE: This endpoint requires an OpenAI cloud API key. Local LLM servers do not support STT.");

        Path tmpFile = null;
        try {
            // Download sample audio to a temp file (Whisper requires a file, not a URL)
            tmpFile = Files.createTempFile("openai-stt-", ".mp3");
            try (InputStream in = URI.create(SAMPLE_AUDIO_URL).toURL().openStream()) {
                Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Downloaded sample audio to {}", tmpFile);

            OpenAIClient client = ClientFactory.create();

            TranscriptionCreateParams params = TranscriptionCreateParams.builder().file(tmpFile).model(model).build();

            // TranscriptionCreateResponse is a sealed union (Transcription | Diarized | Verbose).
            // For whisper-1 the default format is json, which maps to the Transcription variant.
            //@formatter:off
            TranscriptionCreateResponse response = client
                    .audio()
                    .transcriptions()
                    .create(params);
            //@formatter:on
            String transcript = response.isTranscription() ? response.asTranscription().text() : response.toString();

            logger.info("Transcript: {}", transcript);

        } catch (Exception e) {
            logger.info("Transcription request failed: {} — this endpoint requires OpenAI cloud access.", e.getMessage());
            logger.error("Transcription request failed", e);
        } finally {
            if (tmpFile != null) {
                try {
                    Files.deleteIfExists(tmpFile);
                } catch (Exception ignored) {
                }
            }
        }
    }
    
}

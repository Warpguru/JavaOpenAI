package edu.java.examples;

import com.openai.client.OpenAIClient;
import com.openai.models.audio.speech.SpeechCreateParams;
import edu.java.api.ClientFactory;
import edu.java.api.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Text-to-Speech (TTS) example.
 *
 * <p>Uses the OpenAI Audio Speech endpoint to synthesise spoken audio from a fixed text
 * string and writes the MP3 result to {@value #OUTPUT_FILE} in the current working directory.
 *
 * <p>This endpoint is available on the OpenAI cloud only; local LLM servers do not support
 * it. The example exits with a clear message if the endpoint returns an error.
 *
 * <p>Run via: {@code java -jar JavaOpenAI-x.y.z.jar tts}
 */
public class AudioSpeechExample {

    private static final Logger logger = LogManager.getLogger(AudioSpeechExample.class);

    /** Name of the MP3 file written to the current working directory. */
    private static final String OUTPUT_FILE = "AudioSpeechExample.mp3";

    /** Text synthesised into speech. */
    private static final String TTS_INPUT = "Hello! This is a text-to-speech demonstration using the OpenAI API.";

    /**
     * Voice used for synthesis.
     * {@code Voice} is a sealed union type in the SDK; the string overload must be used.
     */
    private static final String TTS_VOICE = "nova";

    /**
     * Runs the TTS example.
     *
     * <p>Requests audio synthesis for a short demo sentence using the model configured by
     * {@code OPENAI_TTS_MODEL} and the {@code nova} voice, then writes the resulting MP3
     * bytes to {@value #OUTPUT_FILE}.
     */
    public void run() {
        String model = Config.getTtsModel();
        logger.info("TTS model : {}", model);
        logger.info("NOTE: This endpoint requires an OpenAI cloud API key. Local LLM servers do not support TTS.");

        try {
            OpenAIClient client = ClientFactory.create();

            //@formatter:off
            SpeechCreateParams params = SpeechCreateParams
                    .builder()
                    .input(TTS_INPUT)
                    .model(model)
                    // Voice is a sealed union type; use string overload for named voices
                    .voice(TTS_VOICE)
                    .build();
            //@formatter:on

            // The SDK returns the audio as a BinaryResponseContent; read all bytes.
            //@formatter:off
            byte[] audioBytes = client
                    .audio()
                    .speech()
                    .create(params)
                    .body()
                    .readAllBytes();
            //@formatter:on

            Path outputPath = Path.of(OUTPUT_FILE);
            Files.write(outputPath, audioBytes);

            logger.info("Audio written to: {}", outputPath.toAbsolutePath());
            logger.info("File size: {} bytes", audioBytes.length);

        } catch (IOException e) {
            logger.info("Failed to write audio file: {}", e.getMessage());
            logger.error("TTS file write failed", e);
        } catch (Exception e) {
            logger.info("TTS request failed: {} — this endpoint requires OpenAI cloud access.", e.getMessage());
            logger.error("TTS request failed", e);
        }
    }
    
}

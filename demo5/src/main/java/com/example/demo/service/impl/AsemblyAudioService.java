package com.example.demo.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class AsemblyAudioService {

    @Value("${google.cloud.credentials.path}")
    private Resource credentialsResource;

    public String uploadAudio(MultipartFile file) throws IOException {
        byte[] audioBytes = file.getBytes();

        // Convert audio to WAV format (if not already)
        byte[] wavBytes = convertToWav(audioBytes, file.getOriginalFilename());

        // Load Google credentials
        GoogleCredentials credentials;
        try (InputStream credentialsStream = credentialsResource.getInputStream()) {
            credentials = GoogleCredentials.fromStream(credentialsStream);
        }

        try (SpeechClient speechClient = SpeechClient.create(SpeechSettings.newBuilder().setCredentialsProvider(() -> credentials).build())) {
            ByteString audioContent = ByteString.copyFrom(wavBytes);

            int sampleRate = getSampleRate(new ByteArrayInputStream(wavBytes));

            // Log audio parameters

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(sampleRate)
                    .setLanguageCode("en-US")
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioContent)
                    .build();

            RecognizeResponse response = speechClient.recognize(config, audio);
            StringBuilder transcription = new StringBuilder();

            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcription.append(alternative.getTranscript());
            }

            return transcription.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to transcribe audio", e);
        }
    }

    private byte[] convertToWav(byte[] audioBytes, String originalFilename) throws IOException {
        // Check if already WAV format
        if (originalFilename.toLowerCase().endsWith(".wav")) {
            return audioBytes; // Return as-is
        }

        // Convert to WAV using AudioSystem
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
             AudioInputStream source = AudioSystem.getAudioInputStream(bais)) {

            // Log the audio format details
            logAudioFormatDetails(source.getFormat());

            // Define your target PCM format for conversion
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 1, 2, 48000, false);
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcm, source);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            AudioSystem.write(pcmStream, AudioFileFormat.Type.WAVE, baos);
            return baos.toByteArray();
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Unsupported audio format", e);
        }
    }

    private int getSampleRate(ByteArrayInputStream inputStream) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream)) {
            AudioFormat format = audioInputStream.getFormat();
            return (int) format.getSampleRate();
        } catch (Exception e) {
            e.printStackTrace();
            return 48000; // default to 16000 if there is an error
        }
    }

    private void logAudioFormatDetails(AudioFormat format) {
        System.out.println("Audio Format: " + format.toString());
        System.out.println("Channels: " + format.getChannels());
        System.out.println("Sample Rate: " + (int) format.getSampleRate());
        System.out.println("Sample Size in Bits: " + format.getSampleSizeInBits());
        System.out.println("Encoding: " + format.getEncoding());
    }
}

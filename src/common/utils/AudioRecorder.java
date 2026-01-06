package common.utils;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioRecorder {
    private TargetDataLine targetDataLine;
    private Thread recordingThread;

    // Định dạng âm thanh chuẩn: WAV, 44.1kHz, 16bit, Mono (đủ dùng cho voice)
    private AudioFormat getAudioFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 1; // Mono
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void startRecording(String filePath) {
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Microphone không được hỗ trợ!");
                return;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            System.out.println("--- BẮT ĐẦU GHI ÂM ---");

            // Chạy luồng riêng để ghi dữ liệu, tránh đơ giao diện
            recordingThread = new Thread(() -> {
                try (AudioInputStream audioInputStream = new AudioInputStream(targetDataLine)) {
                    File file = new File(filePath);
                    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            recordingThread.start();

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            System.out.println("--- DỪNG GHI ÂM ---");
        }
    }
}
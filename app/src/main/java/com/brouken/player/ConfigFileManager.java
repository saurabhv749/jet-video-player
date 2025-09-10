package com.brouken.player;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ConfigFileManager {
    private final Context context;
    private final File configDir;

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final MessageDigest DIGEST;
    static { // Static initializer block to handle potential exception
        try {
            DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize SHA-256 MessageDigest", e);
        }
    }

    public class VideoConfig {
        public int resizeMode;
        public float aspectRatio;
        public String aspectRatioTitle;
        public float scale;
    }

    public ConfigFileManager(Context context) {
        this.context = context;
        // Use app-specific external storage directory
        this.configDir = new File(context.getExternalFilesDir(null), "configs");
        // Ensure the configs directory exists
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    @NonNull
    public static String sha256Hash(@NonNull String input) {
        synchronized (DIGEST) {
            byte[] hash = DIGEST.digest(input.getBytes(StandardCharsets.UTF_8));
            char[] hexChars = new char[hash.length * 2];
            for (int i = 0; i < hash.length; i++) {
                int v = hash[i] & 0xFF;
                hexChars[i * 2] = HEX_ARRAY[v >>> 4];
                hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);
        }
    }

    // Save configuration for a video file
    public void saveConfig(@NonNull String uri, int resizeMode, float aspectRatio, String ratioTitle, float scale) {
        try {
            String configData = resizeMode + "," + aspectRatio + "," + ratioTitle + "," + scale;
            File configFile = new File(configDir, sha256Hash(uri).concat("_config.txt"));
            FileWriter writer = new FileWriter(configFile);
            writer.write(configData);
            writer.flush();
            writer.close();
        } catch (IOException e) {
        }
    }

    // Read configuration for a video file
    public VideoConfig readConfig(@NonNull String uri) {
        try {
            File configFile = new File(configDir, sha256Hash(uri).concat("_config.txt"));
            if (!configFile.exists()) {
                return null; // No config file exists
            }
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            String configData = reader.readLine();
            reader.close();
            String[] parts = configData.split(",");
            if (parts.length < 4) {
                return null; // Invalid config format
            }
            VideoConfig config = new VideoConfig();
            config.resizeMode = Integer.parseInt(parts[0]);
            config.aspectRatio = Float.parseFloat(parts[1]);
            config.aspectRatioTitle = parts[2];
            config.scale = Float.parseFloat(parts[3]);
            return config;
        } catch (IOException e) {
            // e.printStackTrace();
            return null;
        }
    }

    public void updateScale(@NonNull String uri, float scale) {
        VideoConfig config = readConfig(uri);
        if (config != null) {
            saveConfig(uri, config.resizeMode, config.aspectRatio, config.aspectRatioTitle, scale);
        }
    }
}
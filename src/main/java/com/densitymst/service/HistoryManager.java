package com.densitymst.service;

import com.densitymst.model.HistoryItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistent storage of execution history using Gson JSON serialization.
 * History is stored in a .densitymst directory within the user's home folder.
 */
public class HistoryManager {

    private static final String HISTORY_DIR = ".densitymst";
    private static final String HISTORY_FILE = "history.json";
    private final Path historyPath;
    private final Gson gson;

    public HistoryManager() {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path dir = homeDir.resolve(HISTORY_DIR);
        this.historyPath = dir.resolve(HISTORY_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Warning: Could not create history directory: " + e.getMessage());
        }
    }

    /**
     * Saves a new history item to the persistent store.
     */
    public void save(HistoryItem item) {
        List<HistoryItem> history = loadAll();
        history.add(0, item); // Most recent first
        writeAll(history);
    }

    /**
     * Loads all history items from the persistent store.
     */
    public List<HistoryItem> loadAll() {
        if (!Files.exists(historyPath)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(historyPath)) {
            Type listType = new TypeToken<List<HistoryItem>>() {}.getType();
            List<HistoryItem> items = gson.fromJson(reader, listType);
            return items != null ? items : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Warning: Could not load history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Writes the full history list to the file.
     */
    private void writeAll(List<HistoryItem> history) {
        try (Writer writer = Files.newBufferedWriter(historyPath)) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            System.err.println("Warning: Could not save history: " + e.getMessage());
        }
    }

    /**
     * Clears all history.
     */
    public void clearAll() {
        writeAll(new ArrayList<>());
    }
}

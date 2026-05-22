package org.example;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserService {
    private static final String FILE_NAME = "users.txt";
    private final Map<String, String> userDatabase = new HashMap<>();

    public UserService() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) userDatabase.put(parts[0], parts[1]);
                }
            } catch (IOException e) { LoggerService.log("ERROR", "DB load error: " + e.getMessage()); }
        }
    }

    public boolean login(String u, String p) {
        return userDatabase.containsKey(u) && userDatabase.get(u).equals(p);
    }

    public boolean register(String u, String p) {
        if (u.isBlank() || p.isBlank() || userDatabase.containsKey(u)) return false;
        userDatabase.put(u, p);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            writer.write(u + ":" + p);
            writer.newLine();
        } catch (IOException e) { LoggerService.log("ERROR", "Save error: " + e.getMessage()); }
        return true;
    }
}
package org.example;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class LoggerService {
    public static void log(String level, String message) {
        try (FileWriter fw = new FileWriter("app.log", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(LocalDateTime.now() + " [" + level + "] " + message);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
package org.example;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportGenerator {
    public static void generateWeatherReport(String city, String fullData) throws Exception {
        File dir = new File("reports");
        if (!dir.exists()) dir.mkdirs();
        String fileName = "reports/WeatherReport_" + System.currentTimeMillis() + ".pdf";

        try (PdfWriter writer = new PdfWriter(fileName);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            document.add(new Paragraph("Generated: " + LocalDateTime.now()));
            document.add(new Paragraph("City: " + city));
            document.add(new Paragraph(fullData));
        }
        LoggerService.log("INFO", "Report saved: " + fileName);
    }
}
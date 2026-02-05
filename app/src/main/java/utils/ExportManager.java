package utils;

import data.TestResult;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ExportManager {

    public static File exportToCSV(Context context, List<TestResult> results) {
        File file = new File(context.getExternalFilesDir(null), "RollingResistance_Export.csv");
        try (FileWriter writer = new FileWriter(file)) {
            // Header
            writer.append("Timestamp;Tire;Pressure(bar);Speed(kmh);Mode;P0(W);PLoad(W);Crr\n");

            for (TestResult res : results) {
                writer.append(String.valueOf(res.timestamp)).append(";")
                        .append(res.tireName).append(";")
                        .append(String.valueOf(res.pressureBar)).append(";")
                        .append(String.valueOf(res.speedKmh)).append(";")
                        .append(res.isManualInput ? "Manual" : "ESP").append(";")
                        .append(String.valueOf(res.I0A)).append(";")
                        .append(String.valueOf(res.ILoadedA)).append(";")
                        .append(String.format("%.6f", res.calculatedCrr)).append("\n");
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void createPdfReport(File file, List<TestResult> results) {
        try {
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Rolling Resistance Report").setFontSize(20).setBold());

            // Gruppierung nach Reifenname, um Vergleiche zu ermöglichen
            Map<String, List<TestResult>> groupedData = results.stream()
                    .collect(Collectors.groupingBy(res -> res.tireName));

            for (String tire : groupedData.keySet()) {
                document.add(new Paragraph("Tire: " + tire).setBold());

                Table table = new Table(4); // 4 Spalten
                table.addCell("Pressure (bar)");
                table.addCell("Speed (km/h)");
                table.addCell("Power Loss (W)");
                table.addCell("Crr Value");

                for (TestResult res : groupedData.get(tire)) {
                    table.addCell(String.valueOf(res.pressureBar));
                    table.addCell(String.valueOf(res.speedKmh));
                    table.addCell(String.format("%.2f", res.ILoadedA - res.I0A));
                    table.addCell(String.format("%.6f", res.calculatedCrr));
                }
                document.add(table);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
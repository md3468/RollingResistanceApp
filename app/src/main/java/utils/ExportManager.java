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
import java.util.Locale;


public class ExportManager {

    public static File exportToCSV(Context context, List<TestResult> results) {
        File file = new File(context.getExternalFilesDir(null), "RollingResistance_Export.csv");
        try (FileWriter writer = new FileWriter(file)) {
            // Header
            writer.append("ID;User_ID;Tire_Name/Size;Pressure_[Bar];Temperature;Speed_[Km/h];Speed_[RPM];Tubeless;Temperature_Stable;Pressure_Checked;Weight_on_Lever;Weight_on_Tire;Idel_Current;Loaded_Current;Mean_Idel_Current;Mean_Loaded_Current;Power_[P0];Power_[PLoad];Prr;Crr\n");

            for (TestResult res : results) {
                writer.append(String.valueOf(res.id)).append(";")
                        .append(String.valueOf(res.userId)).append(";")
                        .append(res.tireName).append(";")
                        .append(String.format(Locale.US, "%.2f", res.pressureBar)).append(";")
                        .append(String.format(Locale.US, "%.2f", res.temperatureC)).append(";")
                        .append(String.format(Locale.US, "%.2f", res.speedKmh)).append(";")
                        .append(String.format(Locale.US, "%.0f", res.etSpeedrpm)).append(";")
                        .append(res.isTubeless ? "Yes" : "No").append(";")
                        .append(res.isTempStable ? "Yes" : "No").append(";")
                        .append(res.isPressureChecked ? "Yes" : "No").append(";")
                        .append(String.format(Locale.US, "%.3f", res.massKg)).append(";")
                        .append(String.format(Locale.US, "%.3f", res.weightOnTire)).append(";")
                        .append(res.idleCurrentAmp != null ? res.idleCurrentAmp : "").append(";")
                        .append(res.loadCurrentAmp != null ? res.loadCurrentAmp : "").append(";")
                        .append(String.format(Locale.US, "%.3f", res.I0A)).append(";")
                        .append(String.format(Locale.US, "%.3f", res.ILoadedA)).append(";")
                        .append(String.format(Locale.US, "%.3f", res.powerP0)).append(";")
                        .append(String.format(Locale.US, "%.3f", res.powerPLoad)).append(";")
                        .append(String.format(Locale.US, "%.3f", res.pRR)).append(";")
                        .append(String.format(Locale.US, "%.6f", res.calculatedCrr)).append("\n");
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

            Table table = new Table(20);
            String[] headers = {"ID", "User_ID", "Tire_Name/Size", "Pressure_[Bar]", "Temperature", "Speed_[Km/h]", "Speed_[RPM]", "Tubeless", "Temp_Stable", "Pres_Checked", "Weight_Lever", "Weight_Tire", "Idle_Cur", "Load_Cur", "Mean_Idle", "Mean_Load", "P0", "PLoad", "Prr", "Crr"};
            for (String h : headers) table.addCell(h);

            for (TestResult res : results) {
                table.addCell(String.valueOf(res.id));
                table.addCell(String.valueOf(res.userId));
                table.addCell(res.tireName);
                table.addCell(String.format("%.2f", res.pressureBar));
                table.addCell(String.format("%.2f", res.temperatureC));
                table.addCell(String.format("%.2f", res.speedKmh));
                table.addCell(String.format("%.0f", res.etSpeedrpm));
                table.addCell(res.isTubeless ? "X" : "");
                table.addCell(res.isTempStable ? "X" : "");
                table.addCell(res.isPressureChecked ? "X" : "");
                table.addCell(String.format("%.3f", res.massKg));
                table.addCell(String.format("%.3f", res.weightOnTire));
                table.addCell(res.idleCurrentAmp != null ? res.idleCurrentAmp : "");
                table.addCell(res.loadCurrentAmp != null ? res.loadCurrentAmp : "");
                table.addCell(String.format("%.3f", res.I0A));
                table.addCell(String.format("%.3f", res.ILoadedA));
                table.addCell(String.format("%.3f", res.powerP0));
                table.addCell(String.format("%.3f", res.powerPLoad));
                table.addCell(String.format("%.3f", res.pRR));
                table.addCell(String.format("%.6f", res.calculatedCrr));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

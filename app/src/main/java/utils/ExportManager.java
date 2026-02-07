package utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import data.TestResult;

public class ExportManager {

    public static void createPdfReportFromStream(OutputStream outputStream, List<TestResult> results) {
        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate());

            // --- English Labels ---
            document.add(new Paragraph("Rolling Resistance Analysis Report").setFontSize(22).setBold());
            document.add(new Paragraph("Generated on: " + new java.util.Date().toString()));

            Map<String, List<TestResult>> groupedResults = results.stream()
                    .collect(Collectors.groupingBy(r -> r.tireName));

            for (Map.Entry<String, List<TestResult>> entry : groupedResults.entrySet()) {
                String tireName = entry.getKey();
                List<TestResult> tireData = entry.getValue();
                tireData.sort((a, b) -> Double.compare(a.pressureBar, b.pressureBar));

                document.add(new Paragraph("\nAnalysis for Tire: " + tireName).setBold().setFontSize(16));

                // --- 1. CHART ---
                if (tireData.size() > 1) {
                    document.add(new Paragraph("Crr vs. Tire Pressure:"));
                    Bitmap chartBitmap = generateChartBitmap(tireData);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Image chartImage = new Image(ImageDataFactory.create(stream.toByteArray()));
                    chartImage.setMaxWidth(UnitValue.createPercentValue(80));
                    document.add(chartImage);
                }

                // --- 2. TABLE ---
                Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 2, 2, 3, 3}))
                        .useAllAvailableWidth();

                table.addCell("ID");
                table.addCell("Pressure [bar]");
                table.addCell("Speed [km/h]");
                table.addCell("Loss [W]");
                table.addCell("Crr-Value");
                table.addCell("Notes");

                for (TestResult res : tireData) {
                    table.addCell(String.valueOf(res.id));
                    table.addCell(String.format(Locale.US, "%.2f", res.pressureBar));
                    table.addCell(String.format(Locale.US, "%.2f", res.speedKmh));
                    table.addCell(String.format(Locale.US, "%.3f", res.pRR));
                    table.addCell(String.format(Locale.US, "%.6f", res.calculatedCrr));
                    table.addCell(res.isTubeless ? "Tubeless" : "Tube");
                }
                document.add(table);
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * HIER findest du die Methode generateChartBitmap
     */
    private static Bitmap generateChartBitmap(List<TestResult> data) {
        int width = 850; // Etwas breiter für mehr Platz
        int height = 500;
        int paddingLeft = 140; // Deutlich mehr Platz für die Crr-Zahlen + Label
        int paddingBottom = 90;
        int paddingRight = 50;
        int paddingTop = 50;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#1976D2"));
        linePaint.setStrokeWidth(5f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);

        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(3f);
        axisPaint.setTextSize(16f); // Etwas kleiner für die Skalenwerte
        axisPaint.setAntiAlias(true);

        Paint labelPaint = new Paint(axisPaint);
        labelPaint.setTextSize(20f);
        labelPaint.setFakeBoldText(true);

        // --- Skalierung berechnen ---
        double minP = data.get(0).pressureBar;
        double maxP = data.get(data.size() - 1).pressureBar;
        double pRange = (maxP - minP == 0) ? 1 : (maxP - minP);

        double minCrr = data.stream().mapToDouble(r -> r.calculatedCrr).min().orElse(0);
        double maxCrr = data.stream().mapToDouble(r -> r.calculatedCrr).max().orElse(0.01);

        // Puffer, damit Punkte nicht am Rand kleben
        double diff = maxCrr - minCrr;
        double buffer = (diff == 0) ? 0.001 : diff * 0.15;
        minCrr -= buffer;
        maxCrr += buffer;
        double crrRange = maxCrr - minCrr;

        // Achsen zeichnen
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint); // X
        canvas.drawLine(paddingLeft, height - paddingBottom, paddingLeft, paddingTop, axisPaint); // Y

        // --- Y-Achsen Ticks (Zahlenwerte) ---
        int numTicks = 5;
        for (int i = 0; i <= numTicks; i++) {
            float y = (height - paddingBottom) - (i * (height - paddingBottom - paddingTop) / numTicks);
            double val = minCrr + (i * crrRange / numTicks);

            canvas.drawLine(paddingLeft - 8, y, paddingLeft, y, axisPaint);
            // Positionierung der Zahlen: rechtsbündig vor der Achse
            String valStr = String.format(Locale.US, "%.5f", val);
            canvas.drawText(valStr, paddingLeft - 85, y + 6, axisPaint);
        }

        // --- Achsenbeschriftung ---
        // X-Achse
        canvas.drawText("Pressure [bar]", (width + paddingLeft) / 2f - 60, height - 20, labelPaint);

        // Y-Achse: Weiter nach links geschoben (x = 25 statt 40), um nicht in die Zahlen zu ragen
        canvas.save();
        canvas.rotate(-90, 30, height / 2f);
        canvas.drawText("Crr-Value", 30, height / 2f, labelPaint);
        canvas.restore();

        // --- Daten zeichnen ---
        float lastX = -1, lastY = -1;
        for (TestResult res : data) {
            float x = (float) (paddingLeft + (res.pressureBar - minP) / pRange * (width - paddingLeft - paddingRight));
            float y = (float) ((height - paddingBottom) - (res.calculatedCrr - minCrr) / crrRange * (height - paddingBottom - paddingTop));

            canvas.drawCircle(x, y, 10, linePaint);

            // X-Achsen Wert (Druck)
            canvas.drawText(String.format(Locale.US, "%.1f", res.pressureBar), x - 15, height - paddingBottom + 30, axisPaint);

            if (lastX != -1) {
                canvas.drawLine(lastX, lastY, x, y, linePaint);
            }
            lastX = x;
            lastY = y;
        }
        return bitmap;
    }
}
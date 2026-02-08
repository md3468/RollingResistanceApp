package utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import data.TestResult;

public class ExportManager {

    private static final int[] LINE_COLORS = {
            Color.parseColor("#1976D2"), // Blau
            Color.parseColor("#D32F2F"), // Rot
            Color.parseColor("#388E3C"), // Grün
            Color.parseColor("#FBC02D")  // Gelb
    };

    public static void createPdfReportFromStream(OutputStream outputStream, List<TestResult> results) {
        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            // Wir bleiben bei A4 Querformat für den Vergleich
            Document document = new Document(pdf, PageSize.A4.rotate());

            // Ränder verkleinern, um mehr Platz auf der ersten Seite zu schaffen
            document.setMargins(20, 20, 20, 20);

            // Titel kompakter gestalten
            document.add(new Paragraph("Rolling Resistance Analysis Report")
                    .setFontSize(20)
                    .setBold()
                    .setMarginBottom(0)); // Kein Abstand nach unten

            document.add(new Paragraph("Generated on: " + new java.util.Date().toString())
                    .setFontSize(10)
                    .setMarginBottom(10));

            Map<String, List<TestResult>> groupedResults = results.stream()
                    .collect(Collectors.groupingBy(r -> r.tireName));

            // --- 1. GEMEINSAMER GRAPH ---
            if (results.size() > 1) {
                // "Performance Comparison" Titel direkt vor das Bild
                document.add(new Paragraph("Performance Comparison:")
                        .setBold()
                        .setFontSize(14)
                        .setMarginBottom(5));

                Bitmap chartBitmap = generateComparisonChart(groupedResults);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                Image chartImage = new Image(ImageDataFactory.create(stream.toByteArray()));

                // WICHTIG: Skalierung anpassen, damit es auf die erste Seite passt
                // Wir setzen eine feste Höhe oder nutzen Autoscale
                chartImage.setAutoScale(true);
                chartImage.setMaxHeight(350f); // Begrenzt die Höhe, damit Text noch drunter passt
                chartImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

                document.add(chartImage);
            }

            // --- 2. TABELLEN ---
            // Wenn der Platz nach dem Graphen nicht reicht, bricht iText hier automatisch um
            for (Map.Entry<String, List<TestResult>> entry : groupedResults.entrySet()) {
                document.add(new Paragraph("Data Table: " + entry.getKey())
                        .setBold()
                        .setMarginTop(10));

                List<TestResult> tireData = entry.getValue();
                tireData.sort(Comparator.comparingDouble(a -> a.pressureBar));

                Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2, 2, 3}))
                        .useAllAvailableWidth();

                // Kompakterer Tabellen-Header
                table.addHeaderCell("Tire Model");
                table.addHeaderCell("Bar");
                table.addHeaderCell("km/h");
                table.addHeaderCell("Loss [W]");
                table.addHeaderCell("Crr-Value");

                for (TestResult res : tireData) {
                    table.addCell(res.tireName);
                    table.addCell(String.format(Locale.US, "%.2f", res.pressureBar));
                    table.addCell(String.format(Locale.US, "%.1f", res.speedKmh));
                    table.addCell(String.format(Locale.US, "%.2f", res.pRR));
                    table.addCell(String.format(Locale.US, "%.6f", res.calculatedCrr));
                }
                document.add(table);
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Bitmap generateComparisonChart(Map<String, List<TestResult>> groupedData) {
        int width = 900;
        int height = 550;
        int paddingLeft = 140, paddingBottom = 90, paddingRight = 180, paddingTop = 60;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(3f);
        axisPaint.setTextSize(16f);
        axisPaint.setAntiAlias(true);

        // --- Globale Min/Max Werte für einheitliche Skalierung ---
        List<TestResult> allResults = groupedData.values().stream()
                .flatMap(List::stream).collect(Collectors.toList());

        double minP = allResults.stream().mapToDouble(r -> r.pressureBar).min().orElse(0);
        double maxP = allResults.stream().mapToDouble(r -> r.pressureBar).max().orElse(10);
        double minCrr = allResults.stream().mapToDouble(r -> r.calculatedCrr).min().orElse(0);
        double maxCrr = allResults.stream().mapToDouble(r -> r.calculatedCrr).max().orElse(0.01);

        double pRange = (maxP - minP == 0) ? 1 : (maxP - minP);
        double crrBuffer = (maxCrr - minCrr) * 0.15;
        minCrr -= crrBuffer; maxCrr += crrBuffer;
        double crrRange = maxCrr - minCrr;

        // Achsen
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint);
        canvas.drawLine(paddingLeft, height - paddingBottom, paddingLeft, paddingTop, axisPaint);

        // Y-Achse Beschriftung
        for (int i = 0; i <= 5; i++) {
            float y = (height - paddingBottom) - (i * (height - paddingBottom - paddingTop) / 5f);
            double val = minCrr + (i * crrRange / 5f);
            canvas.drawText(String.format(Locale.US, "%.5f", val), paddingLeft - 85, y + 6, axisPaint);
            canvas.drawLine(paddingLeft - 8, y, paddingLeft, y, axisPaint);
        }

        // --- Graphen und Legende zeichnen ---
        int tireIndex = 0;
        float legendY = paddingTop + 20;

        for (Map.Entry<String, List<TestResult>> entry : groupedData.entrySet()) {
            int color = LINE_COLORS[tireIndex % LINE_COLORS.length];

            Paint linePaint = new Paint();
            linePaint.setColor(color);
            linePaint.setStrokeWidth(5f);
            linePaint.setAntiAlias(true);
            linePaint.setStyle(Paint.Style.STROKE);

            List<TestResult> tireData = entry.getValue();
            tireData.sort(Comparator.comparingDouble(a -> a.pressureBar));

            float lastX = -1, lastY = -1;
            for (TestResult res : tireData) {
                float x = (float) (paddingLeft + (res.pressureBar - minP) / pRange * (width - paddingLeft - paddingRight));
                float y = (float) ((height - paddingBottom) - (res.calculatedCrr - minCrr) / crrRange * (height - paddingBottom - paddingTop));

                canvas.drawCircle(x, y, 8, linePaint);
                if (lastX != -1) canvas.drawLine(lastX, lastY, x, y, linePaint);

                // X-Achsen Beschriftung nur für den ersten Reifen oder allgemein
                if (tireIndex == 0) {
                    canvas.drawText(String.format(Locale.US, "%.1f", res.pressureBar), x - 15, height - paddingBottom + 30, axisPaint);
                }

                lastX = x; lastY = y;
            }

            // Legende rechts im Bild
            canvas.drawRect(width - paddingRight + 20, legendY - 10, width - paddingRight + 50, legendY, linePaint);
            axisPaint.setTextSize(18f);
            canvas.drawText(entry.getKey(), width - paddingRight + 60, legendY, axisPaint);
            legendY += 35;

            tireIndex++;
        }

        // Labels
        axisPaint.setFakeBoldText(true);
        canvas.drawText("Pressure [bar]", (width - paddingRight + paddingLeft) / 2f - 50, height - 20, axisPaint);
        canvas.save();
        canvas.rotate(-90, 30, height / 2f);
        canvas.drawText("Crr-Value", 30, height / 2f, axisPaint);
        canvas.restore();

        return bitmap;
    }
}
package utils;

import data.TestResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculationHelper {
    private static final double G = 9.81;
    private static final double LEVER_HANG = 0.875;    // l_hang in m
    private static final double LEVER_TIRE = 0.358;    // l_reifen in m
    private static final double V_SUPPLY_DEFAULT = 12.0;

    public static void processAndCalculate(TestResult res) {
        // --- 1. Perform all calculations with full precision ---
        
        // Get Diameter (d) and calculate Circumference (U)
        double d = extractDiameterFromEtrto(res.tireName);
        double U = Math.PI * d;

        // Calculate speed v [m/s]
        double v = U * (res.etSpeedrpm / 60.0);
        double rawSpeedKmh = v * 3.6;

        // Effective weight on tire: m_eff = (m_hang * l_hang) / l_reifen
        double mEff = (res.massKg * LEVER_HANG) / LEVER_TIRE;

        // Powers (Using default 12V)
        double vSupply = V_SUPPLY_DEFAULT;
        double p0 = vSupply * res.I0A;
        double pWeighted = vSupply * res.ILoadedA;
        double pRR = pWeighted - p0;

        // Crr = P_rr / (m_eff * g * v)
        double rawCrr = 0.0;
        if (mEff > 0 && v > 0) {
            rawCrr = pRR / (mEff * G * v);
        }

        // --- 2. Round all fields AFTER the calculations are done ---
        
        res.pressureBar = round(res.pressureBar, 2);
        res.temperatureC = round(res.temperatureC, 2);
        res.speedKmh = round(rawSpeedKmh, 2);
        res.etSpeedrpm = round(res.etSpeedrpm, 0);
        
        res.massKg = round(res.massKg, 3);
        res.weightOnTire = round(mEff, 3);
        
        res.idleCurrentAmp = round(res.idleCurrentAmp, 3);
        res.loadCurrentAmp = round(res.loadCurrentAmp, 3);
        res.I0A = round(res.I0A, 3);
        res.ILoadedA = round(res.ILoadedA, 3);
        
        res.powerP0 = round(p0, 3);
        res.powerPLoad = round(pWeighted, 3);
        res.pRR = round(pRR, 3);
        
        res.calculatedCrr = round(rawCrr, 6);
    }

    private static double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        try {
            return BigDecimal.valueOf(value)
                    .setScale(places, RoundingMode.HALF_UP)
                    .doubleValue();
        } catch (NumberFormatException e) {
            return value;
        }
    }

    public static double calculateAverage(String input) {
        if (input == null || input.isBlank()) return 0.0;
        String[] parts = input.split("[,\\s;]+");
        double sum = 0;
        int count = 0;
        for (String part : parts) {
            try {
                String cleanPart = part.trim().replace(",", ".");
                if (!cleanPart.isEmpty()) {
                    sum += Double.parseDouble(cleanPart);
                    count++;
                }
            } catch (NumberFormatException ignored) {}
        }
        return count > 0 ? sum / count : 0.0;
    }

    public static double getFirstValue(String input) {
        if (input == null || input.isBlank()) return 0.0;
        String[] parts = input.split("[,\\s;]+");
        for (String part : parts) {
            try {
                String cleanPart = part.trim().replace(",", ".");
                if (!cleanPart.isEmpty()) {
                    return Double.parseDouble(cleanPart);
                }
            } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private static double extractDiameterFromEtrto(String etrto) {
        if (etrto == null || etrto.isBlank()) return 0.0;
        try {
            Matcher matcher = Pattern.compile("\\d+").matcher(etrto);
            int count = 0;
            while (matcher.find()) {
                count++;
                if (count == 2) {
                    return Double.parseDouble(matcher.group()) / 1000.0;
                }
            }
        } catch (Exception e) {
            System.err.println("ETRTO Parse Error: " + e.getMessage());
        }
        return 0.0;
    }

    public static void calculateAndFill(TestResult result) {
        processAndCalculate(result);
    }
}

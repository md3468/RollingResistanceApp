package utils;

import data.TestResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalculationHelper {
    private static final double G = 9.81;
    private static final double LEVER_HANG = 0.875;    // l_hang in m
    private static final double LEVER_TIRE = 0.358;    // l_reifen in m
    private static final double V_SUPPLY_DEFAULT = 12.0;

    public static void processAndCalculate(TestResult res) {
        // 1. Get Diameter (d) and calculate Circumference (U)
        double d = extractDiameterFromEtrto(res.tireName);
        double U = Math.PI * d;

        // 2. Calculate speed v [m/s] = U * (rpm / 60)
        double v = U * (res.etSpeedrpm / 60.0);
        res.speedKmh = v * 3.6; // Storing km/h but using v for Crr

        // 3. Current averages
        double iIdle = res.I0A;
        double iLoad = res.ILoadedA;

        // 4. Effective weight on tire: m_eff = (m_hang * l_hang) / l_reifen
        double mEff = (res.massKg * LEVER_HANG) / LEVER_TIRE;

        // 5. Powers (Using system voltage or default)
        double vSupply = (res.voltageSystem > 0) ? res.voltageSystem : V_SUPPLY_DEFAULT;
        double p0 = vSupply * iIdle;
        double pWeighted = vSupply * iLoad;
        double pRR = pWeighted - p0;

        // 6. C_rr = P_rr / (m_eff * g * v)
        if (mEff > 0 && v > 0) {
            res.calculatedCrr = pRR / (mEff * G * v);
        } else {
            res.calculatedCrr = 0.0;
        }
    }

    public static double calculateAverage(String input) {
        if (input == null || input.isBlank()) return 0.0;
        // Split by comma, space or semicolon
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

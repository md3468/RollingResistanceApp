package utils;

import android.content.Context;
import android.content.SharedPreferences;
import data.TestResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculationHelper {
    public static double G = 9.81;
    public static double LEVER_HANG = 0.875;    // l_hang in m
    public static double LEVER_TIRE = 0.358;    // l_reifen in m
    public static double V_SUPPLY_DEFAULT = 12.0;
    public static double MOTOR_SPEED_RPM = 213.0; // Default motor speed

    private static final String PREFS_NAME = "CalculationConstants";

    public static void loadConstants(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        G = Double.longBitsToDouble(prefs.getLong("G", Double.doubleToLongBits(9.81)));
        LEVER_HANG = Double.longBitsToDouble(prefs.getLong("LEVER_HANG", Double.doubleToLongBits(0.875)));
        LEVER_TIRE = Double.longBitsToDouble(prefs.getLong("LEVER_TIRE", Double.doubleToLongBits(0.358)));
        V_SUPPLY_DEFAULT = Double.longBitsToDouble(prefs.getLong("V_SUPPLY_DEFAULT", Double.doubleToLongBits(12.0)));
        MOTOR_SPEED_RPM = Double.longBitsToDouble(prefs.getLong("MOTOR_SPEED_RPM", Double.doubleToLongBits(213.0)));
    }

    public static void saveConstants(Context context, double g, double lHang, double lTire, double vSupply, double rpm) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong("G", Double.doubleToLongBits(g));
        editor.putLong("LEVER_HANG", Double.doubleToLongBits(lHang));
        editor.putLong("LEVER_TIRE", Double.doubleToLongBits(lTire));
        editor.putLong("V_SUPPLY_DEFAULT", Double.doubleToLongBits(vSupply));
        editor.putLong("MOTOR_SPEED_RPM", Double.doubleToLongBits(rpm));
        editor.apply();
        
        // Update local variables
        G = g;
        LEVER_HANG = lHang;
        LEVER_TIRE = lTire;
        V_SUPPLY_DEFAULT = vSupply;
        MOTOR_SPEED_RPM = rpm;
    }

    public static void processAndCalculate(TestResult res) {
        // --- 1. Perform all calculations with full precision ---
        res.etSpeedrpm = MOTOR_SPEED_RPM; // Set the constant value

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
        
        res.massKg = round(res.massKg, 3);
        res.weightOnTire = round(mEff, 3);
        
        // res.idleCurrentAmp and res.loadCurrentAmp are now Strings containing raw input
        
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

    public static double parseInput(String input) {
        if (input == null || input.isBlank()) return 0.0;
        try {
            String clean = input.trim().replace(",", ".");
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static double calculateAverage(String input) {
        if (input == null || input.isBlank()) return 0.0;
        String[] parts = input.split("[\\s;\\t]+");
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
        String[] parts = input.split("[\\s;\\t]+");
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

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
    public static double LEVER_HANG = 0.875;
    // Wir behalten den Namen bei, behandeln ihn aber als RADIUS der Rolle (in m)
    public static double LEVER_TIRE = 0.358;
    public static double V_SUPPLY_DEFAULT = 12.0;
    public static double MOTOR_SPEED_RPM = 213.0;

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

        G = g;
        LEVER_HANG = lHang;
        LEVER_TIRE = lTire;
        V_SUPPLY_DEFAULT = vSupply;
        MOTOR_SPEED_RPM = rpm;
    }

    public static void processAndCalculate(TestResult res) {
        res.etSpeedrpm = MOTOR_SPEED_RPM;

        // --- KORREKTUR: Geschwindigkeit basiert nun auf Rollenradius (LEVER_TIRE) ---
        // v = 2 * PI * r * (RPM / 60)
        double rollerRadius = LEVER_TIRE;
        double circumferenceRoller = 2 * Math.PI * rollerRadius;
        double v = circumferenceRoller * (res.etSpeedrpm / 60.0);

        double rawSpeedKmh = v * 3.6;

        // Effektive Last auf den Reifen
        // m_eff = (m_hang * l_hang) / l_reifen
        double mEff = (res.massKg * LEVER_HANG) / LEVER_TIRE;

        // Leistungen
        double vSupply = V_SUPPLY_DEFAULT;
        double p0 = vSupply * res.I0A;
        double pWeighted = vSupply * res.ILoadedA;
        double pRR = pWeighted - p0;

        // Crr = P_rr / (m_eff * g * v)
        double rawCrr = 0.0;
        if (mEff > 0 && v > 0) {
            rawCrr = pRR / (mEff * G * v);
        }

        // --- Rundung ---
        res.pressureBar = round(res.pressureBar, 2);
        res.temperatureC = round(res.temperatureC, 2);
        res.speedKmh = round(rawSpeedKmh, 2);
        res.massKg = round(res.massKg, 3);
        res.weightOnTire = round(mEff, 3);
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
            return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
        } catch (NumberFormatException e) { return value; }
    }

    public static double parseInput(String input) {
        if (input == null || input.isBlank()) return 0.0;
        try {
            return Double.parseDouble(input.trim().replace(",", "."));
        } catch (NumberFormatException e) { return 0.0; }
    }

    public static double calculateAverage(String input) {
        if (input == null || input.isBlank()) return 0.0;
        String[] parts = input.split("[\\s;\\t]+");
        double sum = 0; int count = 0;
        for (String part : parts) {
            try {
                sum += Double.parseDouble(part.trim().replace(",", "."));
                count++;
            } catch (NumberFormatException ignored) {}
        }
        return count > 0 ? sum / count : 0.0;
    }

    public static void calculateAndFill(TestResult result) {
        processAndCalculate(result);
    }
}
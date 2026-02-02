package utils;

import data.TestResult;

public class CalculationHelper {
    // Constant values for the test rig
    private static final double G = 9.81;
    private static final double LEVER_HANG = 0.875; // meters
    private static final double LEVER_TIRE = 0.358; // meters

    /**
     * Calculates Crr and updates the TestResult object.
     */
    public static void calculateAndFill(TestResult res) {
        // 1. Convert lever mass to effective mass on tire
        double effectiveMassOnTire = (res.massKg * LEVER_HANG) / LEVER_TIRE;

        // 2. Convert speed from km/h to m/s
        double speedMs = res.speedKmh / 3.6;

        // 3. Power loss (Prr) = P_loaded - P_idle
        double powerLossRR = res.pLoadedW - res.p0W;

        // 4. Crr Calculation: Prr / (m_eff * g * v)
        if (effectiveMassOnTire > 0 && speedMs > 0) {
            res.calculatedCrr = powerLossRR / (effectiveMassOnTire * G * speedMs);
        } else {
            res.calculatedCrr = 0.0;
        }
    }
}
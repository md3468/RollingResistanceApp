package utils;

import data.TestResult;

public class CalculationHelper {
    private static final double G = 9.81;
    private static final double LEVER_HANG = 0.875;
    private static final double LEVER_TIRE = 0.358;

    public static void processAndCalculate(TestResult res) {
        // Schritt 1: Falls ESP-Daten genutzt werden, Leistung berechnen
        if (!res.isManualInput) {
            res.I0A = res.voltageSystem * res.idleCurrentAmp;
            res.ILoadedA = res.voltageSystem * res.loadCurrentAmp;
        }

        // Schritt 2: Crr Physik-Logik
        double effectiveMassOnTire = (res.massKg * LEVER_HANG) / LEVER_TIRE;
        double speedMs = res.speedKmh / 3.6;
        double powerLossRR = res.ILoadedA - res.I0A;

        if (effectiveMassOnTire > 0 && speedMs > 0) {
            res.calculatedCrr = powerLossRR / (effectiveMassOnTire * G * speedMs);
        }
    }

    public static void calculateAndFill(TestResult result) {

    }
}
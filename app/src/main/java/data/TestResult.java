package data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "test_results")
public class TestResult {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int userId;
    public String tireName;
    public double pressureBar;
    public double temperatureC;
    public double speedKmh;
    public double massKg;
    public double voltageSystem;
    public double idleCurrentAmp;
    public double loadCurrentAmp;
    public double p0W;
    public double pLoadedW;
    public boolean isTubeless;
    public boolean isTempStable;
    public boolean isPressureChecked;
    public boolean isManualInput;
    public double calculatedCrr;
    public long timestamp;
    public TestResult() {
        this.timestamp = System.currentTimeMillis();
    }
}
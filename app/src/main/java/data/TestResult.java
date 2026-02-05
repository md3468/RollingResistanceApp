package data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "test_results")
public class TestResult {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    public int id;

    @ColumnInfo(name = "User_ID")
    public int userId;

    @ColumnInfo(name = "Tire_Name/Size")
    public String tireName;

    @ColumnInfo(name = "Pressure_[Bar]")
    public double pressureBar;

    @ColumnInfo(name = "Temperature")
    public double temperatureC;

    @ColumnInfo(name = "Speed_[Km/h]")
    public double speedKmh;

    @ColumnInfo(name = "Speed_[RPM]")
    public double etSpeedrpm;

    @ColumnInfo(name = "Tubeless")
    public boolean isTubeless;

    @ColumnInfo(name = "Temperature_Stable")
    public boolean isTempStable;

    @ColumnInfo(name = "Pressure_Checked")
    public boolean isPressureChecked;
    
    @ColumnInfo(name = "Weight_on_Lever")
    public double massKg;        // WeightOnLever
    
    @ColumnInfo(name = "Weight_on_Tire")
    public double weightOnTire;  // WeightOnTire (calculated mEff)
    
    @ColumnInfo(name = "Idel_Current")
    public double idleCurrentAmp; // IdelCurrent (single reading or raw)
    
    @ColumnInfo(name = "Loaded_Current")
    public double loadCurrentAmp; // LoadedCurrent (single reading or raw)
    
    @ColumnInfo(name = "Mean_Idel_Current")
    public double I0A;            // MeanIdelCurrent
    
    @ColumnInfo(name = "Mean_Loaded_Current")
    public double ILoadedA;       // MeanLoadedCurrent
    
    @ColumnInfo(name = "Power_[P0]")
    public double powerP0;        // PowerP0
    
    @ColumnInfo(name = "Power_[PLoad]")
    public double powerPLoad;     // PowerPLoad
    
    @ColumnInfo(name = "Prr")
    public double pRR;            // Prr
    
    @ColumnInfo(name = "Crr")
    public double calculatedCrr;  // Crr

    @ColumnInfo(name = "Timestamp")
    public long timestamp;

    public TestResult() {
        this.timestamp = System.currentTimeMillis();
    }
}

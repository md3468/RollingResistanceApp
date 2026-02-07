package data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TestDao {
    @Insert
    long insertUser(User user);

    @Query("SELECT * FROM users WHERE username = :name LIMIT 1")
    User getUserByName(String name);

    @Insert
    void insertResult(TestResult result);

    @Delete
    void deleteResult(TestResult result);

    @Query("SELECT * FROM test_results WHERE User_ID = :uId ORDER BY timestamp DESC")
    List<TestResult> getResultsForUser(int uId);

    @Query("SELECT DISTINCT `Tire_Name/Size` FROM test_results WHERE User_ID = :uId")
    List<String> getAllUniqueTires(int uId);

    @Query("SELECT * FROM test_results WHERE `Tire_Name/Size` = :tName AND User_ID = :uId ORDER BY `Pressure_[Bar]` ASC, `Speed_[Km/h]` ASC")
    List<TestResult> getResultsForTire(String tName, int uId);

    @Query("SELECT * FROM test_results WHERE User_ID = :uId ORDER BY timestamp DESC")
    List<TestResult> getAllResults(int uId);
    @Query("SELECT DISTINCT `Tire_Name/Size` FROM test_results ORDER BY `Tire_Name/Size` ASC")
    List<String> getAllUniqueTireNames();
}

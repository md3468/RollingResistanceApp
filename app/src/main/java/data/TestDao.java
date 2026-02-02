package data;

import androidx.room.Dao;
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

    @Query("SELECT * FROM test_results WHERE userId = :uId ORDER BY timestamp DESC")
    List<TestResult> getResultsForUser(int uId);
}
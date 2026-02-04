package com.coolapp.assignment1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import data.AppDatabase;
import data.TestResult;

public class ListActivity extends AppCompatActivity implements TestResultAdapter.OnItemDeleteListener {

    private TestResultAdapter adapter;
    private AppDatabase db;
    private RecyclerView recyclerView;
    private int swipedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list);

        // Matching the inset logic from MainActivity exactly
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.list_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_back_to_main).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_test_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new TestResultAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        db = AppDatabase.getDatabase(this);
        loadData();

        setupSwipeToReveal();
    }

    private void setupSwipeToReveal() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final float snapWidth = -250f;

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {}

            @Override
            public int getMovementFlags(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder vh) { return Float.MAX_VALUE; }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) { return Float.MAX_VALUE; }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View foregroundView = ((TestResultAdapter.ViewHolder) vh).viewForeground;
                int pos = vh.getBindingAdapterPosition();
                
                float translationX = dX;
                if (swipedPosition == pos) {
                    translationX = Math.min(dX + snapWidth, 0);
                    if (dX > -snapWidth / 2) {
                        swipedPosition = -1;
                        translationX = 0;
                    }
                } else if (dX < snapWidth) {
                    translationX = snapWidth;
                    if (!isCurrentlyActive) swipedPosition = pos;
                }

                getDefaultUIUtil().onDraw(c, rv, foregroundView, translationX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                View foregroundView = ((TestResultAdapter.ViewHolder) vh).viewForeground;
                if (swipedPosition == vh.getBindingAdapterPosition()) {
                    foregroundView.setTranslationX(snapWidth);
                } else {
                    getDefaultUIUtil().clearView(foregroundView);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void loadData() {
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            List<TestResult> results = db.testDao().getAllResults(prefs.getInt("currentUserId", -1));
            runOnUiThread(() -> adapter.setResults(results));
        }).start();
    }

    @Override
    public void onItemDelete(int position) {
        new Thread(() -> {
            db.testDao().deleteResult(adapter.getResultAt(position));
            runOnUiThread(() -> {
                swipedPosition = -1;
                adapter.removeResult(position);
            });
        }).start();
    }
}

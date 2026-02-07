package com.coolapp.assignment1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import data.TestResult;

public class TestResultAdapter extends RecyclerView.Adapter<TestResultAdapter.ViewHolder> {

    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    private List<TestResult> results;
    private final OnItemDeleteListener deleteListener;
    // Speichert die IDs der ausgewählten Items
    private final Set<Integer> selectedIds = new HashSet<>();

    public TestResultAdapter(List<TestResult> results, OnItemDeleteListener deleteListener) {
        this.results = results;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TestResult result = results.get(position);
        holder.tvTireName.setText(result.tireName);
        holder.tvCrr.setText(String.format(Locale.getDefault(), "Crr: %.6f", result.calculatedCrr));
        holder.tvDetails.setText(String.format(Locale.getDefault(), "%.2f bar | %.2f km/h", result.pressureBar, result.speedKmh));

        // Checkbox Zustand setzen (verhindert Fehler beim Scrollen)
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedIds.contains(result.id));

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIds.add(result.id);
            } else {
                selectedIds.remove(result.id);
            }
        });

        holder.btnDeleteItem.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onItemDelete(holder.getBindingAdapterPosition());
            }
        });
    }

    // --- Hilfsmethode für die ListActivity ---
    public List<TestResult> getSelectedResults() {
        List<TestResult> selectedList = new ArrayList<>();
        for (TestResult res : results) {
            if (selectedIds.contains(res.id)) {
                selectedList.add(res);
            }
        }
        return selectedList;
    }

    @Override
    public int getItemCount() {
        return results != null ? results.size() : 0;
    }

    public void setResults(List<TestResult> results) {
        this.results = results;
        notifyDataSetChanged();
    }

    public TestResult getResultAt(int position) {
        return results.get(position);
    }

    public void removeResult(int position) {
        results.remove(position);
        notifyItemRemoved(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTireName, tvCrr, tvDetails;
        CheckBox cbSelect;
        public final View viewForeground;
        final ImageButton btnDeleteItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTireName = itemView.findViewById(R.id.tv_item_tire_name);
            tvCrr = itemView.findViewById(R.id.tv_item_crr);
            tvDetails = itemView.findViewById(R.id.tv_item_details);
            cbSelect = itemView.findViewById(R.id.cb_select_item);
            viewForeground = itemView.findViewById(R.id.view_foreground);
            btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}
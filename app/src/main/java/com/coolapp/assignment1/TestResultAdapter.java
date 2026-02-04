package com.coolapp.assignment1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;
import data.TestResult;

public class TestResultAdapter extends RecyclerView.Adapter<TestResultAdapter.ViewHolder> {

    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    private List<TestResult> results;
    private final OnItemDeleteListener deleteListener;

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
        holder.tvCrr.setText(String.format(Locale.getDefault(), "Crr: %.5f", result.calculatedCrr));
        holder.tvDetails.setText(String.format(Locale.getDefault(), "%.1f bar | %.1f km/h", result.pressureBar, result.speedKmh));

        holder.btnDeleteItem.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onItemDelete(holder.getBindingAdapterPosition());
            }
        });
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
        public final View viewForeground;
        final ImageButton btnDeleteItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTireName = itemView.findViewById(R.id.tv_item_tire_name);
            tvCrr = itemView.findViewById(R.id.tv_item_crr);
            tvDetails = itemView.findViewById(R.id.tv_item_details);
            viewForeground = itemView.findViewById(R.id.view_foreground);
            btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}

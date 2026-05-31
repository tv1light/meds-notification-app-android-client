package com.medreminder.ui.intake_log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medreminder.R;
import com.medreminder.data.local.entity.IntakeLogWithDrug;
import com.medreminder.util.DateTimeUtils;
import com.medreminder.util.StatusUiUtils;

import java.util.ArrayList;
import java.util.List;

public class IntakeLogAdapter extends RecyclerView.Adapter<IntakeLogAdapter.LogVH> {
    private final List<IntakeLogWithDrug> items = new ArrayList<>();

    public void submit(List<IntakeLogWithDrug> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intake_log, parent, false);
        return new LogVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogVH holder, int position) {
        IntakeLogWithDrug item = items.get(position);
        holder.tvLogDrug.setText(item.drugName == null ? "Препарат" : item.drugName);
        holder.tvLogPlanned.setText(holder.itemView.getContext().getString(R.string.planned_at, DateTimeUtils.formatDateTime(item.plannedDateTime)));
        String actualText = item.actualDateTime == null ? "-" : DateTimeUtils.formatDateTime(item.actualDateTime);
        holder.tvLogActual.setText(holder.itemView.getContext().getString(R.string.actual_at, actualText));
        holder.tvLogStatus.setText(StatusUiUtils.labelForStatus(item.status));
        holder.tvLogStatus.setTextColor(StatusUiUtils.colorForStatus(holder.itemView.getContext(), item.status));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LogVH extends RecyclerView.ViewHolder {
        final TextView tvLogDrug;
        final TextView tvLogPlanned;
        final TextView tvLogActual;
        final TextView tvLogStatus;

        LogVH(@NonNull View itemView) {
            super(itemView);
            tvLogDrug = itemView.findViewById(R.id.tvLogDrug);
            tvLogPlanned = itemView.findViewById(R.id.tvLogPlanned);
            tvLogActual = itemView.findViewById(R.id.tvLogActual);
            tvLogStatus = itemView.findViewById(R.id.tvLogStatus);
        }
    }
}

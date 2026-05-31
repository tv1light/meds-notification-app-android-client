package com.medreminder.ui.intake_log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
        List<IntakeLogWithDrug> newItems = list == null ? new ArrayList<>() : new ArrayList<>(list);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).id == newItems.get(newItemPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                IntakeLogWithDrug oldItem = items.get(oldItemPosition);
                IntakeLogWithDrug newItem = newItems.get(newItemPosition);
                return oldItem.courseId == newItem.courseId
                        && oldItem.drugId == newItem.drugId
                        && oldItem.plannedDateTime == newItem.plannedDateTime
                        && equalsNullableLong(oldItem.actualDateTime, newItem.actualDateTime)
                        && equalsNullable(oldItem.status, newItem.status)
                        && equalsNullable(oldItem.comment, newItem.comment)
                        && equalsNullable(oldItem.drugName, newItem.drugName);
            }
        });

        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
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
        holder.tvLogDrug.setText(item.drugName == null ? holder.itemView.getContext().getString(R.string.fallback_drug_name) : item.drugName);
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

    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private boolean equalsNullableLong(Long left, Long right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}

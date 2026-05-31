package com.medreminder.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.medreminder.R;
import com.medreminder.data.local.entity.ReminderWithCourseDrug;
import com.medreminder.util.Constants;
import com.medreminder.util.DateTimeUtils;
import com.medreminder.util.StatusUiUtils;

import java.util.ArrayList;
import java.util.List;

public class TodayReminderAdapter extends RecyclerView.Adapter<TodayReminderAdapter.TodayVH> {
    public interface ActionListener {
        void onTaken(ReminderWithCourseDrug item);

        void onUndoTaken(ReminderWithCourseDrug item);

        void onSkip(ReminderWithCourseDrug item);

        void onPostpone(ReminderWithCourseDrug item);
    }

    private final List<ReminderWithCourseDrug> items = new ArrayList<>();
    private final ActionListener listener;

    public TodayReminderAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void submit(List<ReminderWithCourseDrug> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TodayVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_today_reminder, parent, false);
        return new TodayVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodayVH holder, int position) {
        ReminderWithCourseDrug item = items.get(position);
        holder.tvDrugName.setText(item.drugName);
        holder.tvDosage.setText(item.dosageText);
        holder.tvTime.setText(DateTimeUtils.formatTime(item.plannedDateTime));
        holder.tvStatus.setText(StatusUiUtils.labelForStatus(item.status));
        holder.tvStatus.setTextColor(StatusUiUtils.colorForStatus(holder.itemView.getContext(), item.status));

        boolean isPlanned = Constants.STATUS_PLANNED.equals(item.status);
        boolean isTaken = Constants.STATUS_TAKEN.equals(item.status);

        holder.btnSkip.setVisibility(isTaken ? View.GONE : View.VISIBLE);
        holder.btnPostpone.setVisibility(isTaken ? View.GONE : View.VISIBLE);

        if (isTaken) {
            holder.btnTaken.setEnabled(true);
            holder.btnTaken.setText(R.string.action_cancel_taken);
            holder.btnTaken.setOnClickListener(v -> listener.onUndoTaken(item));
            holder.btnSkip.setOnClickListener(null);
            holder.btnPostpone.setOnClickListener(null);
        } else {
            holder.btnTaken.setText(R.string.action_taken);
            holder.btnTaken.setEnabled(isPlanned);
            holder.btnSkip.setEnabled(isPlanned);
            holder.btnPostpone.setEnabled(isPlanned);

            holder.btnTaken.setOnClickListener(v -> listener.onTaken(item));
            holder.btnSkip.setOnClickListener(v -> listener.onSkip(item));
            holder.btnPostpone.setOnClickListener(v -> listener.onPostpone(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TodayVH extends RecyclerView.ViewHolder {
        final TextView tvDrugName;
        final TextView tvDosage;
        final TextView tvTime;
        final TextView tvStatus;
        final MaterialButton btnTaken;
        final MaterialButton btnSkip;
        final MaterialButton btnPostpone;

        TodayVH(@NonNull View itemView) {
            super(itemView);
            tvDrugName = itemView.findViewById(R.id.tvDrugName);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnTaken = itemView.findViewById(R.id.btnTaken);
            btnSkip = itemView.findViewById(R.id.btnSkip);
            btnPostpone = itemView.findViewById(R.id.btnPostpone);
        }
    }
}

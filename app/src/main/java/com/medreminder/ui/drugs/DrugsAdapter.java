package com.medreminder.ui.drugs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medreminder.R;
import com.medreminder.data.local.entity.DrugEntity;

import java.util.ArrayList;
import java.util.List;

public class DrugsAdapter extends RecyclerView.Adapter<DrugsAdapter.DrugVH> {
    private final List<DrugEntity> items = new ArrayList<>();

    public void submit(List<DrugEntity> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DrugVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drug, parent, false);
        return new DrugVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DrugVH holder, int position) {
        DrugEntity item = items.get(position);
        holder.tvDrugName.setText(item.name);
        holder.tvDrugDetails.setText(item.form + " • " + item.dosage + " • " + item.activeSubstance);
        holder.tvDrugProducer.setText(item.country + " • " + item.manufacturer);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DrugVH extends RecyclerView.ViewHolder {
        final TextView tvDrugName;
        final TextView tvDrugDetails;
        final TextView tvDrugProducer;

        DrugVH(@NonNull View itemView) {
            super(itemView);
            tvDrugName = itemView.findViewById(R.id.tvDrugName);
            tvDrugDetails = itemView.findViewById(R.id.tvDrugDetails);
            tvDrugProducer = itemView.findViewById(R.id.tvDrugProducer);
        }
    }
}

package com.medreminder.ui.drugs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.medreminder.R;
import com.medreminder.data.local.entity.DrugEntity;

import java.util.ArrayList;
import java.util.List;

public class DrugsAdapter extends RecyclerView.Adapter<DrugsAdapter.DrugVH> {
    private final List<DrugEntity> items = new ArrayList<>();

    public void submit(List<DrugEntity> list) {
        List<DrugEntity> newItems = list == null ? new ArrayList<>() : new ArrayList<>(list);
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
                DrugEntity oldItem = items.get(oldItemPosition);
                DrugEntity newItem = newItems.get(newItemPosition);
                return oldItem.serverId == newItem.serverId
                        && oldItem.updatedAt == newItem.updatedAt
                        && equalsNullable(oldItem.name, newItem.name)
                        && equalsNullable(oldItem.form, newItem.form)
                        && equalsNullable(oldItem.dosage, newItem.dosage)
                        && equalsNullable(oldItem.activeSubstance, newItem.activeSubstance)
                        && equalsNullable(oldItem.country, newItem.country)
                        && equalsNullable(oldItem.manufacturer, newItem.manufacturer);
            }
        });

        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
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
        holder.tvDrugDetails.setText(holder.itemView.getContext().getString(
                R.string.drug_details_template,
                item.form,
                item.dosage,
                item.activeSubstance
        ));
        holder.tvDrugProducer.setText(holder.itemView.getContext().getString(
                R.string.drug_producer_template,
                item.country,
                item.manufacturer
        ));
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

    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}

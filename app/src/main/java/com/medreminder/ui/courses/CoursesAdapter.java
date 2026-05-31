package com.medreminder.ui.courses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medreminder.R;
import com.medreminder.data.local.entity.CourseWithDrug;
import com.medreminder.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class CoursesAdapter extends RecyclerView.Adapter<CoursesAdapter.CourseVH> {
    public interface ClickListener {
        void onClick(CourseWithDrug course);
    }

    private final List<CourseWithDrug> items = new ArrayList<>();
    private final ClickListener clickListener;

    public CoursesAdapter(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submit(List<CourseWithDrug> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new CourseVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseVH holder, int position) {
        CourseWithDrug item = items.get(position);
        holder.tvCourseDrug.setText(item.drugName);
        String info = item.dosageText + " • " + DateTimeUtils.formatDate(item.startDate) + " - " + DateTimeUtils.formatDate(item.endDate);
        holder.tvCourseInfo.setText(info);
        boolean active = item.endDate >= DateTimeUtils.startOfDay(System.currentTimeMillis());
        holder.tvCourseState.setText(active ? "Активный" : "Завершён");
        holder.itemView.setOnClickListener(v -> clickListener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CourseVH extends RecyclerView.ViewHolder {
        final TextView tvCourseDrug;
        final TextView tvCourseInfo;
        final TextView tvCourseState;

        CourseVH(@NonNull View itemView) {
            super(itemView);
            tvCourseDrug = itemView.findViewById(R.id.tvCourseDrug);
            tvCourseInfo = itemView.findViewById(R.id.tvCourseInfo);
            tvCourseState = itemView.findViewById(R.id.tvCourseState);
        }
    }
}

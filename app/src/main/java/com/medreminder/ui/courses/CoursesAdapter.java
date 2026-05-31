package com.medreminder.ui.courses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
        List<CourseWithDrug> newItems = data == null ? new ArrayList<>() : new ArrayList<>(data);
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
                return items.get(oldItemPosition).courseId == newItems.get(newItemPosition).courseId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                CourseWithDrug oldItem = items.get(oldItemPosition);
                CourseWithDrug newItem = newItems.get(newItemPosition);
                return oldItem.drugId == newItem.drugId
                        && oldItem.startDate == newItem.startDate
                        && oldItem.endDate == newItem.endDate
                        && equalsNullable(oldItem.drugName, newItem.drugName)
                        && equalsNullable(oldItem.dosageText, newItem.dosageText)
                        && equalsNullable(oldItem.scheduleType, newItem.scheduleType)
                        && oldItem.isActive == newItem.isActive;
            }
        });

        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
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
        holder.tvCourseInfo.setText(holder.itemView.getContext().getString(
                R.string.course_info_template,
                item.dosageText,
                DateTimeUtils.formatDate(item.startDate),
                DateTimeUtils.formatDate(item.endDate)
        ));
        boolean active = item.endDate >= DateTimeUtils.startOfDay(System.currentTimeMillis());
        holder.tvCourseState.setText(active ? R.string.course_active : R.string.course_finished);
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

    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}

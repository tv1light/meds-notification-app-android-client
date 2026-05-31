package com.medreminder.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.medreminder.R;
import com.medreminder.data.local.entity.ReminderWithCourseDrug;
import com.medreminder.data.repository.ReminderRepository;
import com.medreminder.ui.schedule.PostponeActivity;
import com.medreminder.util.DateTimeUtils;

import java.util.List;

public class TodayFragment extends Fragment implements TodayReminderAdapter.ActionListener {
    private ReminderRepository reminderRepository;
    private TodayReminderAdapter adapter;
    private LinearLayout emptyState;

    private final ActivityResultLauncher<Intent> postponeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                Intent data = result.getData();
                long reminderId = data.getLongExtra("reminder_id", -1L);
                long newTime = data.getLongExtra("new_time", -1L);
                if (reminderId > 0 && newTime > 0) {
                    reminderRepository.postponeToTime(reminderId, newTime, null);
                }
            }
    );

    public TodayFragment() {
        super(R.layout.fragment_today);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reminderRepository = new ReminderRepository(requireContext());

        TextView tvDate = view.findViewById(R.id.tvTodayDate);
        tvDate.setText(DateTimeUtils.formatDate(System.currentTimeMillis()));

        emptyState = view.findViewById(R.id.emptyState);
        RecyclerView recyclerView = view.findViewById(R.id.rvToday);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TodayReminderAdapter(this);
        recyclerView.setAdapter(adapter);

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.swipeRefresh);
        refreshLayout.setOnRefreshListener(() -> refreshLayout.setRefreshing(false));

        reminderRepository.observeDay(System.currentTimeMillis()).observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<ReminderWithCourseDrug> reminders) {
        adapter.submit(reminders);
        boolean isEmpty = reminders == null || reminders.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTaken(ReminderWithCourseDrug item) {
        reminderRepository.markTaken(item.reminderId, () -> toast(getString(R.string.status_taken)));
    }

    @Override
    public void onUndoTaken(ReminderWithCourseDrug item) {
        reminderRepository.undoTaken(item.reminderId, () -> toast(getString(R.string.taken_canceled)));
    }

    @Override
    public void onSkip(ReminderWithCourseDrug item) {
        reminderRepository.markSkipped(item.reminderId, () -> toast(getString(R.string.status_skipped)));
    }

    @Override
    public void onPostpone(ReminderWithCourseDrug item) {
        Intent intent = new Intent(requireContext(), PostponeActivity.class);
        intent.putExtra("reminder_id", item.reminderId);
        intent.putExtra("planned_time", item.plannedDateTime);
        postponeLauncher.launch(intent);
    }

    private void toast(String text) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
    }
}

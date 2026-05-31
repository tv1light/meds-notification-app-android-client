package com.medreminder.ui.main;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.medreminder.R;
import com.medreminder.data.local.entity.ReminderWithCourseDrug;
import com.medreminder.data.repository.ReminderRepository;
import com.medreminder.ui.schedule.PostponeActivity;
import com.medreminder.util.DateTimeUtils;

import java.util.Calendar;
import java.util.List;

public class TodayFragment extends Fragment implements TodayReminderAdapter.ActionListener {
    private static final String STATE_SELECTED_DAY = "state_selected_day";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private ReminderRepository reminderRepository;
    private TodayReminderAdapter adapter;
    private TextView tvDate;
    private TextView emptyState;
    private View btnGoToday;
    private SwipeRefreshLayout swipeRefresh;

    private LiveData<List<ReminderWithCourseDrug>> daySource;
    private long selectedDayMillis;

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
        long todayStart = DateTimeUtils.startOfDay(System.currentTimeMillis());
        selectedDayMillis = savedInstanceState == null
                ? todayStart
                : DateTimeUtils.startOfDay(savedInstanceState.getLong(STATE_SELECTED_DAY, todayStart));

        tvDate = view.findViewById(R.id.tvTodayDate);
        emptyState = view.findViewById(R.id.emptyState);
        btnGoToday = view.findViewById(R.id.btnGoToday);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        RecyclerView recyclerView = view.findViewById(R.id.rvToday);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TodayReminderAdapter(this);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btnPrevDay).setOnClickListener(v -> selectDay(selectedDayMillis - DAY_MS));
        view.findViewById(R.id.btnNextDay).setOnClickListener(v -> selectDay(selectedDayMillis + DAY_MS));
        tvDate.setOnClickListener(v -> showDatePicker());
        btnGoToday.setOnClickListener(v -> selectDay(System.currentTimeMillis()));

        swipeRefresh.setOnRefreshListener(() -> {
            observeSelectedDay();
            swipeRefresh.setRefreshing(false);
        });

        updateHeader();
        observeSelectedDay();
    }

    private void render(List<ReminderWithCourseDrug> reminders) {
        adapter.submit(reminders);
        boolean isEmpty = reminders == null || reminders.isEmpty();
        if (isEmpty) {
            if (isTodaySelected()) {
                emptyState.setText(R.string.empty_today);
            } else {
                emptyState.setText(getString(R.string.empty_day, DateTimeUtils.formatDate(selectedDayMillis)));
            }
        }
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

    private void selectDay(long dayMillis) {
        selectedDayMillis = DateTimeUtils.startOfDay(dayMillis);
        updateHeader();
        observeSelectedDay();
    }

    private void observeSelectedDay() {
        if (daySource != null) {
            daySource.removeObservers(getViewLifecycleOwner());
        }
        daySource = reminderRepository.observeDay(selectedDayMillis);
        daySource.observe(getViewLifecycleOwner(), this::render);
    }

    private void updateHeader() {
        if (isTodaySelected()) {
            tvDate.setText(getString(R.string.today_with_date, DateTimeUtils.formatDate(selectedDayMillis)));
            btnGoToday.setVisibility(View.GONE);
        } else {
            tvDate.setText(DateTimeUtils.formatDate(selectedDayMillis));
            btnGoToday.setVisibility(View.VISIBLE);
        }
    }

    private boolean isTodaySelected() {
        long today = DateTimeUtils.startOfDay(System.currentTimeMillis());
        return today == selectedDayMillis;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDayMillis);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectDay(selected.getTimeInMillis());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_DAY, selectedDayMillis);
    }
}

package com.medreminder.ui.intake_log;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.medreminder.R;
import com.medreminder.data.local.entity.IntakeLogWithDrug;
import com.medreminder.data.repository.IntakeLogRepository;
import com.medreminder.util.Constants;
import com.medreminder.util.DateTimeUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class IntakeLogFragment extends Fragment {
    private IntakeLogRepository repository;
    private IntakeLogAdapter adapter;

    private EditText etDate;
    private EditText etDrug;
    private TextView tvStats;
    private TextView tvEmpty;

    private Long selectedDay;
    private LiveData<List<IntakeLogWithDrug>> source;

    public IntakeLogFragment() {
        super(R.layout.fragment_intake_log);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new IntakeLogRepository(requireContext());

        etDate = view.findViewById(R.id.etFilterDate);
        etDrug = view.findViewById(R.id.etFilterDrug);
        tvStats = view.findViewById(R.id.tvStats);
        tvEmpty = view.findViewById(R.id.tvEmptyLogs);

        RecyclerView recyclerView = view.findViewById(R.id.rvLogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new IntakeLogAdapter();
        recyclerView.setAdapter(adapter);

        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setOnLongClickListener(v -> {
            selectedDay = null;
            etDate.setText("");
            subscribe();
            return true;
        });

        etDrug.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                subscribe();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        subscribe();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDay != null) {
            calendar.setTimeInMillis(selectedDay);
        }
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    selectedDay = selected.getTimeInMillis();
                    etDate.setText(DateTimeUtils.formatDate(selectedDay));
                    subscribe();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void subscribe() {
        String query = etDrug.getText() == null ? "" : etDrug.getText().toString().trim();
        if (source != null) {
            source.removeObservers(getViewLifecycleOwner());
        }
        source = repository.observe(selectedDay, query);
        source.observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<IntakeLogWithDrug> logs) {
        adapter.submit(logs);
        boolean empty = logs == null || logs.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        int total = logs == null ? 0 : logs.size();
        int taken = 0;
        int skipped = 0;
        if (logs != null) {
            for (IntakeLogWithDrug item : logs) {
                if (Constants.STATUS_TAKEN.equals(item.status)) {
                    taken++;
                }
                if (Constants.STATUS_SKIPPED.equals(item.status)) {
                    skipped++;
                }
            }
        }
        String compliance = total == 0 ? "0" : String.format(Locale.getDefault(), "%.1f", (taken * 100f / total));
        tvStats.setText(getString(R.string.stats_template, total, taken, skipped, compliance));
    }
}

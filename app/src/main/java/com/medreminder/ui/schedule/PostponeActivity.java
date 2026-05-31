package com.medreminder.ui.schedule;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.medreminder.R;
import com.medreminder.util.DateTimeUtils;

import java.util.Calendar;

public class PostponeActivity extends AppCompatActivity {
    private long reminderId;
    private long plannedTime;
    private long selectedDateStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postpone);

        reminderId = getIntent().getLongExtra("reminder_id", -1L);
        plannedTime = getIntent().getLongExtra("planned_time", System.currentTimeMillis());
        selectedDateStart = DateTimeUtils.startOfDay(plannedTime);

        EditText etDate = findViewById(R.id.etPostponeDate);
        Button btnPickDate = findViewById(R.id.btnPickDate);
        etDate.setText(DateTimeUtils.formatDate(selectedDateStart));
        etDate.setOnClickListener(v -> showDatePicker(etDate));
        btnPickDate.setOnClickListener(v -> showDatePicker(etDate));

        TimePicker timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(plannedTime);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
            timePicker.setMinute(calendar.get(Calendar.MINUTE));
        } else {
            timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
            timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
        }

        Button btnSave = findViewById(R.id.btnSavePostpone);
        btnSave.setOnClickListener(v -> {
            int hour;
            int minute;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = timePicker.getHour();
                minute = timePicker.getMinute();
            } else {
                hour = timePicker.getCurrentHour();
                minute = timePicker.getCurrentMinute();
            }

            Calendar target = Calendar.getInstance();
            target.setTimeInMillis(selectedDateStart);
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            long newTime = target.getTimeInMillis();

            Intent data = new Intent();
            data.putExtra("reminder_id", reminderId);
            data.putExtra("new_time", newTime);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    private void showDatePicker(EditText dateField) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateStart);
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDateStart = DateTimeUtils.startOfDay(selected.getTimeInMillis());
                    dateField.setText(DateTimeUtils.formatDate(selectedDateStart));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
}

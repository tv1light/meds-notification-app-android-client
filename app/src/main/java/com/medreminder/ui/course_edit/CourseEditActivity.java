package com.medreminder.ui.course_edit;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.medreminder.R;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.local.entity.TherapyCourseEntity;
import com.medreminder.data.repository.CourseRepository;
import com.medreminder.data.repository.DrugRepository;
import com.medreminder.data.repository.RepositoryCallback;
import com.medreminder.util.AppExecutors;
import com.medreminder.util.Constants;
import com.medreminder.util.DateTimeUtils;
import com.medreminder.util.ValidationUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CourseEditActivity extends AppCompatActivity {
    private AutoCompleteTextView etDrug;
    private EditText etDosage;
    private EditText etStartDate;
    private EditText etEndDate;
    private EditText etTimes;
    private EditText etInterval;
    private EditText etNotifyBefore;
    private Spinner spNotificationType;
    private Button btnSave;
    private Button btnDelete;

    private final List<DrugEntity> drugCatalog = new ArrayList<>();
    private final List<Integer> selectedTimesMinutes = new ArrayList<>();

    private DrugRepository drugRepository;
    private CourseRepository courseRepository;

    private long selectedDrugId = -1L;
    private long startDateMillis = DateTimeUtils.startOfDay(System.currentTimeMillis());
    private long endDateMillis = startDateMillis;
    private int selectedIntervalMinutes = 0;
    private int selectedNotifyBeforeMinutes = 0;
    private TherapyCourseEntity editingCourse;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_edit);

        initViews();

        drugRepository = new DrugRepository(this);
        courseRepository = new CourseRepository(this);

        setupSpinners();
        setupDateFields();
        setupTimeControls();
        loadDrugDictionary();

        long courseId = getIntent().getLongExtra("course_id", -1L);
        if (courseId > 0) {
            btnDelete.setVisibility(View.VISIBLE);
            loadCourse(courseId);
        } else {
            selectedTimesMinutes.clear();
            selectedTimesMinutes.add(8 * 60);
            selectedTimesMinutes.add(20 * 60);
            updateTimesView();
            updateIntervalView();
            updateNotifyBeforeView();
        }

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveCourse());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void initViews() {
        etDrug = findViewById(R.id.etDrug);
        etDosage = findViewById(R.id.etDosage);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etTimes = findViewById(R.id.etTimes);
        etInterval = findViewById(R.id.etInterval);
        etNotifyBefore = findViewById(R.id.etNotifyBefore);
        spNotificationType = findViewById(R.id.spNotificationType);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDeleteCourse);

        etStartDate.setText(DateTimeUtils.formatDate(startDateMillis));
        etEndDate.setText(DateTimeUtils.formatDate(endDateMillis));

        etDrug.setOnClickListener(v -> etDrug.showDropDown());
        etDrug.setOnItemClickListener((parent, view, position, id) -> {
            String name = parent.getItemAtPosition(position).toString();
            DrugEntity drug = findDrugByName(name);
            if (drug != null) {
                applySelectedDrug(drug);
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> notificationAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.notification_type_labels,
                android.R.layout.simple_spinner_item
        );
        notificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNotificationType.setAdapter(notificationAdapter);
    }

    private void setupDateFields() {
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupTimeControls() {
        Button btnAddTime = findViewById(R.id.btnAddTime);
        Button btnClearTimes = findViewById(R.id.btnClearTimes);
        Button btnConfigureInterval = findViewById(R.id.btnConfigureInterval);
        Button btnClearInterval = findViewById(R.id.btnClearInterval);
        Button btnChooseDrug = findViewById(R.id.btnChooseDrug);
        Button btnConfigureNotifyBefore = findViewById(R.id.btnConfigureNotifyBefore);
        Button btnClearNotifyBefore = findViewById(R.id.btnClearNotifyBefore);

        etTimes.setOnClickListener(v -> openAddTimePicker());
        etInterval.setOnClickListener(v -> openIntervalPicker());
        etNotifyBefore.setOnClickListener(v -> openNotifyBeforePicker());

        btnAddTime.setOnClickListener(v -> openAddTimePicker());
        btnClearTimes.setOnClickListener(v -> {
            selectedTimesMinutes.clear();
            updateTimesView();
        });

        btnConfigureInterval.setOnClickListener(v -> openIntervalPicker());
        btnClearInterval.setOnClickListener(v -> {
            selectedIntervalMinutes = 0;
            updateIntervalView();
        });

        btnChooseDrug.setOnClickListener(v -> openDrugPickerDialog());

        btnConfigureNotifyBefore.setOnClickListener(v -> openNotifyBeforePicker());
        btnClearNotifyBefore.setOnClickListener(v -> {
            selectedNotifyBeforeMinutes = 0;
            updateNotifyBeforeView();
        });
    }

    private void showDatePicker(boolean start) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start ? startDateMillis : endDateMillis);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    long value = DateTimeUtils.startOfDay(selected.getTimeInMillis());
                    if (start) {
                        startDateMillis = value;
                        etStartDate.setText(DateTimeUtils.formatDate(value));
                        if (endDateMillis < startDateMillis) {
                            endDateMillis = startDateMillis;
                            etEndDate.setText(DateTimeUtils.formatDate(endDateMillis));
                        }
                    } else {
                        endDateMillis = value;
                        etEndDate.setText(DateTimeUtils.formatDate(value));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void openAddTimePicker() {
        int initial = selectedTimesMinutes.isEmpty() ? 8 * 60 : selectedTimesMinutes.get(selectedTimesMinutes.size() - 1);
        showWheelTimePicker(initial, selectedMinutes -> {
            if (selectedTimesMinutes.contains(selectedMinutes)) {
                toast("Это время уже добавлено");
                return;
            }
            selectedTimesMinutes.add(selectedMinutes);
            Collections.sort(selectedTimesMinutes);
            updateTimesView();
        });
    }

    private void openIntervalPicker() {
        int initial = Math.max(0, selectedIntervalMinutes);
        showWheelTimePicker(initial, selectedMinutes -> {
            selectedIntervalMinutes = selectedMinutes;
            updateIntervalView();
        });
    }

    private void openNotifyBeforePicker() {
        String[] minuteItems = new String[16];
        for (int i = 0; i <= 15; i++) {
            minuteItems[i] = i + " мин";
        }
        int checked = Math.max(0, Math.min(15, selectedNotifyBeforeMinutes));
        final int[] selected = new int[]{checked};
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_before_intake)
                .setSingleChoiceItems(minuteItems, checked, (dialog, which) -> selected[0] = which)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    selectedNotifyBeforeMinutes = selected[0];
                    updateNotifyBeforeView();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showWheelTimePicker(int initialMinutes, OnTimeSelectedListener listener) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        int initialHour = Math.max(0, Math.min(23, initialMinutes / 60));
        int initialMinute = Math.max(0, Math.min(59, initialMinutes % 60));
        timePicker.setHour(initialHour);
        timePicker.setMinute(initialMinute);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    listener.onSelected(hour * 60 + minute);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void updateTimesView() {
        if (selectedTimesMinutes.isEmpty()) {
            etTimes.setText("");
            return;
        }
        List<String> display = new ArrayList<>();
        for (Integer item : selectedTimesMinutes) {
            display.add(formatMinutes(item));
        }
        etTimes.setText(TextUtils.join(", ", display));
    }

    private void updateIntervalView() {
        if (selectedIntervalMinutes <= 0) {
            etInterval.setText(getString(R.string.interval_not_set));
            return;
        }
        etInterval.setText(formatMinutes(selectedIntervalMinutes));
    }

    private void updateNotifyBeforeView() {
        etNotifyBefore.setText(getString(R.string.minutes_short_template, selectedNotifyBeforeMinutes));
    }

    private String formatMinutes(int totalMinutes) {
        int normalized = Math.max(0, totalMinutes);
        int hour = normalized / 60;
        int minute = normalized % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private void loadDrugDictionary() {
        AppExecutors.io().execute(() -> {
            List<DrugEntity> drugs = drugRepository.getAllSync();
            List<String> names = new ArrayList<>();
            drugCatalog.clear();
            drugCatalog.addAll(drugs);
            for (DrugEntity drug : drugs) {
                names.add(drug.name);
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        CourseEditActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        names
                );
                etDrug.setAdapter(adapter);
                if (editingCourse != null) {
                    applyCourseToUi();
                }
            });
        });
    }

    private void openDrugPickerDialog() {
        if (drugCatalog.isEmpty()) {
            toast("Справочник пуст");
            return;
        }
        String[] names = new String[drugCatalog.size()];
        for (int i = 0; i < drugCatalog.size(); i++) {
            names[i] = drugCatalog.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_drug)
                .setItems(names, (dialog, which) -> applySelectedDrug(drugCatalog.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private DrugEntity findDrugByName(String name) {
        for (DrugEntity drug : drugCatalog) {
            if (drug.name.equals(name)) {
                return drug;
            }
        }
        return null;
    }

    private void applySelectedDrug(DrugEntity drug) {
        selectedDrugId = drug.id;
        etDrug.setText(drug.name, false);
        if (TextUtils.isEmpty(etDosage.getText())) {
            etDosage.setText(drug.dosage);
        }
    }

    private void loadCourse(long courseId) {
        AppExecutors.io().execute(() -> {
            editingCourse = courseRepository.getByIdSync(courseId);
            runOnUiThread(this::applyCourseToUi);
        });
    }

    private void applyCourseToUi() {
        if (editingCourse == null) {
            return;
        }
        selectedDrugId = editingCourse.drugId;
        for (DrugEntity item : drugCatalog) {
            if (item.id == editingCourse.drugId) {
                etDrug.setText(item.name, false);
                break;
            }
        }

        etDosage.setText(editingCourse.dosageText);
        startDateMillis = editingCourse.startDate;
        endDateMillis = editingCourse.endDate;
        etStartDate.setText(DateTimeUtils.formatDate(startDateMillis));
        etEndDate.setText(DateTimeUtils.formatDate(endDateMillis));

        selectedTimesMinutes.clear();
        selectedTimesMinutes.addAll(DateTimeUtils.parseTimesCsv(editingCourse.exactTimesCsv));
        if (selectedTimesMinutes.isEmpty()) {
            selectedTimesMinutes.add(8 * 60);
        }
        Collections.sort(selectedTimesMinutes);
        updateTimesView();

        selectedIntervalMinutes = Math.max(0, editingCourse.intervalMinutes);
        updateIntervalView();

        selectedNotifyBeforeMinutes = Math.max(0, Math.min(15, editingCourse.notificationLeadMinutes));
        updateNotifyBeforeView();

        spNotificationType.setSelection(indexOfValue(R.array.notification_type_values, editingCourse.notificationType));
    }

    private int indexOfValue(int arrayRes, String value) {
        String[] values = getResources().getStringArray(arrayRes);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private String selectedNotificationValue() {
        String[] values = getResources().getStringArray(R.array.notification_type_values);
        int pos = spNotificationType.getSelectedItemPosition();
        return values[Math.max(0, Math.min(pos, values.length - 1))];
    }

    private void saveCourse() {
        String drugName = etDrug.getText() == null ? "" : etDrug.getText().toString().trim();
        DrugEntity selectedByName = findDrugByName(drugName);
        if (selectedByName != null) {
            selectedDrugId = selectedByName.id;
        } else {
            selectedDrugId = -1L;
        }

        String dosage = etDosage.getText() == null ? "" : etDosage.getText().toString().trim();
        if (dosage.isEmpty()) {
            toast("Укажите дозировку");
            return;
        }
        if (!ValidationUtils.isValidDosageRange(dosage)) {
            toast("Дозировка должна быть в диапазоне от 1 мкг до 100 г");
            return;
        }

        long durationDays = ((DateTimeUtils.startOfDay(endDateMillis) - DateTimeUtils.startOfDay(startDateMillis)) / (24L * 60L * 60L * 1000L)) + 1;
        if (durationDays < 1 || durationDays > 365) {
            toast("Длительность курса должна быть от 1 до 365 дней");
            return;
        }

        if (selectedTimesMinutes.isEmpty()) {
            toast("Добавьте хотя бы одно время приема");
            return;
        }

        if (selectedIntervalMinutes > 0) {
            if (selectedIntervalMinutes < 5) {
                toast("Интервал должен быть не менее 5 минут");
                return;
            }
            if (1440 / selectedIntervalMinutes > 288) {
                toast("Слишком частые напоминания");
                return;
            }
        }

        if (selectedNotifyBeforeMinutes < 0 || selectedNotifyBeforeMinutes > 15) {
            toast("Опережение уведомления должно быть от 0 до 15 минут");
            return;
        }

        if (selectedDrugId <= 0) {
            if (!ValidationUtils.isValidDrugText(drugName, 2, 50)) {
                toast(getString(R.string.validation_drug_name));
                return;
            }
            btnSave.setEnabled(false);
            drugRepository.createOrGetCustomDrug(drugName, dosage, new RepositoryCallback<>() {
                @Override
                public void onSuccess(DrugEntity value) {
                    selectedDrugId = value.id;
                    etDrug.setText(value.name, false);
                    persistCourse(dosage);
                }

                @Override
                public void onError(String message) {
                    btnSave.setEnabled(true);
                    toast(message);
                }
            });
            return;
        }

        persistCourse(dosage);
    }

    private void persistCourse(String dosage) {
        TherapyCourseEntity course = editingCourse != null ? editingCourse : new TherapyCourseEntity();
        course.drugId = selectedDrugId;
        course.dosageText = dosage;
        course.startDate = DateTimeUtils.startOfDay(startDateMillis);
        course.endDate = DateTimeUtils.startOfDay(endDateMillis);
        course.scheduleType = selectedIntervalMinutes > 0 ? Constants.SCHEDULE_INTERVAL : Constants.SCHEDULE_EXACT_TIMES;
        course.notificationType = selectedNotificationValue();
        course.exactTimesCsv = buildTimesCsv();
        course.intervalMinutes = selectedIntervalMinutes;
        course.notificationLeadMinutes = selectedNotifyBeforeMinutes;
        course.courseDays = 0;
        course.pauseDays = 0;

        btnSave.setEnabled(false);
        courseRepository.saveCourse(course, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Long value) {
                btnSave.setEnabled(true);
                toast("Курс сохранён");
                finish();
            }

            @Override
            public void onError(String message) {
                btnSave.setEnabled(true);
                toast(message);
            }
        });
    }

    private void confirmDelete() {
        if (editingCourse == null || editingCourse.id <= 0) {
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_course)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteCourse())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteCourse() {
        btnDelete.setEnabled(false);
        btnSave.setEnabled(false);
        courseRepository.deleteCourse(editingCourse.id, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Boolean value) {
                toast("Курс удалён");
                finish();
            }

            @Override
            public void onError(String message) {
                btnDelete.setEnabled(true);
                btnSave.setEnabled(true);
                toast(message);
            }
        });
    }

    private String buildTimesCsv() {
        List<String> items = new ArrayList<>();
        for (Integer minute : selectedTimesMinutes) {
            items.add(formatMinutes(minute));
        }
        return TextUtils.join(",", items);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private interface OnTimeSelectedListener {
        void onSelected(int selectedMinutes);
    }
}

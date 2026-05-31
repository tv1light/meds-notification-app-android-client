package com.medreminder.ui.courses;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.medreminder.R;
import com.medreminder.data.local.entity.CourseWithDrug;
import com.medreminder.data.repository.CourseRepository;
import com.medreminder.ui.course_edit.CourseEditActivity;

import java.util.List;

public class CoursesFragment extends Fragment {
    private CourseRepository courseRepository;
    private CoursesAdapter adapter;
    private EditText etSearch;
    private Spinner spSort;
    private TextView tvEmpty;

    private LiveData<List<CourseWithDrug>> source;

    public CoursesFragment() {
        super(R.layout.fragment_courses);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        courseRepository = new CourseRepository(requireContext());
        etSearch = view.findViewById(R.id.etSearchCourse);
        spSort = view.findViewById(R.id.spSort);
        tvEmpty = view.findViewById(R.id.tvEmptyCourses);

        RecyclerView rvCourses = view.findViewById(R.id.rvCourses);
        rvCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CoursesAdapter(course -> {
            Intent intent = new Intent(requireContext(), CourseEditActivity.class);
            intent.putExtra("course_id", course.courseId);
            startActivity(intent);
        });
        rvCourses.setAdapter(adapter);

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.course_sort_labels,
                android.R.layout.simple_spinner_item
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSort.setAdapter(sortAdapter);

        FloatingActionButton fab = view.findViewById(R.id.fabAddCourse);
        fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), CourseEditActivity.class)));

        etSearch.addTextChangedListener(new TextWatcher() {
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

        spSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                subscribe();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        subscribe();
    }

    private void subscribe() {
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        boolean sortByDrugName = spSort.getSelectedItemPosition() == 1;

        if (source != null) {
            source.removeObservers(getViewLifecycleOwner());
        }

        source = courseRepository.observeCourses(query, sortByDrugName);
        source.observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<CourseWithDrug> list) {
        adapter.submit(list);
        tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}

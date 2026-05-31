package com.medreminder.ui.drugs;

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
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.repository.DrugRepository;

import java.util.List;

public class DrugsFragment extends Fragment {
    private DrugRepository drugRepository;
    private DrugsAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty;

    private LiveData<List<DrugEntity>> source;

    public DrugsFragment() {
        super(R.layout.fragment_drugs);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        drugRepository = new DrugRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.rvDrugs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DrugsAdapter();
        recyclerView.setAdapter(adapter);

        etSearch = view.findViewById(R.id.etSearchDrugs);
        tvEmpty = view.findViewById(R.id.tvEmptyDrugs);

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

        subscribe();
    }

    private void subscribe() {
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        if (source != null) {
            source.removeObservers(getViewLifecycleOwner());
        }
        source = drugRepository.observe(query);
        source.observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<DrugEntity> items) {
        adapter.submit(items);
        tvEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}

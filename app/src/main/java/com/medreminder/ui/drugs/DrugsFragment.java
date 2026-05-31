package com.medreminder.ui.drugs;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.medreminder.R;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.repository.DrugRepository;
import com.medreminder.data.repository.RepositoryCallback;
import com.medreminder.data.repository.SessionRepository;
import com.medreminder.util.AppExecutors;

import java.util.List;

public class DrugsFragment extends Fragment {
    private DrugRepository drugRepository;
    private SessionRepository sessionRepository;
    private DrugsAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty;
    private FloatingActionButton fabAddDrug;

    private LiveData<List<DrugEntity>> source;

    public DrugsFragment() {
        super(R.layout.fragment_drugs);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        drugRepository = new DrugRepository(requireContext());
        sessionRepository = new SessionRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.rvDrugs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DrugsAdapter();
        recyclerView.setAdapter(adapter);

        etSearch = view.findViewById(R.id.etSearchDrugs);
        tvEmpty = view.findViewById(R.id.tvEmptyDrugs);
        fabAddDrug = view.findViewById(R.id.fabAddDrug);
        fabAddDrug.setOnClickListener(v -> openAddDrugDialog());

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
        refreshAdminAccess();
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

    private void refreshAdminAccess() {
        AppExecutors.io().execute(() -> {
            boolean isAdmin = sessionRepository.isAdminSync();
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> fabAddDrug.setVisibility(isAdmin ? View.VISIBLE : View.GONE));
        });
    }

    private void openAddDrugDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_drug, null, false);
        EditText etName = dialogView.findViewById(R.id.etDrugName);
        EditText etForm = dialogView.findViewById(R.id.etDrugForm);
        EditText etDosage = dialogView.findViewById(R.id.etDrugDosage);
        EditText etSubstance = dialogView.findViewById(R.id.etDrugSubstance);
        EditText etCountry = dialogView.findViewById(R.id.etDrugCountry);
        EditText etManufacturer = dialogView.findViewById(R.id.etDrugManufacturer);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_drug)
                .setView(dialogView)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (dialog, which) -> drugRepository.addDrugByAdmin(
                        text(etName),
                        text(etForm),
                        text(etDosage),
                        text(etSubstance),
                        text(etCountry),
                        text(etManufacturer),
                        new RepositoryCallback<>() {
                            @Override
                            public void onSuccess(DrugEntity value) {
                                toast(getString(R.string.drug_added));
                            }

                            @Override
                            public void onError(String message) {
                                toast(message);
                            }
                        }
                ))
                .show();
    }

    private String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}

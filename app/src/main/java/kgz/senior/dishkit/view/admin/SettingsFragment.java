package kgz.senior.dishkit.view.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.AppSettings;
import kgz.senior.dishkit.viewmodel.AdminViewModel;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;


public class SettingsFragment extends Fragment {

    private AdminViewModel adminViewModel;

    private TextInputEditText cafeNameEditText;
    private ImageView logoPreviewImageView;
    private Button uploadLogoButton;
    private MaterialSwitch showImageSwitch;
    private MaterialSwitch showDescriptionSwitch;
    private MaterialSwitch showPriceSwitch;
    private Button saveSettingsButton;
    private ProgressBar settingsProgressBar;

    private Uri tempLogoUri;

    // Activity Result Launcher для выбора логотипа
    private final ActivityResultLauncher<Intent> pickLogoImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    tempLogoUri = result.getData().getData();
                    logoPreviewImageView.setImageURI(tempLogoUri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        // --- Инициализация View-элементов ---
        cafeNameEditText = view.findViewById(R.id.cafeNameEditText);
        logoPreviewImageView = view.findViewById(R.id.logoPreviewImageView);
        uploadLogoButton = view.findViewById(R.id.uploadLogoButton);
        showImageSwitch = view.findViewById(R.id.showImageSwitch);
        showDescriptionSwitch = view.findViewById(R.id.showDescriptionSwitch);
        showPriceSwitch = view.findViewById(R.id.showPriceSwitch);
        saveSettingsButton = view.findViewById(R.id.saveSettingsButton);
        settingsProgressBar = view.findViewById(R.id.settingsProgressBar);

        // --- Подписка на LiveData из ViewModel ---
        adminViewModel.getAppSettings().observe(getViewLifecycleOwner(), this::updateAppSettingsUI);
        adminViewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);
        adminViewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
        adminViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), "Настройки успешно сохранены!", Toast.LENGTH_SHORT).show();
                tempLogoUri = null; // Сброс после успешной загрузки
            }
        });

        // --- Обработчики событий ---
        uploadLogoButton.setOnClickListener(v -> pickImage(pickLogoImageLauncher));
        saveSettingsButton.setOnClickListener(v -> saveAppSettings());


        return view;
    }

// ... (внутри класса SettingsFragment)

    private void updateAppSettingsUI(AppSettings settings) {
        if (settings != null) {
            cafeNameEditText.setText(settings.getCafeName());
            if (settings.getLogoUrl() != null && !settings.getLogoUrl().isEmpty()) {
                Glide.with(requireContext()).load(settings.getLogoUrl()).into(logoPreviewImageView);
            } else {
                logoPreviewImageView.setImageResource(R.drawable.ic_logo);
            }

            // ИСПРАВЛЕНО: isImageVisible() вместо isShowImage()
            showImageSwitch.setChecked(settings.isImageVisible());

            // ИСПРАВЛЕНО: isDescriptionVisible() вместо isShowDescription()
            showDescriptionSwitch.setChecked(settings.isDescriptionVisible());

            // ИСПРАВЛЕНО: isPriceVisible() вместо isShowPrice()
            showPriceSwitch.setChecked(settings.isPriceVisible());
        }
    }

    private void saveAppSettings() {
        AppSettings settings = adminViewModel.getAppSettings().getValue();
        if (settings == null) {
            settings = new AppSettings();
        }

        settings.setCafeName(cafeNameEditText.getText().toString());

        // ИСПРАВЛЕНО: setImageVisible() вместо setShowImage()
        settings.setImageVisible(showImageSwitch.isChecked());

        // ИСПРАВЛЕНО: setDescriptionVisible() вместо setShowDescription()
        settings.setDescriptionVisible(showDescriptionSwitch.isChecked());

        // ИСПРАВЛЕНО: setPriceVisible() вместо setShowPrice()
        settings.setPriceVisible(showPriceSwitch.isChecked());

        if (tempLogoUri != null) {
            adminViewModel.uploadLogo(tempLogoUri);
        } else {
            adminViewModel.saveAppSettings(settings);
        }
    }

// ...

    private void pickImage(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcher.launch(intent);
    }

    private void showLoading(boolean isLoading) {
        settingsProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveSettingsButton.setEnabled(!isLoading);
        // Можно также отключать другие элементы UI, если нужно
    }
}
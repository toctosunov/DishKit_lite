package kgz.senior.dishkit.view.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log; // Убедитесь, что этот импорт есть
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.AppSettings;
import kgz.senior.dishkit.model.Category;
import kgz.senior.dishkit.model.MenuItem;
import kgz.senior.dishkit.viewmodel.AdminViewModel;

public class DishesFragment extends Fragment implements AdminMenuAdapter.OnMenuItemActionListener {

    private static final String TAG = "DishesFragment"; // Добавили TAG для логов

    private AdminViewModel adminViewModel;

    private RecyclerView adminMenuRecyclerView;
    private AdminMenuAdapter adminMenuAdapter;
    private ExtendedFloatingActionButton addDishButton;
    private ProgressBar dishesProgressBar;
    private Spinner sortSpinner;
    private MaterialToolbar dishesToolbar;

    private Uri tempDishImageUri;
    private ImageView dialogDishImagePreviewGlobal;
    private MenuItem currentEditingMenuItem;

    private List<Category> availableCategories = new ArrayList<>();
    private List<MenuItem> allMenuItems = new ArrayList<>();

    private SortType currentSortType = SortType.ORDER; // Дефолтное значение

    private enum SortType {
        ORDER,
        ALPHABETICAL
    }

    private final ActivityResultLauncher<Intent> pickDishImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    tempDishImageUri = result.getData().getData();
                    if (dialogDishImagePreviewGlobal != null) {
                        Glide.with(requireContext()).load(tempDishImageUri).into(dialogDishImagePreviewGlobal);
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dishes, container, false);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        // --- Инициализация View-элементов ---
        adminMenuRecyclerView = view.findViewById(R.id.adminMenuRecyclerView);
        addDishButton = view.findViewById(R.id.addDishButton);
        dishesProgressBar = view.findViewById(R.id.dishesProgressBar);
        sortSpinner = view.findViewById(R.id.sortSpinner);
        dishesToolbar = view.findViewById(R.id.dishesToolbar);

        // --- Настройка RecyclerView для блюд ---
        adminMenuRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adminMenuAdapter = new AdminMenuAdapter();
        adminMenuAdapter.setOnMenuItemActionListener(this);
        adminMenuRecyclerView.setAdapter(adminMenuAdapter);

        // --- Настройка Spinner для сортировки ---
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.sort_options,
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(spinnerAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                SortType newSortType;
                if (selectedOption.equals("По порядку")) {
                    newSortType = SortType.ORDER;
                } else { // "По алфавиту"
                    newSortType = SortType.ALPHABETICAL;
                }

                Log.d(TAG, "Spinner selection changed to: " + selectedOption + ". newSortType: " + newSortType);

                if (currentSortType != newSortType) {
                    Log.d(TAG, "Current sort type changed from " + currentSortType + " to " + newSortType);
                    currentSortType = newSortType;
                    adminViewModel.updateAdminDishSortType(newSortType.name());
                }
                applySortAndDisplayItems(allMenuItems);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });


        // --- Подписка на LiveData из ViewModel ---
        adminViewModel.getMenuItems().observe(getViewLifecycleOwner(), menuItemsList -> {
            Log.d(TAG, "MenuItems LiveData updated. Number of items: " + (menuItemsList != null ? menuItemsList.size() : 0));
            allMenuItems = menuItemsList;
            applySortAndDisplayItems(allMenuItems);
        });

        adminViewModel.getCategories().observe(getViewLifecycleOwner(), categoriesList -> {
            availableCategories.clear();
            availableCategories.addAll(categoriesList);
        });

        adminViewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);
        adminViewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error: " + errorMessage);
            }
        });
        adminViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), "Операция успешно завершена!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Operation successful.");
                tempDishImageUri = null;
                dialogDishImagePreviewGlobal = null;
            }
        });

        // НОВОЕ: Подписка на изменения AppSettings
        adminViewModel.getAppSettings().observe(getViewLifecycleOwner(), appSettings -> {
            if (appSettings != null) {
                String sortTypeString = appSettings.getAdminDishSortType();
                Log.d(TAG, "AppSettings LiveData updated. adminDishSortType from Firestore: " + sortTypeString);
                try {
                    SortType newSortType = SortType.valueOf(sortTypeString);
                    if (currentSortType != newSortType) {
                        Log.d(TAG, "Updating currentSortType from AppSettings: " + currentSortType + " -> " + newSortType);
                        currentSortType = newSortType;
                        int spinnerPosition = (newSortType == SortType.ORDER) ? 0 : 1;
                        sortSpinner.setSelection(spinnerPosition, false); // false, чтобы не вызывал onItemSelected
                        applySortAndDisplayItems(allMenuItems);
                    } else {
                        Log.d(TAG, "Sort type from AppSettings is same as current: " + currentSortType + ". No change needed for currentSortType.");
                        // Если тип совпадает, но LiveData обновилась, все равно применить сортировку,
                        // чтобы убедиться, что список отображается корректно.
                        applySortAndDisplayItems(allMenuItems);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Неизвестный тип сортировки в AppSettings: " + sortTypeString + ". Устанавливаю ORDER по умолчанию.", e);
                    currentSortType = SortType.ORDER;
                    sortSpinner.setSelection(0, false);
                    applySortAndDisplayItems(allMenuItems);
                }
            } else {
                Log.d(TAG, "AppSettings is null. Setting currentSortType to ORDER.");
                currentSortType = SortType.ORDER;
                sortSpinner.setSelection(0, false);
                applySortAndDisplayItems(allMenuItems);
            }
        });


        // --- Обработчики событий ---
        addDishButton.setOnClickListener(v -> showAddEditDishDialog(null));

        return view;
    }

    private void applySortAndDisplayItems(List<MenuItem> menuItemsList) {
        if (menuItemsList == null || menuItemsList.isEmpty()) {
            Log.d(TAG, "applySortAndDisplayItems: menuItemsList is null or empty. Setting empty adapter.");
            adminMenuAdapter.setMenuItems(new ArrayList<>());
            return;
        }
        Log.d(TAG, "applySortAndDisplayItems called with currentSortType: " + currentSortType + ". Number of items: " + menuItemsList.size());

        List<MenuItem> sortedList = new ArrayList<>(menuItemsList);

        // --- ДОБАВИТЬ ЭТОТ ЛОГ ---
        Log.d(TAG, "Before sort (currentSortType: " + currentSortType + "):");
        for (MenuItem item : sortedList) {
            Log.d(TAG, "  Item: " + item.getName() + ", Category: " + item.getCategory() + ", Order: " + item.getOrder());
        }
        // --- КОНЕЦ ДОБАВЛЯЕМОГО ЛОГА ---

        Comparator<MenuItem> categoryComparator = (item1, item2) -> {
            String category1 = item1.getCategory() != null ? item1.getCategory() : "Без категории";
            String category2 = item2.getCategory() != null ? item2.getCategory() : "Без категории";
            return category1.compareToIgnoreCase(category2);
        };

        Comparator<MenuItem> secondaryComparator;
        if (currentSortType == SortType.ORDER) {
            Log.d(TAG, "Using secondaryComparator: ORDER");
            secondaryComparator = Comparator.comparingInt(MenuItem::getOrder);
        } else { // SortType.ALPHABETICAL
            Log.d(TAG, "Using secondaryComparator: ALPHABETICAL");
            secondaryComparator = Comparator.comparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER);
        }

        sortedList.sort(categoryComparator.thenComparing(secondaryComparator));

        // --- И этот лог тоже ---
        Log.d(TAG, "After sort (currentSortType: " + currentSortType + "):");
        for (MenuItem item : sortedList) {
            Log.d(TAG, "  Item: " + item.getName() + ", Category: " + item.getCategory() + ", Order: " + item.getOrder());
        }
        // --- КОНЕЦ ДОБАВЛЯЕМОГО ЛОГА ---

        Log.d(TAG, "Sorted list size: " + sortedList.size() + ". First item (after sort): " + (sortedList.isEmpty() ? "N/A" : sortedList.get(0).getName() + " (Order: " + sortedList.get(0).getOrder() + ", Category: " + sortedList.get(0).getCategory() + ")"));
        adminMenuAdapter.setMenuItems(sortedList);
    }


    private void pickImage(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcher.launch(intent);
    }

    // --- Диалог добавления/редактирования блюда ---
    private void showAddEditDishDialog(@Nullable MenuItem itemToEdit) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_edit_dish, null);
        builder.setView(dialogView);

        TextInputEditText dishNameEditText = dialogView.findViewById(R.id.dialogDishNameEditText);
        TextInputEditText dishDescriptionEditText = dialogView.findViewById(R.id.dialogDishDescriptionEditText);
        AutoCompleteTextView dishCategoryAutoCompleteTextView = dialogView.findViewById(R.id.dialogDishCategoryAutoCompleteTextView);
        TextInputEditText dishPriceEditText = dialogView.findViewById(R.id.dialogDishPriceEditText);
        TextInputEditText dishOrderEditText = dialogView.findViewById(R.id.dialogDishOrderEditText);
        MaterialSwitch dishVisibleSwitch = dialogView.findViewById(R.id.dialogDishVisibleSwitch);
        dialogDishImagePreviewGlobal = dialogView.findViewById(R.id.dialogDishImagePreview);
        Button uploadDishImageButton = dialogView.findViewById(R.id.dialogUploadDishImageButton);

        tempDishImageUri = null;

        List<String> categoryNames = availableCategories.stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames);
        dishCategoryAutoCompleteTextView.setAdapter(categoryAdapter);

        if (itemToEdit != null) {
            builder.setTitle("Редактировать Блюдо");
            dishNameEditText.setText(itemToEdit.getName());
            dishDescriptionEditText.setText(itemToEdit.getDescription());
            dishCategoryAutoCompleteTextView.setText(itemToEdit.getCategory() != null ? itemToEdit.getCategory() : "", false);
            dishPriceEditText.setText(String.valueOf(itemToEdit.getPrice()));
            dishOrderEditText.setText(String.valueOf(itemToEdit.getOrder()));
            dishVisibleSwitch.setChecked(itemToEdit.isVisible());
            if (itemToEdit.getImageUrl() != null && !itemToEdit.getImageUrl().isEmpty()) {
                Glide.with(requireContext()).load(itemToEdit.getImageUrl()).into(dialogDishImagePreviewGlobal);
            } else {
                dialogDishImagePreviewGlobal.setImageResource(R.drawable.ic_logo);
            }
            currentEditingMenuItem = itemToEdit;
        } else {
            builder.setTitle("Добавить Новое Блюдо");
            currentEditingMenuItem = new MenuItem();
            dishOrderEditText.setText(String.valueOf(allMenuItems.size() + 1));
            if (!categoryNames.isEmpty()) {
                dishCategoryAutoCompleteTextView.setText(categoryNames.get(0), false);
            } else {
                dishCategoryAutoCompleteTextView.setText("Без категории", false);
            }
            dialogDishImagePreviewGlobal.setImageResource(R.drawable.ic_logo);
            dishVisibleSwitch.setChecked(true);
        }

        uploadDishImageButton.setOnClickListener(v -> pickImage(pickDishImageLauncher));

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = dishNameEditText.getText().toString().trim();
            String description = dishDescriptionEditText.getText().toString().trim();
            String priceText = dishPriceEditText.getText().toString().trim();
            String orderText = dishOrderEditText.getText().toString().trim();
            String selectedCategoryName = dishCategoryAutoCompleteTextView.getText().toString().trim();

            if (name.isEmpty() || priceText.isEmpty() || orderText.isEmpty() || selectedCategoryName.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все обязательные поля (Имя, Цена, Порядок, Категория)", Toast.LENGTH_LONG).show();
                return;
            }

            Optional<Category> existingCategory = availableCategories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(selectedCategoryName))
                    .findFirst();

            String selectedCategoryId = null;
            if (existingCategory.isPresent()) {
                selectedCategoryId = existingCategory.get().getId();
            } else {
                Category newCategory = new Category(null, selectedCategoryName, 0, null);
                adminViewModel.addOrUpdateCategory(newCategory);
            }

            MenuItem dishToSave = itemToEdit != null ? itemToEdit : new MenuItem();
            dishToSave.setName(name);
            dishToSave.setDescription(description);
            dishToSave.setCategory(selectedCategoryName);
            dishToSave.setCategoryId(selectedCategoryId);
            dishToSave.setPrice(Double.parseDouble(priceText));
            dishToSave.setOrder(Integer.parseInt(orderText));
            dishToSave.setVisible(dishVisibleSwitch.isChecked());

            if (tempDishImageUri != null) {
                adminViewModel.addOrUpdateMenuItem(dishToSave, tempDishImageUri);
            } else {
                adminViewModel.addOrUpdateMenuItem(dishToSave, null);
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onEditClick(MenuItem item) {
        showAddEditDishDialog(item);
    }

    @Override
    public void onDeleteClick(MenuItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Удалить блюдо?")
                .setMessage("Вы уверены, что хотите удалить блюдо \"" + item.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> adminViewModel.deleteMenuItem(item.getId()))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showLoading(boolean isLoading) {
        dishesProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        addDishButton.setEnabled(!isLoading);
        adminMenuRecyclerView.setEnabled(!isLoading);
        sortSpinner.setEnabled(!isLoading);
    }
}
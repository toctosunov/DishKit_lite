package kgz.senior.dishkit.view;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.Category;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.CategoryViewHolder> {

    // --- Динамические данные и состояние ---
    private List<CategoryDisplayWrapper> displayList = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();
    private String expandedParentId = null;
    private int selectedPosition = RecyclerView.NO_POSITION;

    private OnCategorySelectedListener listener;

    private static final int SUB_CATEGORY_INDENT_DP = 24;

    public CategoryListAdapter(List<Category> categories, OnCategorySelectedListener listener) {
        this.listener = listener;
        setCategories(categories);
    }

    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    // --- Метод, вызываемый при получении новых данных ---
    public void setCategories(List<Category> allAvailableCategories) {
        this.allCategories = new ArrayList<>(allAvailableCategories);
        rebuildDisplayList(true); // Всегда вызываем с автовыбором при загрузке данных
    }

    /**
     * Формирует список отображения (displayList) на основе текущего expandedParentId.
     * @param autoSelectIfNone Если true, автоматически выбирает первый элемент, если нет выбранного.
     */
    private void rebuildDisplayList(boolean autoSelectIfNone) {
        // 1. Сохраняем ID ранее выбранной категории для восстановления
        String previouslySelectedId = selectedPosition != RecyclerView.NO_POSITION && selectedPosition < displayList.size()
                ? displayList.get(selectedPosition).getCategory().getId()
                : null;

        displayList.clear();

        // 2. Группируем категории по parentId
        Map<String, List<Category>> subcategoriesByParentId = allCategories.stream()
                .filter(c -> c.getParentId() != null && !c.getParentId().isEmpty())
                .collect(Collectors.groupingBy(Category::getParentId));

        // 3. Получаем основные категории и сортируем их
        List<Category> mainCategories = allCategories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId().isEmpty())
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .collect(Collectors.toList());

        // 4. Формируем плоский список для RecyclerView, добавляя подкатегории, если родитель раскрыт
        for (Category mainCategory : mainCategories) {
            displayList.add(new CategoryDisplayWrapper(mainCategory, 0));

            // Если эта родительская категория раскрыта, добавляем подкатегории
            if (Objects.equals(mainCategory.getId(), expandedParentId)) {
                List<Category> subcategories = subcategoriesByParentId.get(mainCategory.getId());
                if (subcategories != null) {
                    subcategories.stream()
                            .sorted((s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()))
                            .forEach(sub -> displayList.add(new CategoryDisplayWrapper(sub, 1)));
                }
            }
        }

        // 5. Обработка "Без категории" в конце
        allCategories.stream()
                .filter(c -> c.getName().equals("Без категории"))
                .findFirst()
                .ifPresent(noCat -> displayList.add(new CategoryDisplayWrapper(noCat, 0)));


        // 6. Восстановление выбранной позиции (по ID)
        selectedPosition = RecyclerView.NO_POSITION;
        if (previouslySelectedId != null) {
            for (int i = 0; i < displayList.size(); i++) {
                if (Objects.equals(displayList.get(i).getCategory().getId(), previouslySelectedId)) {
                    selectedPosition = i;
                    break;
                }
            }
        }

        // 7. Если ничего не выбрано И РАЗРЕШЕНО автовыбором, выбираем первый элемент
        if (autoSelectIfNone && selectedPosition == RecyclerView.NO_POSITION && !displayList.isEmpty()) {
            // !!! ВАЖНО: Передаем true, чтобы selectCategory не вызывал rebuildDisplayList
            selectCategory(0, true);
        } else {
            notifyDataSetChanged();
        }
    }


    /**
     * Обрабатывает клик и логику раскрытия/скрытия
     * @param isAutoSelection true, если вызов инициирован автовыбором, false - кликом пользователя.
     */
    public void selectCategory(int position, boolean isAutoSelection) {
        if (position < 0 || position >= displayList.size()) return;

        Category selected = displayList.get(position).getCategory();

        // 1. Всегда уведомляем Activity о выборе для фильтрации блюд
        if (listener != null) {
            listener.onCategorySelected(selected);
        }

        // 2. Обновляем selectedPosition и визуальное состояние
        int oldSelectedPosition = selectedPosition;
        selectedPosition = position;

        // Обновляем визуальное состояние только двух элементов
        if (oldSelectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(oldSelectedPosition);
        notifyItemChanged(selectedPosition);

        // 3. Логика раскрытия/скрытия (только для основных категорий, не для "Без категории")
        boolean isMainCategory = selected.getParentId() == null || selected.getParentId().isEmpty();
        boolean isNoCategory = selected.getName().equals("Без категории");

        if (isMainCategory && !isNoCategory) {

            // !!! НОВАЯ ПРОВЕРКА: Если это автовыбор, мы выбираем категорию, но не раскрываем ее,
            // чтобы избежать рекурсии.
            if (isAutoSelection) {
                return;
            }

            // Если клик по той же категории, скрываем (переключаем)
            if (Objects.equals(expandedParentId, selected.getId())) {
                expandedParentId = null;
            }
            // Иначе, раскрываем новую
            else {
                expandedParentId = selected.getId();
            }

            // Перестраиваем список, чтобы показать/скрыть подкатегории.
            rebuildDisplayList(false);

            return;
        }
    }

    // Перегруженный метод для клика (для обратной совместимости, равносилен isAutoSelection=false)
    public void selectCategory(int position) {
        selectCategory(position, false);
    }


    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_left_panel, parent, false);
        return new CategoryViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryDisplayWrapper wrapper = displayList.get(position);
        Category category = wrapper.getCategory();

        // Устанавливаем текст
        holder.categoryNameTextView.setText(category.getName());

        // Устанавливаем отступ (для подкатегорий)
        int indentPx = 0;
        if (wrapper.getIndentLevel() > 0) {
            // Конвертируем DP в пиксели для отступа
            indentPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    SUB_CATEGORY_INDENT_DP, // SUB_CATEGORY_INDENT_DP = 24
                    holder.itemView.getResources().getDisplayMetrics()
            );
        }

        // !!! ИЗМЕНЕННАЯ ЛОГИКА: Применяем отступ слева !!!
        holder.categoryNameTextView.setPadding(indentPx, 0, 0, 0);


        // Логика выделения (выделяем синим, если выбрано)
        if (position == selectedPosition) {
            holder.categoryNameTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_secondary));
        } else {
            holder.categoryNameTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_onPrimaryContainer));
        }

        // Логика клика
        holder.itemView.setOnClickListener(v -> selectCategory(position));
    }


    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameTextView;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.categoryNameTextView);
        }
    }

    private static class CategoryDisplayWrapper {
        private final Category category;
        private final int indentLevel;

        public CategoryDisplayWrapper(Category category, int indentLevel) {
            this.category = category;
            this.indentLevel = indentLevel;
        }

        public Category getCategory() {
            return category;
        }

        public int getIndentLevel() {
            return indentLevel;
        }
    }
}
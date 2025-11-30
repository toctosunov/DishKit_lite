package kgz.senior.dishkit.view.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.Category;
import kgz.senior.dishkit.view.MainActivity;
import kgz.senior.dishkit.viewmodel.AdminViewModel;

public class AdminPanelActivity extends AppCompatActivity { // Убрали 'abstract' и 'implements AdminMenuAdapter.OnMenuItemActionListener'

    private AdminViewModel adminViewModel;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        Toolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Панель Администратора");

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        AdminPagerAdapter pagerAdapter = new AdminPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Привязываем TabLayout к ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Блюда");
                            break;
                        case 1:
                            tab.setText("Категории");
                            break;
                       case 2:
                            tab.setText("Настройки");
                            break;
                    }
                }).attach();

        // Подписка на LiveData из ViewModel для общих операций (например, выход)
        adminViewModel.getLogoutSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AdminPanelActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            adminViewModel.logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showAddEditCategoryDialog(Category category) {
        String title = (category == null) ? "Добавить категорию" : "Редактировать категорию: " + category.getName();
        Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
    }
}
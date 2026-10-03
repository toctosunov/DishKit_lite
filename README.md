# 🍽️ DishKit Lite — Электронное планшетное меню для кафе и ресторанов

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Java 11](https://img.shields.io/badge/Language-Java%2011-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Material 3](https://img.shields.io/badge/UI-Material%20Design%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

**DishKit Lite** — это нативное Android-приложение, разработанное специально для использования на планшетах в качестве интерактивного электронного меню и терминала заказа для гостей в залах кафе, ресторанов и лаунж-баров.

Приложение оптимизировано для работы в **альбомной (ландшафтной) ориентации**, поддерживает мгновенную синхронизацию каталога блюд через **Google Cloud Firestore**, позволяет гостям собирать предварительный заказ в корзину с подсчётом суммы в сомах (сом / KGS) и содержит защищенную панель администратора со скрытой точкой входа.

---

## 📌 Содержание
- [✨ Ключевые возможности](#-ключевые-возможности)
  - [Режим гостя (Интерактивное меню)](#режим-гостя-интерактивное-меню)
  - [Секретный вход в панель администратора](#секретный-вход-в-панель-администратора)
  - [Панель управления (Admin Panel)](#панель-управления-admin-panel)
- [🏗 Архитектура проекта](#-архитектура-проекта)
- [📂 Структура каталогов](#-структура-каталогов)
- [🗄 Модель данных Firebase Firestore](#-модель-данных-firebase-firestore)
- [⚙️ Технологический стек](#️-технологический-стек)
- [🚀 Быстрый старт и установка](#-быстрый-старт-и-установка)
- [🔒 Настройка безопасности Firebase](#-настройка-безопасности-firebase)
- [👤 Автор и контакты](#-автор-и-контакты)

---

## ✨ Ключевые возможности

### 🍽 Режим гостя (Интерактивное меню)
- **Альбомный интерфейс для планшетов:** Эргономичное двухколоночное разделение — слева вертикальный переключатель категорий, справа сетка карточек блюд (3 колонки).
- **Быстрая навигация по категориям:** Древовидная поддержка основных категорий и подкатегорий.
- **Подробные карточки блюд:** Фото блюда высокого разрешения (кеширование и плавная подгрузка через **Glide**), название, развернутый состав/описание и стоимость в сомах (сом).
- **Корзина гостя (Guest Cart):**
  - Локальный расчет стоимости и количества выбранных позиций в оперативной памяти устройства (`CartManager`).
  - Быстрое изменение количества (+ / -) и удаление блюд.
  - Модальное окно подтверждения перед полной очисткой корзины.
  - Без засорения базы данных незавершенными черновиками.
- **Гибкое отображение на лету:** Администратор может удаленно отключить показ фото, описаний, цен или корзины — меню мгновенно перестроится на экранах всех планшетов в зале без перезапуска приложения!

### 🔐 Секретный вход в панель администратора
В приложении реализован специальный механизм **kiosk-защиты**:
- Гости не видят кнопку входа в настройки или админку.
- Для входа сотруднику необходимо **быстро нажать 5 раз на логотип заведения в шапке (Toolbar)** в течение **1.5 секунд** (`REQUIRED_CLICKS = 5`, `CLICK_TIMEOUT_MS = 1500`).
- При успешном распознавании жеста открывается экран входа с логином и паролем Firebase Auth.

### 🛠 Панель управления (Admin Panel)
Встроенная админка разделена на 3 вкладки (**ViewPager2 + TabLayout**):
1. **Блюда (DishesFragment):**
   - Просмотр всех блюд каталога с бейджами активности и цен.
   - Добавление и редактирование блюда: загрузка изображения из галереи напрямую в Firebase Storage (`dish_images/`), указание названия, описания, цены, категории и порядкового номера.
   - Сортировка списка: по заданному порядку (`order`) или по алфавиту (`alphabetical`).
   - Переключатель видимости блюда в меню в один клик.
2. **Категории (CategoryManagementFragment):**
   - Управление структурой категорий и подкатегорий (поле `parentId`).
   - Валидация уникальности названия в пределах родительской ветки.
   - Каскадное пакетное удаление (`runBatch`): при удалении родительской категории все вложенные подкатегории удаляются автоматически.
3. **Настройки заведения (SettingsFragment):**
   - Изменение названия кафе / ресторана.
   - Загрузка и обновление логотипа заведения (`logos/`).
   - Глобальные переключатели видимости: отображение фото, описаний, цен и корзины гостя.

---

## 🏗 Архитектура проекта

Приложение построено по классическому архитектурному шаблону **MVVM (Model-View-ViewModel)** с использованием репозиториев и реактивных потоков **Android Jetpack LiveData**:

```
                    +----------------------------------------+
                    |          Firebase Cloud Services       |
                    |  (Firestore, Storage, Authentication)  |
                    +-------------------+--------------------+
                                        | Real-time Listeners / SDK
                                        v
                    +----------------------------------------+
                    |             MenuRepository             |
                    | (Слушатели Firestore, кэш, запросы)   |
                    +-------------------+--------------------+
                                        | LiveData
                                        v
                    +----------------------------------------+
                    |             AdminViewModel             |
                    | (Бизнес-логика, валидация, состояние)  |
                    +-------------------+--------------------+
                                        |
                 +----------------------+----------------------+
                 | LiveData                                    | LiveData
                 v                                             v
+----------------------------------+          +----------------------------------+
|           MainActivity           |          |        AdminPanelActivity        |
|  - CategoryListAdapter (Left)    |          |  - DishesFragment                |
|  - UserMenuAdapter (3-col Grid)  |          |  - CategoryManagementFragment    |
|  - GuestCartDialogFragment       |          |  - SettingsFragment              |
|  - CartManager (In-Memory State) |          |  - AdminMenuAdapter              |
+----------------------------------+          +----------------------------------+
```

---

## 📂 Структура каталогов

```
kgz.senior.dishkit/
├── model/
│   ├── AppSettings.java            # Глобальные настройки (название, логотип, тумблеры видимости)
│   ├── Category.java               # Модель категории (id, name, order, parentId)
│   ├── MenuItem.java               # Модель блюда (id, name, description, price, imageUrl, isVisible, order)
│   └── CartItem.java               # Элемент корзины гостя (блюдо + количество + сумма)
├── repository/
│   ├── CartManager.java            # Потокобезопасный Singleton управления корзиной гостя
│   └── MenuRepository.java         # Репозиторий для взаимодействия с Firestore и Firebase Storage
├── utils/
│   └── Constants.java              # Константы коллекций, полей документов и путей Storage
├── view/
│   ├── MainActivity.java           # Главный экран планшета (гостевое меню + 5-tap секретный триггер)
│   ├── CategoryListAdapter.java    # Адаптер вертикального списка категорий
│   ├── UserMenuAdapter.java        # Адаптер сетки карточек блюд (3 колонки)
│   ├── GuestCartDialogFragment.java# Модальное окно корзины с подсчетом стоимости
│   ├── GuestCartAdapter.java       # Адаптер элементов корзины гостя
│   └── admin/
│       ├── LoginActivity.java      # Экран авторизации администратора
│       ├── AdminPanelActivity.java # Главный контейнер панели администратора (TabLayout + ViewPager2)
│       ├── DishesFragment.java     # Управление блюдами (добавление, сортировка, редактирование)
│       ├── CategoryManagementFragment.java # Управление категориями и подкатегориями
│       ├── SettingsFragment.java   # Настройки ресторана и переключатели отображения
│       ├── AdminMenuAdapter.java   # Адаптер списка блюд для администратора
│       ├── CategoryManagementAdapter.java # Адаптер древовидных категорий
│       └── AdminPagerAdapter.java  # ViewPager2 адаптер вкладок админ-панели
└── viewmodel/
    └── AdminViewModel.java         # Центральная ViewModel для управления состоянием и данными
```

---

## 🗄 Модель данных Firebase Firestore

### 1. Коллекция `menu_items` (Блюда)
```json
{
  "id": "dish_abc123",
  "name": "Шашлык из баранины",
  "description": "Нежная баранина, маринованная в травах, подается с маринованным луком и лавашем",
  "category": "Горячие блюда",
  "categoryId": "cat_hot_dishes_1",
  "price": 450.0,
  "imageUrl": "https://firebasestorage.googleapis.com/.../dish_images/lamb_shashlik.jpg",
  "order": 1,
  "isVisible": true
}
```

### 2. Коллекция `categories` (Категории)
```json
{
  "id": "cat_hot_dishes_1",
  "name": "Горячие блюда",
  "order": 1,
  "parentId": null
}
```

### 3. Документ `config/main_settings` (Настройки заведения)
```json
{
  "cafeName": "Чайхана Нават",
  "logoUrl": "https://firebasestorage.googleapis.com/.../logos/logo.png",
  "isImageVisible": true,
  "isDescriptionVisible": true,
  "isPriceVisible": true,
  "isGuestCartVisible": true,
  "adminDishSortType": "ORDER"
}
```

---

## ⚙️ Технологический стек

| Компонент | Стек / Версия | Назначение |
| :--- | :--- | :--- |
| **Язык** | Java 11 | Основной язык разработки Android |
| **Минимальная версия ОС** | Android 7.0 (API 24) | Поддержка широкого парка планшетов |
| **Целевая версия ОС** | Android 14+ (API 36) | Соответствие актуальным требованиям Google Play |
| **Сборка** | Gradle Kotlin DSL (`build.gradle.kts`) | Современный скрипт сборки |
| **База данных** | Cloud Firestore | Хранение данных в реальном времени |
| **Хранилище файлов** | Firebase Storage | Загрузка и отдача изображений и логотипов |
| **Аутентификация** | Firebase Authentication | Безопасный доступ сотрудников к админке |
| **Загрузка изображений**| Glide 4.16.0 | Асинхронная подгрузка и кэширование фото |
| **UI Библиотека** | Google Material Design 3 | Современные компоненты интерфейса |
| **Архитектура** | MVVM + LiveData + Repository | Чистое разделение ответственности |

---

## 🚀 Быстрый старт и установка

### 1. Клонирование репозитория
```bash
git clone https://github.com/toctosunov/DishKit_lite.git
cd DishKit_lite
```

### 2. Подключение Firebase к проекту
1. Зайдите в [Firebase Console](https://console.firebase.google.com/) и создайте новый проект.
2. Включите сервисы:
   - **Authentication**: активируйте способ входа **Email/Password**.
   - **Cloud Firestore**: создайте базу данных в режиме Production или Test.
   - **Firebase Storage**: создайте корзину хранения для загрузки фото блюд и логотипа.
3. Добавьте Android-приложение с package name:
   ```
   kgz.senior.dishkit
   ```
4. Скачайте сгенерированный файл **`google-services.json`** и поместите его в папку:
   ```
   DishKit_lite/app/google-services.json
   ```

### 3. Сборка и запуск в Android Studio
1. Откройте проект в **Android Studio Hedgehog / Iguana / Ladybug** или новее.
2. Дождитесь завершения Gradle Sync.
3. Подключите планшет через USB (или запустите эмулятор планшета в альбомной ориентации).
4. Нажмите **Run** (`Shift + F10`).

---

## 🔒 Настройка безопасности Firebase

Для защиты данных от неавторизованного редактирования рекомендуется настроить правила **Firestore Rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Любой гость планшета может читать меню и настройки
    match /menu_items/{item} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    match /categories/{category} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    match /config/{setting} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

Правила для **Firebase Storage Rules**:
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /logos/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    match /dish_images/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

---

## 👤 Автор и контакты

- **Разработчик:** Мирбек Токтосунов ([@toctosunov](https://github.com/toctosunov))
- **Email:** toktosunovmirbek75@gmail.com
- **Репозиторий проекта:** [https://github.com/toctosunov/DishKit_lite](https://github.com/toctosunov/DishKit_lite)

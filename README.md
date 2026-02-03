# ⚔️ LifeTracker RPG: Fullstack Task Manager

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![C#](https://img.shields.io/badge/C%23-239120?style=for-the-badge&logo=c-sharp&logoColor=white)
![ASP.NET Core](https://img.shields.io/badge/ASP.NET_Core-512BD4?style=for-the-badge&logo=dotnet&logoColor=white)
![MSSQL](https://img.shields.io/badge/SQL_Server-CC2927?style=for-the-badge&logo=microsoft-sql-server&logoColor=white)

> **LifeTracker** — это не просто таск-менеджер. Это RPG-движок для твоей жизни. Превращай реальные задачи в опыт, золото и уровни.

---

## 🌌 О Проекте

Проект представляет собой полноценную **Client-Server** экосистему. Основная идея — перенести механику "Habitica" на собственный стек технологий для гибкого управления личной эффективностью.

### 🕹️ Игровые механики
| Фича | Описание | Реализация |
| :--- | :--- | :--- |
| **HP (Health)** | Теряется при провале "плохих" привычек | Server-side validation |
| **XP (Experience)** | Начисляется за закрытые задачи | Dynamic Leveling System |
| **Gold** | Валюта для покупки айтемов и наград | Database Persistent |
| **Level Up** | Повышение уровня при достижении лимита XP | Full State Sync |

---

## 🛠️ Архитектура и Стек



### 📱 Mobile (Frontend)
- **UI Framework:** Jetpack Compose (Modern Declarative UI)
- **Networking:** Retrofit 2 + OkHttp 
- **Architecture:** MVVM (Model-View-ViewModel)
- **State Management:** StateFlow & Compose State

### 🖥️ Backend (API)
- **Framework:** ASP.NET Core 8.0 Web API
- **ORM:** Entity Framework Core
- **Database:** MS SQL Server (LocalDB)
- **Security:** Серверная обработка всей логики наград и штрафов

---

## 📂 Структура проекта

```bash
root/
├── LifeTrackerMobile/     # 📱 Android приложение (Kotlin)
│   ├── app/src/main/      # Исходный код и ресурсы
│   └── build.gradle       # Конфигурация сборки
└── LifeTrackerBackend/    # ⚙️ Серверная часть (C#)
    ├── Controllers/       # Обработка API запросов
    ├── Models/            # Сущности базы данных
    └── appsettings.json   # Конфигурация сервера

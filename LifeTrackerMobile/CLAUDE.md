# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

*   **Build Project**: `./gradlew assembleDebug`
*   **Run Unit Tests**: `./gradlew test`
*   **Run Specific Test**: `./gradlew testDebugUnitTest --tests "com.lifetracker.mobile.ExampleUnitTest"`
*   **Run Instrumented Tests**: `./gradlew connectedCheck`
*   **Linting**: `./gradlew lint`
*   **Clean Project**: `./gradlew clean`
*   **Generate Room Schemas**: `./gradlew kspDebugKotlin`

## Architecture & Structure

The project follows Clean Architecture principles with Jetpack Compose for the UI.

### Directory Structure
*   `app/src/main/java/com/lifetracker/mobile/`
    *   `core/`: Shared components like network helpers and utility classes.
    *   `data/`: Implementation of repositories, local (Room) and remote (Retrofit) data sources, and data mappers.
    *   `domain/`: Pure business logic containing Domain Models, Repository Interfaces, and Use Cases.
    *   `ui/`: UI components, screens, ViewModels, and state management using Jetpack Compose.
    *   `di/`: Dependency injection configuration (Koin).
    *   `navigation/`: Navigation graph and route definitions.

### Tech Stack
*   **UI**: Jetpack Compose with Material 3.
*   **Dependency Injection**: Koin.
*   **Networking**: Retrofit with OkHttp and `kotlinx-serialization`.
*   **Database**: Room with KSP.
*   **Concurrency**: Kotlin Coroutines & Flow.
*   **Date/Time**: `kotlinx-datetime`.

## Code Conventions
*   **Architecture**: Always separate logic into Domain (UseCases/Models) and Data (Repositories/DTOs).
*   **Mapping**: Use dedicated Mapper classes to convert between DTOs, Domain models, and UI states.
*   **UI State**: Prefer `StateFlow` in ViewModels for exposing UI state to Compose.
*   **DI**: Register new services, repositories, and viewmodels in `AppModule.kt`.
*   **Configuration**: Environment-specific variables (like `BASE_URL`) are handled via `local.properties` and `BuildConfig`.

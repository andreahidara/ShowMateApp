# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew clean                  # Clean build artifacts
./gradlew build                  # Full build
```

**TMDB API token** must be set in `local.properties` as `TMDB_API_TOKEN=<your_token>` (git-ignored). It is injected as a BuildConfig field via `app/build.gradle.kts`.

## Architecture

MVVM + Clean Architecture in a single-Activity Jetpack Compose app.

### Layers

**Data** (`data/`)
- `repository/` — Three repositories: `ShowRepository` (TMDB media, offline cache), `AuthRepository` (Firebase Auth), `UserRepository` (Firestore profile + interaction tracking)
- `local/` — Room database v3 (`AppDatabase`) with `ShowDao`; caches media by category (trending, popular, recommended) for offline fallback
- `network/` — Retrofit + OkHttp; `TmdbApiService` targets TMDB API with `es-ES` locale
- `model/` — `MediaEntity` (Room), `UserProfile` (Firestore), `MediaContent` (TMDB response)

**Domain** (`domain/usecase/`)
- `GetRecommendationsUseCase` — Hybrid scoring: 70% personal affinity (genres 0.5, keywords 0.3, actors 0.2) + 30% Bayesian global rating. Min-Max normalization prevents weight accumulation over time.
- `GetProfileStatsUseCase` — Aggregates user watch stats
- `UpdateUserInterestsUseCase` — Updates genre/keyword/actor scores after each interaction

**Presentation** (`ui/`)
- One `@HiltAndroidApp` Activity (`MainActivity`) hosting a `NavHost`
- Each screen has a paired `@HiltViewModel` exposing `StateFlow<UiState>`
- `AppNavigation.kt` wraps the entire nav graph in `SharedTransitionLayout` for animated detail transitions
- `Screens.kt` defines type-safe routes via `@Serializable` data objects (kotlinx.serialization)
- Theme is **forced dark** (`Theme.kt`); color palette in `Color.kt`

**DI** (`di/`) — Hilt modules: `DatabaseModule`, `NetworkModule`, `FirebaseModule`

**Util** (`util/`)
- `Resource<T>` — sealed class wrapping Loading/Success/Error states
- `SafeApiCall.kt` — suspending wrapper for Retrofit calls with graceful error handling
- `UiText.kt` — abstraction over string resources vs. raw strings

### User Interaction Model

`UserProfile` (Firestore) tracks genre scores, keywords, and actors as weighted maps. Interaction weights:
| Action | Score delta |
|---|---|
| Like | +5 |
| Essential | +10 |
| Dislike | −2 |
| Rate | adaptive |
| Watched | +3 |

### Navigation Flow

`SplashScreen` → checks auth state → `LoginScreen` / `MainScreen`
`MainScreen` hosts bottom nav with tabs: Home, Discover, Swipe, Favorites, Profile
Detail views use shared element transitions from any tab.

## Key Libraries

| Purpose | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose 2.8 (type-safe) |
| DI | Dagger Hilt 2.52 |
| Networking | Retrofit 2.9 + OkHttp |
| Database | Room 2.6 |
| Auth & Cloud | Firebase Auth + Firestore + FCM |
| Image loading | Coil 2.7 |
| Serialization | kotlinx.serialization 1.7 |
| Background | WorkManager 2.9 |

Min SDK: 26 | Target/Compile SDK: 35 | Kotlin 2.0.21 | AGP 8.13.2

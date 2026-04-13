# ▷ ShowMate — Recomendación Personalizada de Series y Películas

> Aplicación Android nativa que aprende de tus gustos para recomendarte exactamente lo que te va a gustar ver.

[![CI — Main](https://github.com/andreahidara/ShowMateApp/actions/workflows/main.yml/badge.svg)](https://github.com/andreahidara/ShowMateApp/actions/workflows/main.yml)
[![PR Checks](https://github.com/andreahidara/ShowMateApp/actions/workflows/pr.yml/badge.svg)](https://github.com/andreahidara/ShowMateApp/actions/workflows/pr.yml)
[![Release](https://github.com/andreahidara/ShowMateApp/actions/workflows/release.yml/badge.svg)](https://github.com/andreahidara/ShowMateApp/actions/workflows/release.yml)

![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![TMDB](https://img.shields.io/badge/TMDB-API%20v3-01B4E4?style=flat-square)

---

## 📱 ¿Qué es ShowMate?

ShowMate resuelve la **sobrecarga de elección** en las plataformas de streaming. En lugar de perderte entre miles de títulos, ShowMate aprende de cada interacción tuya — valoraciones, favoritos, swipes — y genera recomendaciones cada vez más precisas, de forma transparente y sin depender del catálogo de ninguna plataforma.

---

## ✨ Funcionalidades

- **Onboarding con selección de géneros** — establece tu perfil desde el primer uso
- **Swipe de calibración** — desliza tarjetas para afinar el algoritmo al instante
- **Recomendaciones personalizadas** — pantalla Home y sección Discover con tu match %
- **Sección Up Next** — seguimiento de series y episodios que estás viendo
- **Recomendaciones explicables (XAI)** — el sistema te muestra por qué te recomienda cada título
- **Red Social P2P (Friends)** — feed en tiempo real de lo que ven tus amigos y cálculo de compatibilidad 'Match'
- **Dashboard Estadístico Nativo** — visualiza tus estadísticas con gráficas renderizadas puramente en `Canvas` (cero dependencias externas)
- **Búsqueda y Filtros** — búsqueda ágil con chips dinámicos
- **Detalle completo con Shared Transitions** — animaciones fluidas Material 3 desde la portada hasta el detalle de la serie
- **Tráileres integrados** — reproductor de YouTube in-app
- **Gestión Offline First** — funciona sin conexión gracias a RoomDatabase acoplada a Single Source of Truth

---

## 🧠 Cómo funciona el algoritmo

Cada título recibe una **puntuación final** combinando preferencias personales y calidad global:

```
Puntuación Final = 0.7 × Afinidad Personal + 0.3 × Calidad Global (Bayesiana)
```

La **afinidad personal** se construye a partir de pesos dinámicos por géneros, palabras clave y actores (`0.5 / 0.3 / 0.2`), normalizados con Min-Max para evitar la acumulación de pesos con el tiempo.

Los pesos se actualizan con cada interacción:

| Acción | Impacto |
|---|---|
| Swipe derecho (like) | +5 pts |
| Marcar como esencial | +10 pts |
| Swipe izquierdo (skip) | −2 pts |
| Añadir a favoritos | +5 pts |
| Eliminar de favoritos | −2 pts |
| Marcar como vista | +3 pts |
| Valorar (1–5 estrellas) | adaptativo |

Los títulos ya vistos o rechazados **nunca vuelven a aparecer** en las recomendaciones gracias al filtrado excluyente en la capa de datos. Además, el motor incluye **Machine Learning ligero** identificando Patrones Temporales (si el usuario es *Binge-Watcher* o *Espectador Casual*).

---

## 🏗️ Arquitectura

ShowMate sigue **MVI (Model-View-Intent) + Clean Architecture** con tres capas bien separadas:

```
📦 ShowMateApp
 ┣ 📂 data/          → Repositories · TMDB API (Retrofit) · Room · Firebase
 ┣ 📂 domain/        → Use Cases (algoritmo de recomendación, estadísticas, intereses)
 ┗ 📂 ui/            → Pantallas Compose + ViewModels (MVI: State, Events, Effects)
```

- Interfaz 100% declarativa dividida en micro-funciones `@Composable` (State Hoisting)
- Navegación moderna con animaciones experimentales de **SharedTransitionScope**
- Red de datos segura: Interceptores de red (`HttpLoggingInterceptor`) restringidos solo a compilaciones `DEBUG` para proteger las credenciales en Producción.
- Carga de UI concurrente (*Concurrent Fetching*) a través de **Coroutines async/awaitAll()** para no bloquear el Hilo Principal.
- **Single Source of Truth**: Room hace de caché local para que la UI jamás espere pasivamente a la red.

---

## 🛠️ Stack tecnológico

| Tecnología | Uso |
|---|---|
| **Kotlin 2.0** | Lenguaje principal |
| **Jetpack Compose + Material 3** | UI declarativa |
| **Firebase Authentication** | Registro e inicio de sesión |
| **Firebase Firestore** | Perfil de usuario y pesos del algoritmo en la nube |
| **Firebase Cloud Messaging** | Notificaciones push |
| **Room 2.6** | Caché local para modo offline |
| **Retrofit 2 + OkHttp** | Consumo de la API de TMDB |
| **Hilt 2.52** | Inyección de dependencias |
| **Coil 2.7** | Carga asíncrona de imágenes fluidas |
| **Navigation Compose 2.8** | Navegación type-safe |
| **Coroutines + Flow** | Operaciones asíncronas concurrentes |
| **Mockito** | Tests unitarios |

---

## 🚀 Configuración y puesta en marcha

### Requisitos previos

- Android Studio Hedgehog o superior
- JDK 17
- Cuenta en [Firebase](https://firebase.google.com) (plan Spark gratuito)
- Token de API de [The Movie Database (TMDB)](https://www.themoviedb.org/settings/api)

### 1. Clonar el repositorio

```bash
git clone https://github.com/andreahidara/ShowMateApp.git
cd ShowMateApp
```

### 2. Configurar TMDB

Crea el archivo `secret.properties` en la raíz del proyecto:

```properties
TMDB_API_TOKEN=<tu_token_aqui>
```

> **Nota:** Introduce solo el "API Read Access Token" largo. El sistema añadirá automáticamente el prefijo "Bearer".

> `secret.properties` está en `.gitignore` y nunca se sube al repositorio.

### 3. Conectar Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com) y crea un nuevo proyecto
2. Añade una app Android con el package name `com.example.showmateapp`
3. Descarga `google-services.json` y colócalo en `app/`
4. Activa **Authentication** → método Email/Contraseña
5. Activa **Firestore Database** en modo de prueba

### 4. Ejecutar

Abre el proyecto en Android Studio, sincroniza Gradle y ejecuta en un emulador o dispositivo físico con **Android 8.0 (API 26) o superior**.

---

## 📂 Estructura de datos en Firestore

```
users/
  {uid}/
    ├── displayName
    ├── email
    ├── genreScores        → { "18": 25, "35": 15, ... }
    ├── preferredKeywords  → { "detective": 10, ... }
    ├── preferredActors    → { "actor_id": 8, ... }
    ├── likedMediaIds      → [1234, 5678, ...]
    ├── dislikedMediaIds   → [9012, ...]
    ├── favorites/
    │     {showId} → datos básicos del título
    ├── views/
    │     {showId} → datos básicos del título
    └── ratings/
          {showId} → { rating: 4, timestamp: ... }
```

---

## 🧪 Tests

Los tests unitarios cubren `GetRecommendationsUseCase` y el sistema de pesos del algoritmo:

```bash
./gradlew test                   # Tests unitarios
./gradlew connectedAndroidTest   # Tests instrumentados (requiere dispositivo/emulador)
```

---

## 👩‍💻 Autora

**Andrea Hidalgo Arana**
Proyecto de Fin de Grado — Desarrollo de Aplicaciones Multiplataforma
Prometeo by thePower | Arturo Soria · 2025-2026
Tutor: José Manuel Villar Ferradal

---

## 📄 Licencia

Proyecto académico. Los datos de series y películas son propiedad de [The Movie Database (TMDB)](https://www.themoviedb.org).
Este producto usa la API de TMDB pero no está respaldado ni certificado por TMDB.

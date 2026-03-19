# ▷ ShowMate — Recomendación Personalizada de Series

> Aplicación Android nativa que aprende de ti para recomendarte exactamente lo que te va a gustar ver.

![Android](https://img.shields.io/badge/Android-API%2034-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%2B%20Auth-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![TMDB](https://img.shields.io/badge/TMDB-API%20v3-01B4E4?style=flat-square)

---

## 📱 ¿Qué es ShowMate?

ShowMate resuelve la **sobrecarga de elección** en las plataformas de streaming. En lugar de perderte entre miles de títulos, ShowMate aprende de cada interacción tuya — valoraciones, favoritos, swipes — y genera recomendaciones cada vez más precisas, de forma transparente y sin depender del catálogo de ninguna plataforma.

---

## ✨ Funcionalidades principales

- **Onboarding con selección de géneros** — establece tu perfil desde el primer uso
- **Swipe de calibración** — desliza tarjetas para afinar el algoritmo al instante
- **Recomendaciones personalizadas** — pantalla Home y sección Discover con tu match %
- **Búsqueda avanzada** — filtra por género, año de estreno y puntuación mínima
- **Detalle completo de serie** — sinopsis, reparto, temporadas, géneros desde TMDB
- **Sistema de valoración** — puntúa de 1 a 5 estrellas y actualiza tu perfil automáticamente
- **Favoritos y series vistas** — gestión completa con impacto en el algoritmo
- **Perfil con estadísticas** — horas vistas, favoritos totales y géneros top
- **Reinicio de gustos** — vuelve a empezar desde cero cuando quieras
- **Modo offline** — funciona sin conexión con la caché local de Room

---

## 🧠 Cómo funciona el algoritmo

Cada serie recibe una **puntuación final** calculada así:

```
Puntuación Final = 0.7 × Afinidad Personal + 0.3 × Calidad Global
```

La **afinidad personal** se construye a partir de pesos dinámicos por géneros, palabras clave y actores, que se actualizan con cada interacción:

| Acción | Impacto en pesos |
|---|---|
| ⭐⭐⭐⭐⭐ (5 estrellas) | +4 pts |
| ⭐⭐⭐⭐ (4 estrellas) | +2 pts |
| ⭐⭐⭐ (3 estrellas) | 0 pts |
| ⭐⭐ (2 estrellas) | -1 pts |
| ⭐ (1 estrella) | -3 pts |
| Añadir a favoritos | +5 pts |
| Eliminar de favoritos | -2 pts |
| Swipe derecho (like) | +5 pts |
| Swipe izquierdo (skip) | -2 pts |

Las series ya vistas o rechazadas **nunca vuelven a aparecer** en las recomendaciones.

---

## 🏗️ Arquitectura

ShowMate sigue **MVVM + Clean Architecture** con tres capas claramente separadas:

```
📦 ShowMate
 ┣ 📂 presentation/     → Pantallas Compose + ViewModels (StateFlow)
 ┣ 📂 domain/           → Use Cases (algoritmo de recomendación)
 ┗ 📂 data/             → Repositories → TMDB API / Room / Firebase
```

La inyección de dependencias se gestiona con **Hilt** en todas las capas.

---

## 🛠️ Stack tecnológico

| Tecnología | Uso |
|---|---|
| **Kotlin** | Lenguaje principal |
| **Jetpack Compose** | UI declarativa (13 pantallas) |
| **Firebase Authentication** | Registro e inicio de sesión |
| **Firebase Firestore** | Perfil de usuario y pesos del algoritmo en la nube |
| **Room (SQLite)** | Caché local de series para modo offline |
| **Retrofit + Gson** | Consumo de la API de TMDB |
| **Hilt** | Inyección de dependencias |
| **Coil Compose** | Carga asíncrona de imágenes |
| **Navigation Compose** | Navegación type-safe entre pantallas |
| **Coroutines** | Operaciones asíncronas |
| **Firebase Cloud Messaging** | Notificaciones push |
| **WorkManager** | Tareas en background |
| **Mockito** | Tests unitarios |
| **Material Design 3** | Sistema de diseño |

---

## 🚀 Configuración y puesta en marcha

### Requisitos previos

- Android Studio Hedgehog o superior
- JDK 17
- Cuenta en [Firebase](https://firebase.google.com) (plan Spark gratuito)
- API key de [The Movie Database (TMDB)](https://www.themoviedb.org/settings/api)

### 1. Clonar el repositorio

```bash
git clone https://github.com/andreahidara/ShowMateApp.git
cd ShowMateApp
```

### 2. Configurar TMDB

Crea o edita el archivo `local.properties` en la raíz del proyecto y añade:

```properties
TMDB_API_KEY=tu_api_key_aqui
```

### 3. Conectar Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com) y crea un nuevo proyecto
2. Añade una app Android con el package name `com.andreahidara.showmate`
3. Descarga el archivo `google-services.json` y colócalo en `/app/`
4. Activa **Authentication** → método Email/Contraseña
5. Activa **Firestore Database** en modo de prueba

### 4. Ejecutar

Abre el proyecto en Android Studio, sincroniza Gradle y ejecuta en un emulador con **API 34 (Android 14)** o dispositivo físico.

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
    │     {showId} → datos básicos de la serie
    ├── views/
    │     {showId} → datos básicos de la serie
    └── ratings/
          {showId} → { rating: 4, timestamp: ... }
```

---

## 🧪 Tests

Los tests unitarios cubren el `GetRecommendationsUseCase` y el sistema de pesos. Para ejecutarlos:

```bash
./gradlew test
```

---

## 📸 Capturas de pantalla

| Splash | Login | Onboarding | Home |
|---|---|---|---|
| *(próximamente)* | *(próximamente)* | *(próximamente)* | *(próximamente)* |

---

## 👩‍💻 Autora

**Andrea Hidalgo Arana**  
Proyecto de Fin de Grado — Desarrollo de Aplicaciones Multiplataforma  
Prometeo by thePower | Arturo Soria · 2025-2026  
Tutor: José Manuel Villar Ferradal

---

## 📄 Licencia

Proyecto académico. Los datos de series son propiedad de [The Movie Database (TMDB)](https://www.themoviedb.org).  
Este producto usa la API de TMDB pero no está respaldado ni certificado por TMDB.

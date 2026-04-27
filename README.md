# ▷ ShowMate — Recomendación Personalizada de Series

> Aplicación Android nativa que aprende de tus gustos para recomendarte exactamente lo que te va a gustar ver.

![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![TMDB](https://img.shields.io/badge/TMDB-API%20v3-01B4E4?style=flat-square)

---

## 📱 ¿Qué es ShowMate?

ShowMate resuelve la **sobrecarga de elección** en las plataformas de streaming. En lugar de perderte entre miles de títulos, ShowMate aprende de cada interacción tuya — valoraciones, favoritos, swipes — y genera recomendaciones cada vez más precisas, de forma transparente y sin depender del catálogo de ninguna plataforma.

---

## ✨ Funcionalidades

### Descubrimiento y recomendaciones
- **Onboarding personalizado** — 5 pasos: géneros, series vistas, preferencias y análisis de personalidad (8 arquetipos de espectador).
- **Swipe de calibración** — desliza tarjetas para afinar el algoritmo al instante.
- **Recomendaciones personalizadas** — pantalla Home y Discover con tu match % calculado en tiempo real.
- **Recomendaciones explicables (XAI)** — el sistema te muestra por qué te recomienda cada título con barras animadas por categoría.
- **Up Next** — seguimiento inteligente de series y episodios, con marcado automático de serie como vista al completar todos los episodios.

### Detalle y contenido
- **Detalle completo con Shared Transitions** — animaciones fluidas Material 3.
- **Tráileres integrados** — reproductor de YouTube in-app.
- **Seguimiento de episodios** — marca episodios individuales, temporadas completas o toda la serie.
- **Reseñas sociales** — escribe y lee valoraciones de otros usuarios.
- **Valoración por estrellas** — sistema de rating 1–5 con histograma de distribución.

### Social
- **Red de amigos** — envía y acepta solicitudes, feed de actividad en tiempo real.
- **Compatibilidad de gustos** — cálculo de afinidad entre perfiles de usuarios.
- **Group Match** — sesión colaborativa para encontrar una serie que guste a todo el grupo.

### Perfil y estadísticas
- **Estadísticas estilo Wrapped** — resumen anual con horas, géneros, países, racha y tipo de espectador.
- **Listas personalizadas** — crea y gestiona colecciones propias de series.
- **Gamificación** — logros desbloqueables, sistema de XP y niveles, racha de visualización diaria.
- **Perfil psicológico** — 8 arquetipos detectados automáticamente a partir de tus interacciones.

### Técnicas y plataforma
- **Búsqueda con Paging 3** — autocompletado y chips de filtro, carga eficiente paginada.
- **Offline First** — caché local con Room, la app funciona sin conexión.
- **Notificaciones inteligentes** — aviso de nuevas temporadas (SeasonCheckWorker) y recordatorio de racha (StreakReminderWorker) vía WorkManager + FCM.
- **Google Sign-In** — inicio de sesión con cuenta Google además de email/contraseña.
- **Cumplimiento GDPR** — pantalla de consentimiento obligatoria antes del primer uso.

---

## 🧠 Algoritmo de recomendación

Cada título recibe una **puntuación final** combinando preferencias personales y calidad global:

```
Puntuación Final = 0.70 × Afinidad Personal + 0.30 × Calidad Global (Bayesiana)
```

La **afinidad personal** se construye a partir de cinco dimensiones con pesos dinámicos:

| Dimensión | Peso | Descripción |
|---|---|---|
| Géneros | 37% | Favoritos con decaimiento temporal exponencial (semivida 90 días) |
| Keywords | 22% | Temas, ambientaciones y elementos temáticos |
| Estilo narrativo | 19% | 11 clusters detectados automáticamente desde keywords TMDB |
| Actores | 12% | Reparto con el que el usuario ha interactuado positivamente |
| Creadores | 10% | Showrunners y directores de series mejor valoradas |

Los pesos se actualizan con cada interacción:

| Acción | Impacto |
|---|---|
| Swipe derecho / like | +5 pts |
| Marcar como esencial | +10 pts |
| Swipe izquierdo / skip | −2 pts |
| Añadir a favoritos | +5 pts |
| Eliminar de favoritos | −2 pts |
| Marcar como vista | +3 pts |
| Valorar (1–5 estrellas) | adaptativo |

**Características avanzadas:** valoración bayesiana, filtrado colaborativo con boost de popularidad, serendipia (15% de descubrimientos de alta valoración), y diversidad garantizada (ningún género ocupa >35% de resultados).

---

## 🏗️ Arquitectura

ShowMate sigue **MVVM + Clean Architecture** con separación estricta en tres capas:

```
ui/          → Compose screens, ViewModels, componentes
domain/      → Use cases, interfaces de repositorio
data/        → Repositorios, Room, Retrofit, Firebase, Workers
```

- **Hilt** para inyección de dependencias en todas las capas.
- **StateFlow + collectAsStateWithLifecycle** para UI reactiva sin leaks.
- **Room** como Single Source of Truth con caché offline.
- **Paging 3** para listas grandes desde TMDB.
- **Type-safe Navigation** con rutas serializadas.
- **Detekt + Ktlint** para análisis estático de código.

---

## 🛠️ Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| **Kotlin** | 2.0.21 | Lenguaje principal |
| **Jetpack Compose BOM** | 2024.10.01 | UI declarativa con Material 3 |
| **Firebase Suite** | — | Auth, Firestore, Messaging, Crashlytics, App Check |
| **Room** | 2.6.1 | Persistencia local (DB v13) |
| **Retrofit 2** | 2.11.0 | Comunicación con TMDB API |
| **Hilt** | 2.51.1 | Inyección de dependencias |
| **WorkManager** | 2.9.1 | Workers en segundo plano |
| **Coil** | 2.7.0 | Carga de imágenes con caché |
| **Paging 3** | 3.3.2 | Listas paginadas |

---

## 🚀 Configuración y puesta en marcha

### Requisitos previos

- Android Studio Ladybug o superior
- JDK 17
- Cuenta en [Firebase](https://firebase.google.com)
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

### 3. Conectar Firebase

1. Descarga `google-services.json` desde Firebase Console y colócalo en `app/`.
2. Habilita Email/Password Auth, Google Sign-In y Firestore.

---

## 🧪 Tests

```bash
./gradlew test                        # Tests unitarios (~284 tests)
./gradlew connectedAndroidTest        # Tests instrumentados en emulador
```

Cobertura: repositorios, ViewModels, use cases (recomendaciones, achievements, stats), mappers y workers.

---

## 👩‍💻 Autora

**Andrea Hidalgo Arana**
Proyecto de Fin de Grado — Desarrollo de Aplicaciones Multiplataforma
Prometeo by thePower | Arturo Soria · 2025–2026
Tutora/Tutor: José Manuel Villar Ferradal

---

## 📄 Licencia

Proyecto académico. Los datos son propiedad de TMDB. No apto para uso comercial sin autorización de los propietarios de los datos.

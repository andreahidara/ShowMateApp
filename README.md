# ▷ ShowMate — Recomendación Personalizada de Series y Películas

> Aplicación Android nativa que aprende de tus gustos para recomendarte exactamente lo que te va a gustar ver.

![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![TMDB](https://img.shields.io/badge/TMDB-API%20v3-01B4E4?style=flat-square)

---

## 📱 ¿Qué es ShowMate?

ShowMate resuelve la **sobrecarga de elección** en las plataformas de streaming. En lugar de perderte entre miles de títulos, ShowMate aprende de cada interacción tuya — valoraciones, favoritos, swipes — y genera recomendaciones cada vez más precisas, de forma transparente y sin depender del catálogo de ninguna plataforma.

---

## ✨ Funcionalidades (Versión 1.0)

- **Onboarding con selección de géneros** — establece tu perfil desde el primer uso.
- **Swipe de calibración** — desliza tarjetas para afinar el algoritmo al instante.
- **Recomendaciones personalizadas** — pantalla Home y sección Discover con tu match % calculado en tiempo real.
- **Sección Up Next** — seguimiento inteligente de series y episodios que estás viendo.
- **Recomendaciones explicables (XAI)** — el sistema te muestra por qué te recomienda cada título basándose en tus intereses.
- **Red Social P2P (Friends)** — feed en tiempo real de lo que ven tus amigos y cálculo de compatibilidad 'Match'.
- **Dashboard Estadístico Nativo** — visualiza tus estadísticas con gráficas avanzadas renderizadas en `Canvas`.
- **Búsqueda y Filtros** — búsqueda ágil con chips dinámicos y autocompletado.
- **Detalle completo con Shared Transitions** — animaciones fluidas Material 3 para una experiencia premium.
- **Tráileres integrados** — reproductor de YouTube in-app sin salir de la ficha.
- **Gestión Offline First** — soporte completo sin conexión gracias a RoomDatabase.
- **Notificaciones Inteligentes** — recordatorios de nuevos episodios y recomendaciones semanales (WorkManager + FCM).

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

---

## 🏗️ Arquitectura y Calidad de Código

ShowMate sigue **MVI (Model-View-Intent) + Clean Architecture** con un enfoque en la mantenibilidad:

- **Type-safe Navigation**: Implementación robusta de navegación entre pantallas.
- **Hilt Dependency Injection**: Desacoplamiento total de componentes.
- **Static Analysis**: Configurado con `Detekt` y `Ktlint` para asegurar la calidad del código según los estándares de Kotlin.
- **Paging 3**: Carga eficiente de grandes listas de películas y series desde TMDB.
- **Single Source of Truth**: Room actúa como caché local, garantizando que la UI sea siempre reactiva.

---

## 🛠️ Stack tecnológico (Actualizado)

| Tecnología | Uso |
|---|---|
| **Kotlin 2.1.10** | Lenguaje moderno y eficiente |
| **Jetpack Compose (BOM 2024.10.01)** | UI declarativa con Material 3 |
| **Firebase Suite** | Auth, Firestore, Messaging, Crashlytics, Performance |
| **Room 2.8.4** | Persistencia local segura |
| **Retrofit 2 + OkHttp** | Comunicación con la API de TMDB |
| **Hilt 2.56** | Inyección de dependencias |
| **WorkManager** | Tareas en segundo plano (Recomendaciones periódicas) |
| **Coil 2.7** | Carga de imágenes con soporte para SVGs y Blur |

---

## 🚀 Configuración y puesta en marcha

### Requisitos previos

- Android Studio Ladybug o superior
- JDK 21
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

1. Descarga `google-services.json` desde Firebase Console y colócalo en la carpeta `app/`.
2. Habilita Email Auth y Firestore en el proyecto.

---

## 🧪 Tests

```bash
./gradlew test                   # Tests unitarios de lógica de negocio
```

---

## 👩‍💻 Autora

**Andrea Hidalgo Arana**
Proyecto de Fin de Grado — Desarrollo de Aplicaciones Multiplataforma
Prometeo by thePower | Arturo Soria · 2025-2026
Tutor: José Manuel Villar Ferradal

---

## 📄 Licencia

Proyecto académico. Los datos son propiedad de TMDB. No apto para uso comercial sin autorización de los propietarios de los datos.

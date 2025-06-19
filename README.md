# 🛰️ TrackerGPS - Cliente Android

Este repositorio contiene el código fuente del cliente nativo de Android para el sistema **TrackerGPS**. La aplicación está construida enteramente en **Kotlin** y utiliza las últimas tecnologías de desarrollo recomendadas por Google, incluyendo **Jetpack Compose** para la interfaz de usuario.

Su función principal es realizar un seguimiento de la ubicación del dispositivo en segundo plano de forma eficiente y resiliente, enviando los datos al [servidor backend del proyecto](https://github.com/dothardapp/LaravelGPSTracker).

## 📸 Capturas de Pantalla

*(Aquí puedes añadir capturas de pantalla de tu aplicación)*

| Pantalla de Localización | Menú Lateral | Pantalla de Configuración |
| :---: | :---: | :---: |
| *(Tu captura aquí)* | *(Tu captura aquí)* | *(Tu captura aquí)* |

## ✨ Características Principales

* **Seguimiento en Segundo Plano:** Utiliza un `ForegroundService` para garantizar que el seguimiento continúe incluso si la aplicación no está en pantalla, mostrando una notificación persistente al usuario.
* **Tracking Inteligente:** Solo obtiene y envía actualizaciones de ubicación cuando el dispositivo está en movimiento (distancia mínima configurable), optimizando drásticamente el consumo de batería.
* **Resiliencia Offline:** Guarda automáticamente los puntos de localización en una base de datos local (Room) si se pierde la conexión a internet o falla la comunicación con el servidor.
* **Sincronización Automática:** Detecta cuándo se recupera la conexión a internet e intenta enviar todos los puntos que quedaron en la cola de espera.
* **Interfaz de Usuario Moderna:** Construida 100% con Jetpack Compose y Material 3, siguiendo un diseño reactivo y moderno.
* **Configuración Dinámica:** Permite seleccionar el usuario a rastrear desde una lista obtenida en tiempo real del servidor.

## 🏗️ Arquitectura y Stack Tecnológico

La aplicación sigue una arquitectura limpia basada en MVVM (Model-View-ViewModel) y la guía de arquitectura recomendada por Google, separando las responsabilidades en capas.

* **`UI (Vistas)`**: Las pantallas (`Screens`), construidas con Jetpack Compose.
* **`ViewModel`**: Mantiene y gestiona el estado de la UI, exponiéndolo a través de `StateFlow`.
* **`UseCase (Caso de Uso)`**: Contiene la lógica de negocio centralizada (ej. "obtener y enviar localización").
* **`Repository (Repositorio)`**: Es la única fuente de verdad para los datos, comunicándose con la API remota y la base de datos local.

### 🛠️ Tecnologías y Librerías

* **Lenguaje:** [Kotlin](https://kotlinlang.org/) 100%
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material 3.
* **Asincronía:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) & `Flow`.
* **Inyección de Dependencias:** Manual, a través de `ViewModelFactory`.
* **Navegación:** [Compose Navigation](https://developer.android.com/jetpack/compose/navigation).
* **Red:** [Retrofit 2](https://square.github.io/retrofit/) & [Gson](https://github.com/google/gson).
* **Base de Datos Local:** [Room](https://developer.android.com/training/data-storage/room) para la cola de envíos offline.
* **Preferencias:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) para guardar el usuario seleccionado.
* **Ubicación:** [Google Play Services - Fused Location Provider](https://developers.google.com/location-context/fused-location-provider).

## 📂 Estructura del Proyecto

El código está organizado en los siguientes paquetes principales:

* **`data`**: Contiene los modelos de datos (`model`), la capa de red (`network`), el repositorio (`repository`) y la base de datos local (`local`).
* **`domain`**: Contiene los casos de uso que encapsulan la lógica de negocio.
* **`service`**: Incluye el `LocationService` que gestiona el seguimiento en segundo plano.
* **`ui`**: Contiene las pantallas (Composables en la subcarpeta `screens` si se desea) y los `ViewModel`.
* **`util`**: Clases de ayuda y utilidades, como `PermissionsUtil` y `DebugLog`.

## 🚀 Instalación y Puesta en Marcha

Para compilar y ejecutar el proyecto, sigue estos pasos:

1.  **Clonar el Repositorio**
    ```bash
    git clone [https://github.com/dothardapp/TrackerGPS.git](https://github.com/dothardapp/TrackerGPS.git)
    ```

2.  **Abrir en Android Studio**
    Abre el proyecto con una versión reciente de Android Studio (por ejemplo, Hedgehog o superior). Gradle se sincronizará y descargará todas las dependencias necesarias.

3.  **Configurar la URL de la API (Paso CRÍTICO)**
    Para que la aplicación pueda comunicarse con tu backend, debes configurar la URL base.
    * Abre el archivo: `app/src/main/java/com/cco/tracker/data/network/RetrofitClient.kt`
    * Modifica la constante `BASE_URL` para que apunte a la dirección IP y puerto de tu servidor Laravel.
        ```kotlin
        // Ejemplo para un servidor local en la misma red Wi-Fi
        private const val BASE_URL = "[http://192.168.1.100:8000/api/](http://192.168.1.100:8000/api/)" 
        ```

4.  **Ejecutar la Aplicación**
    Compila y ejecuta la aplicación en un emulador o, preferiblemente, en un dispositivo físico para poder probar el GPS real.

### Requisito Previo

* Es necesario tener el backend [TrackerGPS - API del Servidor](https://github.com/dothardapp/LaravelGPSTracker) en funcionamiento y accesible desde la red en la que se encuentra el dispositivo Android.

## 🔮 Futuras Mejoras

- [ ] Mostrar la ruta del tracker en un mapa de Google Maps dentro de la app.
- [ ] Crear una pantalla de "Ajustes" para configurar la frecuencia de actualización, distancia mínima, etc.
- [ ] Mejorar la comunicación de estado entre el `Service` y la `UI` (por ejemplo, usando un `BroadcastReceiver` o un `StateFlow` compartido) para que el estado de `isTracking` sea 100% real.
- [ ] Implementar un sistema de autenticación de usuario (login/password).

---
*Este README fue generado con la ayuda de un asistente de IA.*
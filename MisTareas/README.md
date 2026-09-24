# Mis Tareas – Offline To-Do

Aplicación desarrollada para el **Punto 2 – Almacenamiento Local Permanente con SQLite / Room**
del Taller 3 de Persistencia en Android (Universidad de Nariño, Desarrollo de Aplicaciones Móviles).

## Descripción

Lista personal de tareas (To-Do List) con arquitectura **Offline-First**. Toda la
persistencia se realiza con **Room** sobre **SQLite**, sin ninguna dependencia de
Internet, Firebase, APIs REST ni sincronización remota.

## Tecnologías usadas

- Android Studio
- Kotlin
- Room Persistence Library (sobre SQLite)
- RecyclerView + ListAdapter/DiffUtil
- Material Design 3 (Material Components)
- Kotlin Coroutines + Flow (para observar los datos en tiempo real)
- View Binding

## Estructura del proyecto

```
MisTareas/
├── app/src/main/java/com/example/mistareas/
│   ├── MainActivity.kt
│   ├── data/Tarea.kt
│   ├── data/TareaDao.kt
│   ├── data/AppDatabase.kt
│   └── adapter/TareaAdapter.kt
├── app/src/main/res/layout/
│   ├── activity_main.xml
│   ├── item_tarea.xml
│   └── dialog_tarea.xml
└── README.md
```

## Modelo de datos (entidad `Tarea`)

| Campo             | Tipo    | Obligatorio | Uso                                  |
|-------------------|---------|-------------|---------------------------------------|
| id                | Int     | Sí          | Identificador único (clave primaria). |
| titulo            | String  | Sí          | Nombre de la tarea.                   |
| descripcion       | String  | No          | Detalle de la tarea.                  |
| estadoCompletado  | Boolean | Sí          | Indica si está completada.            |
| fechaCreacion     | Long    | Sí          | Fecha/hora de creación.               |

## Arquitectura

```
MainActivity  →  TareaDao  →  Room Database  →  SQLite local
```

La interfaz (`MainActivity`) trabaja directamente con el `TareaDao`, que accede a la
base de datos Room. Room utiliza SQLite como motor de almacenamiento local.

## Funcionalidades CRUD

- **Crear**: botón flotante (+) abre un formulario con título (obligatorio) y
  descripción (opcional). Al guardar, se inserta la tarea mediante `TareaDao.insertar()`.
- **Consultar**: al abrir la app, las tareas se leen desde Room (`Flow`) y se muestran
  en una `RecyclerView`, ordenadas por fecha de creación.
- **Actualizar**: al pulsar una tarea se abre el mismo formulario con los datos
  precargados; se puede cambiar el título, la descripción o el estado.
- **Marcar completada/pendiente**: mediante el `CheckBox` de cada tarjeta, sin
  necesidad de abrir el formulario.
- **Eliminar**: botón de papelera en cada tarjeta, con diálogo de confirmación.

## Funcionamiento Offline-First

Todas las operaciones (crear, consultar, actualizar, eliminar, marcar como
completada) se ejecutan directamente sobre la base de datos local SQLite a través
de Room. La ausencia de conexión a Internet **no afecta ninguna funcionalidad**,
porque la aplicación no realiza llamadas de red.

## Cómo abrir el proyecto en Android Studio

1. Abrir Android Studio.
2. Seleccionar **File → Open** y elegir la carpeta `MisTareas` (la carpeta raíz que
   contiene `settings.gradle`).
3. Esperar a que Android Studio sincronice Gradle (descargará automáticamente el
   Gradle Wrapper y las dependencias la primera vez que haya conexión a Internet).
4. Ejecutar la app (`Run ▶`) en un emulador o dispositivo físico con Android 7.0
   (API 24) o superior.

> Nota: el proyecto no incluye el binario `gradle-wrapper.jar` (no se puede generar
> sin acceso a red). Android Studio lo genera/descarga automáticamente al sincronizar
> el proyecto por primera vez. Si se prefiere, también se puede abrir con una
> instalación local de Gradle 8.4+.

## Pruebas de persistencia sugeridas

| Prueba                   | Procedimiento                                                                             |
|---------------------------|--------------------------------------------------------------------------------------------|
| Cierre y apertura         | Crear una tarea, cerrar completamente la app, abrirla de nuevo y comprobar que permanece. |
| Reinicio del dispositivo  | Crear tareas, cerrar la app, reiniciar el emulador/dispositivo y verificar que continúan. |
| Sin Internet               | Desactivar Wi-Fi y datos móviles. Crear, editar, completar y eliminar tareas normalmente. |
| Lectura local              | Sin conexión, abrir la app y comprobar que las tareas guardadas se muestran correctamente.|

## Flujo de demostración (sustentación)

1. Abrir la aplicación.
2. Crear una nueva tarea y mostrarla en la lista.
3. Cerrar y volver a abrir la app para demostrar persistencia.
4. Editar la tarea creada.
5. Marcarla como completada (checkbox).
6. Eliminar una tarea.
7. Desactivar Internet (Wi-Fi y datos móviles).
8. Crear una tarea sin conexión.
9. Cerrar y abrir de nuevo sin Internet y comprobar que la tarea permanece.
10. Explicar que Room utiliza SQLite como motor de almacenamiento local.

## Fuera de alcance (intencional)

Siguiendo la especificación del Punto 2, esta aplicación **no incluye**: inicio de
sesión, Firebase, API REST, Google Maps, notificaciones, chat, carga de imágenes,
base de datos remota, ContentProvider ni cifrado de base de datos. La sincronización
con una API remota corresponde al Punto 5 del taller y no se implementa aquí.

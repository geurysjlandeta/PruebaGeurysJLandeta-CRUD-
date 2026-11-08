# Enfoque

App Android (Kotlin + Jetpack Compose) para usar menos el celular y enfocarte más.

- **Hoy**: tiempo en pantalla de hoy y tus apps más usadas (`UsageStatsManager`).
- **Apps**: eliges apps distractoras y les pones un límite diario; al pasarlo se bloquean.
- **Enfoque**: temporizador (15/25/45/60 min) que bloquea tus apps distractoras por completo.

## Permisos (se piden desde la app)
1. **Acceso de uso**: medir el tiempo por app.
2. **Servicio de accesibilidad**: solo detecta el paquete de la app que se abre para bloquearla; `canRetrieveWindowContent=false`, no lee la pantalla.

## Ejecutar
Abre la carpeta `Enfoque/` en Android Studio (Koala o superior, JDK 17) y ejecuta en un celular o emulador (minSdk 26).

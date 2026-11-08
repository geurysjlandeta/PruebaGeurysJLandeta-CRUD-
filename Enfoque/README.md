# Enfoque

App Android (Kotlin + Jetpack Compose) para usar menos el celular y enfocarte más.

- **Hoy**: tiempo en pantalla de hoy y tus apps más usadas (`UsageStatsManager`).
- **Apps**: eliges apps distractoras y les pones un límite diario; al pasarlo se bloquean.
- **Enfoque**: temporizador (15/25/45/60 min) que bloquea tus apps distractoras por completo.

- **Launcher minimalista**: pantalla de inicio negra con reloj, favoritas (mantén pulsada una app para fijarla) y búsqueda. Las apps bloqueadas se ven con 🔒.
- **Horario automático**, **escala de grises** (requiere `adb shell pm grant com.geurys.enfoque android.permission.WRITE_SECURE_SETTINGS` una vez), **aviso al 80 % del límite** y **contador de desbloqueos**.

- **Mensajes (WhatsApp mínimo)**: bloquea WhatsApp en la pestaña Apps y responde en texto plano desde la pestaña Mensajes (lee las notificaciones y usa su botón "Responder"). Sin stickers, estados ni canales. Solo puedes responder a chats que te hayan escrito; no inicia chats nuevos.

- **Pausa consciente**: al abrir una app distractora (dentro de su límite) aparece una pausa de 5–30 s con "Mejor no" / "Continuar"; tras continuar, 5 min sin volver a pausar.
- **Modo noche**: bloqueo + grises en el horario que elijas (por defecto 23:00–7:00).
- **Fricción**: terminar el Enfoque o desactivar un horario activo exige esperar 15 s y confirmar.

## Permisos (se piden desde la app)
1. **Acceso de uso**: medir el tiempo por app.
3. **Acceso a notificaciones** (opcional): fuente alternativa para los mensajes de WhatsApp. Por defecto los mensajes se leen por el servicio de accesibilidad, que ya está activo, así que no hace falta este permiso.
2. **Servicio de accesibilidad**: solo detecta el paquete de la app que se abre para bloquearla; `canRetrieveWindowContent=false`, no lee la pantalla.

## Ejecutar
Abre la carpeta `Enfoque/` en Android Studio (Koala o superior, JDK 17) y ejecuta en un celular o emulador (minSdk 26).

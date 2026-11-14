# PC01 · Lotería en tiempo real (Android)

Proyecto de práctica independiente basado en el enunciado **Práctica Calificada 01 de Desarrollo de Aplicaciones Móviles, 2026-02**.

## Autor
- Nombre: **COMPLETAR**
- Código universitario: **COMPLETAR**

> El enunciado exige un repositorio con nombre `<codigoAlumno>-<apellidoPaterno>-pc01-dsm202602-loteria` y un video corto de la demostración. Cambia este README antes de entregar.

## Funcionalidad
- Kotlin + Jetpack Compose, Firebase Auth (correo/contraseña), Cloud Firestore.
- Una jugada por UID para `sorteo-prueba-2026`, 6 números únicos de 1 a 45.
- Jugadas inmutables una vez guardadas; bloqueo de nuevas jugadas con el sorteo cerrado.
- Escucha en tiempo real con `addSnapshotListener`, sin polling; listener de bolillas solo en la pantalla de seguimiento.
- Resultados locales con `LotteryLogic`: solo finales cuando estado = `finalizado` y existen 6 bolillas.
- Cada usuario solo puede leer su propia jugada según las reglas suministradas.

## Preparación requerida (no incluida en el ZIP)
1. Abre la carpeta raíz en **Android Studio** con JDK 17 y Android SDK 35.
2. Si Android Studio solicita sincronizar Gradle, acepta. Si no está presente el Gradle Wrapper JAR, crea el wrapper con Gradle 8.11.1 (`gradle wrapper --gradle-version 8.11.1`) o usa el Gradle instalado por tu equipo. `gradlew.bat` no está distribuido en este ZIP.
3. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/). Registra una app Android con el **applicationId** `pe.edu.demo.pc01loteria` (si cambias el ID, registra ese nuevo ID).
4. Descarga el archivo real `google-services.json` y guárdalo en **`app/google-services.json`**. El `.example` incluido NO sirve para compilar.
5. En **Authentication → Sign-in method**, habilita Email/Password.
6. Crea una base de datos Cloud Firestore. En la pestaña **Rules**, copia `firestore.rules` y **Publish**. Esas reglas no permiten que los clientes escriban bolillas o modifiquen estados: lo harás desde la consola de Firebase.
7. En Firestore crea colección `sorteos`, documento `sorteo-prueba-2026`:

| Campo | Tipo Firestore | Valor |
| --- | --- | --- |
| nombre | string | Sorteo de prueba PC01 |
| fecha | string | 2026-10-10 |
| estado | string | abierto |

8. Crea dos cuentas en la app desde los dos dispositivos. Guarda jugadas diferentes ANTES de cambiar el estado del sorteo.
9. En ambos dispositivos entra en **Seguir sorteo** para activar la escucha de bolillas.

## Demostración obligatoria
- **A:** `[5,12,18,23,34,41]` (debe terminar con **6 aciertos · GANADOR**).
- **B:** `[5,12,18,23,34,42]` (debe terminar con **5 aciertos · SIGUE INTENTANDO**).
- Cambia `sorteos/sorteo-prueba-2026.estado` a `en_curso` desde Firebase Console.
- Una por una, crea en `sorteos/sorteo-prueba-2026/bolillas` los documentos `01`, `02`, …, `06`. Para cada documento crea dos **campos numéricos** (`number` en Firestore):

| Documento | numero | orden |
| --- | ---: | ---: |
| 01 | 5 | 1 |
| 02 | 12 | 2 |
| 03 | 18 | 3 |
| 04 | 23 | 4 |
| 05 | 34 | 5 |
| 06 | 41 | 6 |

- Observa cómo actualizan ambas sesiones **tras cada bolilla**, sin refrescar ni reiniciar.
- Después de la quinta bolilla: ambos tienen cinco aciertos, pero sin resultado definitivo.
- Después de la sexta bolilla, cambia el estado a `finalizado`.
- Cierra y abre de nuevo la app para verificar la persistencia de jugadas, bolillas y resultados.
- Graba video corto mostrando publicación individual y actualización simultánea.

**Importante:** la consola de Firebase usa privilegios de administrador y no aplica las reglas de seguridad del cliente. Las reglas impiden que un participante modifique el sorteo.

## Estructura
- `FirebaseRepository.kt`: Auth, transacción para registrar jugada y listeners con `callbackFlow` + `awaitClose`.
- `LotteryViewModel.kt`: estado y ciclo de vida de los listeners.
- `LotteryModels.kt`: validación, aciertos y mensajes calculados.
- `MainActivity.kt`: navegación simple, selección y seguimiento Compose.
- `firestore.rules`: acceso autenticado y jugada privada, sin escrituras de sorteo desde clientes.
- `LotteryLogicTest.kt`: pruebas unitarias de jugada ganadora/perdedora y cierre.

## Limitaciones y verificación
- **No puede ejecutarse sin configurar tu propio Firebase.** No se incluyen credenciales, proyecto Firebase ni video de demostración.
- No se ha ejecutado compilación Android/Gradle en el entorno de generación. Importa el proyecto, sincroniza y valida en Android Studio.
- Implementa un solo sorteo de prueba identificado por `DRAW_ID`; está diseñado para la PC01, no para una lotería de producción.
- El estado del sorteo se actualiza en vivo incluso desde Inicio; la escucha de bolillas se activa solo al entrar a Seguimiento.
- Si el docente requiere un nuevo sorteo, cambia `DRAW_ID` y crea el documento correspondiente.

## Publicar en GitHub
1. Crea un repositorio **privado** de ser posible, con nombre que cumpla el enunciado.
2. Desde Android Studio, abre Terminal en la raíz:

```bash
git init
git add .
git commit -m "Implementar PC01 loteria Android Firebase"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
git push -u origin main
```

Si usas un proyecto nuevo, revisa los datos de autor en el README y **nunca publiques credenciales reales**.

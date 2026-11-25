# Changarrito

App Android de punto de venta e inventario para negocios pequeños (abarrotes, papelería, ferretería, farmacia o negocio independiente). Funciona sin internet: todo se guarda en el celular y la nube solo se usa para iniciar sesión y respaldar.

Proyecto escolar del CETI (Tecnólogo en Desarrollo de Software), hecho por Bruno Vizcaya y Marco.

## Qué hace

- **Inventario** con venta por pieza o a granel (KG, GR, M), código de barras, caducidad, costo y % de ganancia.
- **Vender** con carrito, escaneo, cobro con billetes sugeridos y cálculo del cambio.
- **Alertas** de stock bajo, agotado y por caducar, con pedido al proveedor por WhatsApp.
- **Caja**: cierre de caja (también "a ciegas" para trabajadores), gastos y mermas.
- **Reportes** de ventas, ganancias, hora pico, top 10, categorías y productos sin movimiento, más un PDF de estado de cuenta.
- **Cuentas y roles**: login con correo o Google, giro del negocio con catálogo sugerido, perfiles de dueño y trabajadores con clave de 4 dígitos y bitácora de cambios.
- **Respaldo** automático en la nube, ligado a la cuenta del dueño.

## Tecnología

| | |
| --- | --- |
| Lenguaje | Java 11 (sin Kotlin) |
| Android | mínimo API 24 (Android 7.0), compila con SDK 37 |
| Base local | Room / SQLite (`changarrito_db`, esquema v8) |
| Nube | Firebase Authentication + Cloud Firestore |
| Escáner | CameraX + ML Kit (100% en el celular) |
| Segundo plano | WorkManager |
| Gráficas y PDF | Canvas y `PdfDocument` de Android, sin librerías extra |

## Estructura

```
app/src/main/java/com/changarrito/
├── database/     Room: entidades, DAOs y migraciones
├── repository/   Acceso a datos por módulo (ventas, alertas, caja...)
├── negocio/      Giro, perfiles y turnos
├── auth/         Inicio de sesión con Firebase
├── respaldo/     Respaldo y restauración en Firestore
├── reportes/     Cálculos de reportes (clases puras)
├── pdf/          Estado de cuenta en PDF
├── scanner/      Cámara y lectura de códigos de barras
├── viewmodel/    Estado de cada pantalla
├── workers/      Tareas en segundo plano
├── utils/        Precio, cobro, fechas, WhatsApp, notificaciones
└── ui/           Activities, secciones, diálogos, listas, gráficas y guía
```

La app usa MVVM con repositorios: pantalla → ViewModel → Repository → DAO (Room).

## Cómo compilar

1. Clona el repo y ábrelo en Android Studio.
2. Deja que Gradle sincronice.
3. `app/google-services.json` ya apunta al proyecto de Firebase `changarrito-a4552`. Para usar otro proyecto, reemplázalo y publica `firestore.rules` (`firebase deploy --only firestore:rules`).
4. Ejecuta en un emulador o celular con Android 7.0 o superior.

## Pruebas

- Unitarias (JVM): `app/src/test/` → clic derecho → *Run Tests*.
- Instrumentadas (emulador): `app/src/androidTest/` → clic derecho → *Run Tests*.

## Ramas

- `feature/login`: la más reciente, con todo el trabajo.
- `feature/inventario`: hasta el Sprint 4.
- `develop`: rama de integración.
- `main`: versiones listas para entregar.

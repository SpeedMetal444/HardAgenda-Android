# HardAgenda Android

Version Android de [HardAgenda](https://github.com/SpeedMetal444/HardAgenda), un sistema de turnos para consultorios medicos.

## Requisitos

- **PC Windows** con PostgreSQL y Python 3.10+ instalados
- **Celular Android** en la misma red WiFi que la PC
- PostgreSQL con la base de datos de HardAgenda existente

## Instalacion

### 1. Servidor API (PC Windows)

El servidor HTTP actua como puente entre Android y PostgreSQL, ya que el driver JDBC no funciona en Android.

```bash
cd "HardAgenda Android"
python -m venv venv
venv\Scripts\activate
pip install psycopg2-binary
python server.py
```

El servidor queda disponible en `http://0.0.0.0:8080`.

### 2. App Android

Abrir el proyecto en Android Studio, compilar e instalar en el dispositivo.

### 3. Primer login

Al abrir la app por primera vez:

1. Ingresar la URL del servidor (ej: `http://192.168.0.82:8080`)
2. Ingresar el nombre de la base de datos
3. Ingresar usuario y contrasena de PostgreSQL
4. Marcar "Crear base de datos" y/o "Crear tabla" si es la primera vez
5. Tocar "Iniciar"

## Funcionalidades

- **Hoy**: turnos del dia actual con resaltado verde
- **Nuevo turno**: formulario de registro
- **Todos los turnos**: busqueda por DNI, nombre o apellido con menu contextual
- **Historial**: registro de cambios realizados
- **Acerca de**: informacion de la version

## Arquitectura

```
Android App  <-->  server.py (HTTP API)  <-->  PostgreSQL
```

- `server.py` es un servidor HTTP basico que expone una REST API
- La app envia las credenciales de PostgreSQL en cada request via headers HTTP
- No se almacenan contraseñas en el servidor

## Colores

Misma paleta que la version de escritorio:

- Verde oscuro: `#155724`
- Verde claro: `#d4edda`
- Texto gris: `#6b7280`

# NexCord - Aplicación de Chat en Tiempo Real 🚀

NexCord es una aplicación de mensajería instantánea tipo Discord/Telegram diseñada con una arquitectura cliente-servidor robusta. El proyecto combina la velocidad y reactividad de React en el frontend con el potente manejo de concurrencia de Java en el backend, todo conectado mediante WebSockets para una latencia mínima.

## ✨ Características Principales

- **Salas Múltiples:** División en canales temáticos (`#general`, `#dudas`, `#memes`).
- **Mensajes Privados (Susurros):** Comunícate de forma privada con cualquier usuario activo en la sala.
- **Persistencia de Datos:** Todo el historial de mensajes, usuarios y canales se guarda en una base de datos embebida (SQLite). ¡Nada se pierde al reiniciar!
- **Seguridad y Criptografía:** El sistema de inicio de sesión guarda y valida las contraseñas de manera segura utilizando el algoritmo de hasheo **BCrypt**.
- **Soporte Multimedia:** Envío de imágenes y soporte de avatares personalizados codificados en Base64.
- **Interacciones en Tiempo Real:** Indicador dinámico de "Usuario está escribiendo..." y sincronización instantánea de usuarios vivos en la sala.
- **Gestión de Mensajes:** Capacidad para eliminar tus propios mensajes enviados.

## 🛠️ Tecnologías Utilizadas

### Backend (Servidor)
- **Java 17**
- **Java-WebSocket:** Librería para la gestión bidireccional y persistente de conexiones WebSockets (`ws://`).
- **SQLite (JDBC):** Base de datos relacional ligera sin necesidad de levantar servicios externos.
- **jBCrypt:** Para el cifrado unidireccional de contraseñas.
- **Gson:** Para serialización y deserialización del protocolo JSON propio del proyecto.
- **Maven:** Gestión de dependencias.

### Frontend (Cliente)
- **React 19 + TypeScript**
- **Vite:** Herramienta moderna de empaquetado para una experiencia de desarrollo ultrarrápida.
- **CSS3 Vanilla:** Estilizado simulando la paleta "Dark Mode" característica de aplicaciones como Discord, usando variables CSS para mantenimiento limpio.

## 🚀 Cómo ejecutar el proyecto en local

El proyecto está dividido en dos partes independientes. Necesitarás tener instalados **Node.js** (para el frontend), **Java JDK 17+** y **Maven** (para el backend).

### 1. Levantar el Backend (Java)
1. Navega a la carpeta del backend:
   ```bash
   cd backend-java
   ```
2. Descarga las dependencias y compila:
   ```bash
   mvn clean install
   ```
3. Ejecuta el servidor (por defecto se iniciará en el puerto `8081`):
   ```bash
   mvn exec:java
   ```
   *(Alternativa: Puedes abrir la carpeta `backend-java` en tu IDE favorito como IntelliJ o VS Code y ejecutar la clase principal `com.chat.server.ChatServer`).*

### 2. Levantar el Frontend (React)
Abre una nueva terminal y ejecuta:
1. Navega a la carpeta del frontend:
   ```bash
   cd frontend-react
   ```
2. Instala las dependencias (solo la primera vez):
   ```bash
   npm install
   ```
3. Arranca el servidor de desarrollo:
   ```bash
   npm run dev
   ```
4. Abre tu navegador en la URL que indique la consola (generalmente `http://localhost:5173`).

## 📋 Arquitectura y Protocolo
La aplicación sigue un enfoque *Clean Architecture* donde el servidor actúa como un *Broker* que intercepta los mensajes JSON, los guarda en base de datos y hace *broadcast* a los usuarios pertinentes.

Para información más detallada sobre el contrato de datos JSON, la justificación de uso de colecciones concurrentes en Java y los diagramas de secuencia, consulta la documentación interna del proyecto en la carpeta `recursos`.

---
**Autor:** Manuel Ávila  
*Proyecto académico enfocado en Sockets, Concurrencia y Arquitectura de Sistemas Distribuidos.*

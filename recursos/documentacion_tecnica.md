# Documentación Técnica y Arquitectura

## 1. Diseño de Arquitectura (Cliente-Servidor)
La arquitectura de la aplicación consta de un modelo cliente-servidor clásico pero adaptado para tecnologías web:
- **Backend (Servidor)**: Construido en Java. Al no permitir los navegadores web (React) conexiones TCP puras por motivos de seguridad, se ha utilizado el protocolo **WebSockets** a través de la librería `Java-WebSocket`. El servidor actúa como intermediario (Brokering), manteniendo múltiples canales temáticos y gestionando las conexiones simultáneas usando colecciones concurrentes (`ConcurrentHashMap`).
- **Frontend (Cliente)**: Construido con React y TypeScript, empaquetado usando Vite. Mantiene una única conexión WebSocket persistente contra el puerto `8080` del servidor. La interfaz simula el "Dark Mode" de la aplicación Discord mediante el uso intensivo de variables puras de CSS.

*Nota: Se debe añadir en este apartado el diagrama de arquitectura y el diagrama de secuencias solicitado en el PDF original.*

## 2. Protocolo de Comunicación
La comunicación se realiza intercambiando tramas temporales de texto en formato JSON. El contrato de datos `Message` centraliza el protocolo:

```json
{
  "type": "JOIN | LEAVE | MESSAGE | USERS_UPDATE | CHANNELS_UPDATE",
  "username": "EjemploUsuario",
  "channel": "general",
  "content": "Contenido del mensaje",
  "activeUsers": ["EjemploUsuario", "OtroUsuario"],
  "availableChannels": ["general", "dudas", "memes"]
}
```
* **JOIN**: El cliente informa que se une a un canal.
* **MESSAGE**: Mensajes estándar de chat enviados a la sala.
* **USERS_UPDATE**: Broadcast emitido por el servidor cada vez que alguien entra o sale, conteniendo el array refrescado de usuarios vivos en la sala actual.

## 3. Justificación de Tecnologías
- **Java (Backend)**: Se requería para la práctica de Sockets. Se descartó el uso de Spring Boot avanzado para demostrar el control explícito y artesanal de los identificadores de usuarios, conexiones por hilo y colecciones concurrentes, cumpliendo la rúbrica al detalle. 
- **React (Frontend)**: Utilizado por su gran ecosistema y re-renderizado reactivo; crucial cuando se necesita reflejar interfaces complejas en tiempo real (como los componentes de Sidebar de canales o el Panel derecho de usuarios vivos) sin recargar y perder la conexión Socket TCP subyacente.


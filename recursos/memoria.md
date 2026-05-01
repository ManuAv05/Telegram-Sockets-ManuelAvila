# Memoria Breve

## 1. Dificultades Encontradas
1. **Conexión entre React y WebSockets**: La instrucción del PDF era usar Sockets, sin embargo los navegadores web bloquean Sockets puros "TCP". La principal dificultad técnica fue investigar y aislar la librería adecuada en Java (`org.java-websocket`) capaz de realizar el handshake HTTP exigido por la web y transformarlo a una conexión TCP duradera, en lugar de usar sockets `java.net`.
2. **Sincronización Multicliente**: Mantenimiento en tiempo real de los usuarios vivos por canal. Se utilizaron colecciones concurrentes (`ConcurrentHashMap`) en lugar del `HashMap` normal de Java para evitar excepciones `ConcurrentModificationException` mientras varios clientes emiten mensajes de estado simultáneamente.

## 2. Fuentes Utilizadas y Uso de IA
- Documentación oficial de MDN (Mozilla Developer Network) para el objeto `WebSocket` nativo en Javascript (React).
- Repositorio oficial de TooTallNate (`Java-WebSocket`) con ejemplos en Github.
- **Herramientas de IA utilizadas**: *(Modifica esta parte según cómo presentes la memoria al profesor)*.

## 3. Histórico de Equipo
- Se optó por una estructura de carpetas separada para desacoplar en base a Clean Architecture: Back / Front.
- Creación de ramas separadas (Front, Back) en el repositorio para no entorpecer el trabajo si este proyecto se escala.

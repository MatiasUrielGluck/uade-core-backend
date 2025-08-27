# Core Hub Backend

Backend de mensajería centralizada (**Message Hub**) construido con **Spring Boot 3**, **PostgreSQL**, **RabbitMQ** y **Docker**.  
El objetivo es abstraer la mensajería, estandarizar el formato de mensajes, aplicar validaciones, registrar historial y exponer una API para publicar mensajes hacia el broker.

---

## 🚀 Características actuales
- **Spring Boot 3 + Java 21**
- **API REST** con endpoint `/publish`
- **Persistencia en PostgreSQL** (historial de mensajes + payload en JSONB)
- **Broker RabbitMQ** con adapter desacoplado (futuro soporte para Kafka)
- **Idempotencia** por `messageId` único
- **Validación de canales** mediante `ChannelRegistry` (YAML configurable)
- **Actuator healthchecks** para DB y RabbitMQ
- **Configuración externa** vía `.env`

---

## ⚙️ Requisitos
- **Java 21**
- **Maven 3.9+**
- **Docker & Docker Compose**
- Cliente para Postgres (psql, DBeaver, etc.)

---

## 🐳 Levantar en local
1. Crear `.env` (ejemplo):
   ```env
   POSTGRES_URL=jdbc:postgresql://localhost:5432/x
   POSTGRES_USER=x
   POSTGRES_PASSWORD=x

   RABBIT_HOST=localhost
   RABBIT_PORT=5672
   RABBIT_USER=x
   RABBIT_PASSWORD=x
   ```
2. Levantar dependencias:
   ```bash
   docker compose up -d
   ```
3. Correr la app:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
4. Healthcheck:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## 📡 Probar `/publish`
```bash
curl -X POST http://localhost:8080/publish \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: demo-123" \
  -d '{
        "messageId": "msg-001",
        "timestamp": "2025-08-27T21:50:00Z",
        "source": "demo-cli",
        "destination": {"channel":"payments.order.created","eventName":"orderCreated"},
        "metadata": {"key":"val"},
        "payload": {"orderId":123,"amount": 99.99}
      }'
```
- Respuesta esperada: `202 Accepted`
- El mensaje se persiste en Postgres (`message_log`, `payload_store`)
- RabbitMQ lo encola en el exchange configurado (si existe)

---

## 🔒 Seguridad
- Credenciales de DB y broker se leen de variables de entorno (`.env`).
- En producción deben almacenarse en **AWS Secrets Manager** o **Parameter Store** (no en `.env` ni código).
- Los health endpoints deben exponer solo `status` en ambientes productivos.

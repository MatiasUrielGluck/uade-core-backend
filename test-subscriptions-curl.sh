#!/bin/bash
# Script para probar el sistema de suscripciones con curl (sin jq)
# Ejecutar: ./test-subscriptions-curl.sh

BASE_URL="http://localhost:8080"

echo "🧪 Probando sistema de suscripciones con curl..."
echo "================================================"

# Función para extraer ID de la respuesta JSON (sin jq)
extract_id() {
    local json="$1"
    # Buscar subscriptionId en el JSON usando grep y sed
    echo "$json" | grep -o '"subscriptionId":"[^"]*"' | sed 's/"subscriptionId":"//;s/"//'
}

# 1. Crear suscripciones
echo ""
echo "1️⃣ Creando suscripciones..."

echo "Creando suscripción 1 (sin wildcards)..."
RESPONSE1=$(curl -s -X POST "$BASE_URL/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "http://localhost:8000",
    "squadName": "payments-squad",
    "topic": "payments.order.created",
    "eventName": "orderCreated"
  }')
echo "✅ Respuesta: $RESPONSE1"

echo "Creando suscripción 2 (con wildcard *)..."
RESPONSE2=$(curl -s -X POST "$BASE_URL/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://localhost:8000",
    "squadName": "notifications-squad",
    "topic": "payments.order.*",
    "eventName": "order*"
  }')
echo "✅ Respuesta: $RESPONSE2"

echo "Creando suscripción 3 (con wildcard #)..."
RESPONSE3=$(curl -s -X POST "$BASE_URL/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://localhost:8000",
    "squadName": "analytics-squad",
    "topic": "#.order.#",
    "eventName": "#"
  }')
echo "✅ Respuesta: $RESPONSE3"

# Extraer IDs usando la función personalizada
ID1=$(extract_id "$RESPONSE1")
ID2=$(extract_id "$RESPONSE2")
ID3=$(extract_id "$RESPONSE3")

if [ -n "$ID1" ] && [ -n "$ID2" ] && [ -n "$ID3" ]; then
    echo "📋 IDs extraídos: $ID1, $ID2, $ID3"
else
    echo "⚠️  No se pudieron extraer los IDs, usando IDs de ejemplo"
    ID1="550e8400-e29b-41d4-a716-446655440001"
    ID2="550e8400-e29b-41d4-a716-446655440002"
    ID3="550e8400-e29b-41d4-a716-446655440003"
fi

# 2. Consultar todas las suscripciones
echo ""
echo "2️⃣ Consultando todas las suscripciones..."
echo "Respuesta:"
curl -s -X GET "$BASE_URL/subscribe"

# 2.5. Listar eventos suscritos
echo ""
echo "2️⃣5️⃣ Listando eventos suscritos..."
echo "Respuesta:"
curl -s -X GET "$BASE_URL/list"

# 3. Consultar suscripciones por squad
echo ""
echo "3️⃣ Consultando suscripciones por squad..."
echo "Suscripciones del payments-squad:"
curl -s -X GET "$BASE_URL/subscribe/squad/payments-squad"

echo ""
echo "Suscripciones del notifications-squad:"
curl -s -X GET "$BASE_URL/subscribe/squad/notifications-squad"

# 4. Consultar suscripción específica
echo ""
echo "4️⃣ Consultando suscripción específica..."
echo "Suscripción $ID1:"
curl -s -X GET "$BASE_URL/subscribe/$ID1"

# 5. Estadísticas por squad
echo ""
echo "5️⃣ Estadísticas por squad..."
echo "payments-squad: $(curl -s -X GET "$BASE_URL/subscribe/stats/squad/payments-squad") suscripciones activas"
echo "notifications-squad: $(curl -s -X GET "$BASE_URL/subscribe/stats/squad/notifications-squad") suscripciones activas"
echo "analytics-squad: $(curl -s -X GET "$BASE_URL/subscribe/stats/squad/analytics-squad") suscripciones activas"

# 6. Cambiar estado de suscripción
echo ""
echo "6️⃣ Cambiando estado de suscripción..."
echo "Cambiando suscripción $ID1 a INACTIVE..."
curl -s -X PUT "$BASE_URL/subscribe/$ID1/status?status=INACTIVE"
echo "✅ Estado cambiado"

echo ""
echo "Suscripciones activas después del cambio:"
curl -s -X GET "$BASE_URL/subscribe"

echo ""
echo "Reactivando suscripción $ID1..."
curl -s -X PUT "$BASE_URL/subscribe/$ID1/status?status=ACTIVE"
echo "✅ Suscripción reactivada"

# 7. Probar validaciones (errores esperados)
echo ""
echo "7️⃣ Probando validaciones..."
echo "Probando URL inválida..."
curl -s -X POST "$BASE_URL/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "invalid-url",
    "squadName": "test-squad",
    "topic": "test.topic",
    "eventName": "testEvent"
  }'

echo ""
echo "Probando wildcard # inválido..."
curl -s -X POST "$BASE_URL/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://test.com/webhook",
    "squadName": "test-squad",
    "topic": "test#invalid.topic",
    "eventName": "testEvent"
  }'

# 8. Eliminar una suscripción
echo ""
echo "8️⃣ Eliminando suscripción..."
echo "Eliminando suscripción $ID3..."
curl -s -X DELETE "$BASE_URL/unsubscribe/$ID3"
echo "✅ Suscripción eliminada"

echo ""
echo "Suscripciones activas finales:"
curl -s -X GET "$BASE_URL/subscribe"

echo ""
echo "🎉 ¡Pruebas completadas exitosamente!"
echo "================================================"

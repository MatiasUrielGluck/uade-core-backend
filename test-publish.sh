#!/bin/bash

# 🧪 Script de Pruebas - Sistema de Publicación RabbitMQ
# Ejecutar: chmod +x test-publish.sh && ./test-publish.sh
# Uso: ./test-publish.sh [local|cloud]

set -e  # Salir si hay errores

# Configuración de endpoints
LOCAL_URL="http://localhost:8080"
CLOUD_URL="http://arreglaya-core-backend.us-east-1.elasticbeanstalk.com"

# Determinar endpoint a usar
if [ "$1" = "cloud" ]; then
    BASE_URL="$CLOUD_URL"
    ENV_NAME="Cloud Production"
else
    BASE_URL="$LOCAL_URL"
    ENV_NAME="Local Development"
fi

echo "🚀 Iniciando pruebas del sistema de publicación RabbitMQ..."
echo "🌐 Entorno: $ENV_NAME"
echo "🔗 URL Base: $BASE_URL"
echo "=================================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir con colores
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para hacer requests HTTP
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    print_status "$description"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$url")
    fi
    
    # Separar respuesta y código HTTP
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        print_success "HTTP $http_code - $description"
        if [ -n "$body" ] && [ "$body" != "null" ]; then
            echo "$body" | jq . 2>/dev/null || echo "$body"
        fi
    else
        print_error "HTTP $http_code - $description"
        echo "$body"
        return 1
    fi
    echo ""
}

# Verificar que la aplicación esté corriendo
print_status "Verificando que la aplicación esté corriendo..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    print_error "La aplicación no está corriendo en $BASE_URL"
    exit 1
fi
print_success "Aplicación corriendo ✓"

echo ""
echo "🔧 1. VERIFICANDO INFRAESTRUCTURA"
echo "================================="

make_request "GET" "$BASE_URL/channels/debug/infrastructure" "" "Verificando estado de infraestructura"

echo ""
echo "📤 2. PRUEBAS DE PUBLICACIÓN"
echo "============================"

# Test 1: Canal Predefinido
print_status "Test 1: Publicando a canal predefinido (payments.order.created)"
test1_data='{
  "messageId": "msg-001-'$(date +%s)'",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
  "source": "test-suite",
  "destination": {
    "channel": "payments.order.created",
    "eventName": "orderCreated"
  },
  "payload": {
    "orderId": 12345,
    "amount": 99.99,
    "currency": "USD",
    "customerId": "cust-001"
  }
}'

make_request "POST" "$BASE_URL/publish" "$test1_data" "Publicando mensaje a payments.order.created"

# Test 2: Canal Dinámico
print_status "Test 2: Publicando a canal dinámico (payments.order.canceled)"
test2_data='{
  "messageId": "msg-002-'$(date +%s)'",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
  "source": "test-suite",
  "destination": {
    "channel": "payments.order.canceled",
    "eventName": "orderCanceled"
  },
  "payload": {
    "orderId": 12345,
    "reason": "customer_request",
    "refundAmount": 99.99
  }
}'

make_request "POST" "$BASE_URL/publish" "$test2_data" "Publicando mensaje a payments.order.canceled"

# Test 3: Canal de Billing
print_status "Test 3: Publicando a canal de billing (billing.invoice.issued)"
test3_data='{
  "messageId": "msg-003-'$(date +%s)'",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
  "source": "billing-service",
  "destination": {
    "channel": "billing.invoice.issued",
    "eventName": "invoiceIssued"
  },
  "payload": {
    "invoiceId": "inv-001",
    "amount": 299.99,
    "dueDate": "2025-02-27",
    "customerId": "cust-001"
  }
}'

make_request "POST" "$BASE_URL/publish" "$test3_data" "Publicando mensaje a billing.invoice.issued"

echo ""
echo "🔍 3. VERIFICANDO ESTADO DE MENSAJES"
echo "===================================="

# Extraer messageId del primer test para verificar
message_id=$(echo "$test1_data" | jq -r '.messageId')
make_request "GET" "$BASE_URL/publish/status/$message_id" "" "Verificando estado del mensaje $message_id"

echo ""
echo "❌ 4. PRUEBAS DE ERROR"
echo "====================="

# Test de Idempotencia
print_status "Test de Idempotencia: Publicando mismo mensaje dos veces"
duplicate_data='{
  "messageId": "msg-duplicate-'$(date +%s)'",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
  "source": "test-suite",
  "destination": {
    "channel": "payments.order.created",
    "eventName": "orderCreated"
  },
  "payload": {
    "orderId": 99999,
    "amount": 50.00
  }
}'

# Primera publicación
make_request "POST" "$BASE_URL/publish" "$duplicate_data" "Primera publicación (debería funcionar)"

# Segunda publicación (debería ser ignorada por idempotencia)
make_request "POST" "$BASE_URL/publish" "$duplicate_data" "Segunda publicación (debería ser ignorada)"

echo ""
echo "🎯 5. RESUMEN Y VERIFICACIONES"
echo "==============================="

print_status "Verificaciones finales:"
echo ""

# Verificar infraestructura final
make_request "GET" "$BASE_URL/channels/debug/infrastructure" "" "Estado final de infraestructura"

echo ""
print_success "✅ Pruebas completadas!"
echo ""
print_warning "📋 PRÓXIMOS PASOS MANUALES:"
if [ "$1" = "cloud" ]; then
    echo "1. Abrir Swagger UI: $BASE_URL/swagger-ui.html"
    echo "2. Verificar que los endpoints funcionen en la nube"
    echo "3. Revisar logs de la aplicación en AWS"
else
    echo "1. Abrir RabbitMQ GUI: http://localhost:15672 (corehub/corehub)"
    echo "2. Verificar en 'Exchanges': corehub.x.payments, corehub.x.billing"
    echo "3. Verificar en 'Queues': payments.order.created, payments.order.canceled, billing.invoice.issued"
    echo "4. Verificar en 'Bindings': que existan los bindings correctos"
    echo "5. Verificar que los mensajes aparezcan en las colas"
fi
echo ""
print_warning "🚨 PROBLEMA CONOCIDO:"
echo "Si los mensajes no aparecen en RabbitMQ GUI, revisar logs de la aplicación"
echo "El problema puede estar en la lógica de idempotencia (PublishService.java líneas 39-42)"
echo ""
print_status "Revisar logs de la aplicación para ver el flujo completo de publicación"

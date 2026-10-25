#!/bin/bash
echo "Starting DeepBank Microservices..."
mkdir -p logs

nohup java -jar account-service/target/account-service-1.0.0-SNAPSHOT.jar > logs/account.log 2>&1 &
echo "Account Service starting on :8081 (PID: $!)"

nohup java -jar transfer-service/target/transfer-service-1.0.0-SNAPSHOT.jar > logs/transfer.log 2>&1 &
echo "Transfer Service starting on :8082 (PID: $!)"

nohup java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar > logs/notification.log 2>&1 &
echo "Notification Service starting on :8083 (PID: $!)"

nohup java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar > logs/gateway.log 2>&1 &
echo "API Gateway starting on :8080 (PID: $!)"

echo ""
echo "All 4 microservices launched in the background!"
echo "View live logs with: tail -f logs/account.log logs/transfer.log logs/gateway.log"

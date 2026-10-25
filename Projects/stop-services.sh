#!/bin/bash
echo "Stopping all DeepBank microservices on ports 8080, 8081, 8082, 8083..."
fuser -k 8080/tcp 8081/tcp 8082/tcp 8083/tcp 2>/dev/null || true
echo "All microservices stopped."

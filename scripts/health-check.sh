#!/bin/bash

# ============================================
# GPS Tracker Health Check Script
# ============================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8182"
TIMEOUT=5
VERBOSE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -u|--url)
            BASE_URL="$2"
            shift
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [-v|--verbose] [-u|--url <base_url>]"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}=== GPS Tracker Health Check ===${NC}"
echo -e "Checking: $BASE_URL"
echo -e "Time: $(date)"
echo ""

# Function to check endpoint
check_endpoint() {
    local endpoint=$1
    local description=$2
    local expected_status=${3:-200}
    
    if $VERBOSE; then
        echo -e "${BLUE}Checking $description...${NC}"
    fi
    
    response=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT "$BASE_URL$endpoint")
    
    if [ "$response" = "$expected_status" ]; then
        echo -e "${GREEN}✓ $description: OK${NC}"
        return 0
    else
        echo -e "${RED}✗ $description: FAILED (HTTP $response)${NC}"
        return 1
    fi
}

# Function to check with response body
check_endpoint_body() {
    local endpoint=$1
    local description=$2
    
    if $VERBOSE; then
        echo -e "${BLUE}Checking $description...${NC}"
    fi
    
    response=$(curl -s --max-time $TIMEOUT "$BASE_URL$endpoint")
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT "$BASE_URL$endpoint")
    
    if [ "$http_code" = "200" ]; then
        if echo "$response" | grep -q "UP"; then
            echo -e "${GREEN}✓ $description: UP${NC}"
            if $VERBOSE; then
                echo "  Response: $response"
            fi
            return 0
        else
            echo -e "${YELLOW}⚠ $description: Response received but status not UP${NC}"
            if $VERBOSE; then
                echo "  Response: $response"
            fi
            return 1
        fi
    else
        echo -e "${RED}✗ $description: FAILED (HTTP $http_code)${NC}"
        return 1
    fi
}

# Function to check metrics
check_metrics() {
    local endpoint="/actuator/metrics/jvm.memory.used"
    local description="Memory Metrics"
    
    if $VERBOSE; then
        echo -e "${BLUE}Checking $description...${NC}"
    fi
    
    response=$(curl -s --max-time $TIMEOUT "$BASE_URL$endpoint")
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT "$BASE_URL$endpoint")
    
    if [ "$http_code" = "200" ]; then
        memory_used=$(echo "$response" | grep -o '"value":[0-9.]*' | cut -d: -f2)
        if [ ! -z "$memory_used" ]; then
            memory_mb=$(echo "scale=2; $memory_used / 1048576" | bc 2>/dev/null || echo "N/A")
            echo -e "${GREEN}✓ $description: ${memory_mb}MB used${NC}"
            return 0
        fi
    fi
    
    echo -e "${YELLOW}⚠ $description: Unable to retrieve${NC}"
    return 1
}

# Function to check database
check_database() {
    local endpoint="/actuator/health/db"
    local description="Database Connection"
    
    if $VERBOSE; then
        echo -e "${BLUE}Checking $description...${NC}"
    fi
    
    response=$(curl -s --max-time $TIMEOUT "$BASE_URL$endpoint")
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT "$BASE_URL$endpoint")
    
    if [ "$http_code" = "200" ]; then
        if echo "$response" | grep -q '"status":"UP"'; then
            echo -e "${GREEN}✓ $description: Connected${NC}"
            return 0
        else
            echo -e "${RED}✗ $description: Not connected${NC}"
            if $VERBOSE; then
                echo "  Response: $response"
            fi
            return 1
        fi
    else
        echo -e "${RED}✗ $description: FAILED (HTTP $http_code)${NC}"
        return 1
    fi
}

# Run checks
total_checks=0
failed_checks=0

# Basic connectivity
check_endpoint "/" "Application Home" && ((total_checks++)) || ((failed_checks++, total_checks++))

# Health endpoints
check_endpoint_body "/actuator/health" "Overall Health" && ((total_checks++)) || ((failed_checks++, total_checks++))
check_endpoint "/actuator/health/liveness" "Liveness Probe" && ((total_checks++)) || ((failed_checks++, total_checks++))
check_endpoint "/actuator/health/readiness" "Readiness Probe" && ((total_checks++)) || ((failed_checks++, total_checks++))

# Component health
check_database && ((total_checks++)) || ((failed_checks++, total_checks++))

# Metrics
check_metrics && ((total_checks++)) || ((failed_checks++, total_checks++))

# API endpoints
check_endpoint "/api/vehicles" "Vehicles API" && ((total_checks++)) || ((failed_checks++, total_checks++))
check_endpoint "/api/monitoring/status" "Monitoring API" && ((total_checks++)) || ((failed_checks++, total_checks++))

# WebSocket endpoint (just check if it's reachable)
check_endpoint "/ws-location/info" "WebSocket Endpoint" && ((total_checks++)) || ((failed_checks++, total_checks++))

# Summary
echo ""
echo -e "${BLUE}=== Summary ===${NC}"
echo -e "Total checks: $total_checks"
echo -e "Passed: $((total_checks - failed_checks))"
echo -e "Failed: $failed_checks"

if [ $failed_checks -eq 0 ]; then
    echo -e "${GREEN}Overall Status: HEALTHY ✓${NC}"
    exit 0
else
    echo -e "${RED}Overall Status: UNHEALTHY ✗${NC}"
    exit 1
fi
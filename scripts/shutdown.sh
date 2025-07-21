#!/bin/bash

# ============================================
# GPS Tracker Application Shutdown Script
# ============================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Application settings
APP_NAME="ThintureGpsTracker"
PID_FILE="app.pid"
LOG_DIR="logs"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo -e "${YELLOW}PID file not found. Application may not be running.${NC}"
    
    # Try to find process by name
    PID=$(ps aux | grep -E "[j]ava.*$APP_NAME" | awk '{print $2}' | head -1)
    
    if [ -z "$PID" ]; then
        echo -e "${RED}No running instance of $APP_NAME found.${NC}"
        exit 1
    else
        echo -e "${GREEN}Found application running with PID: $PID${NC}"
    fi
else
    PID=$(cat $PID_FILE)
fi

# Check if process exists
if ! ps -p $PID > /dev/null 2>&1; then
    echo -e "${YELLOW}Process with PID $PID is not running.${NC}"
    rm -f $PID_FILE
    exit 0
fi

echo -e "${GREEN}Stopping $APP_NAME (PID: $PID)...${NC}"

# Send SIGTERM for graceful shutdown
kill -15 $PID

# Wait for graceful shutdown
echo -n "Waiting for graceful shutdown"
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo -e "\n${GREEN}Application stopped gracefully.${NC}"
        rm -f $PID_FILE
        exit 0
    fi
    echo -n "."
    sleep 1
done

# Force kill if still running
echo -e "\n${YELLOW}Application did not stop gracefully. Force killing...${NC}"
kill -9 $PID

# Wait a moment and check
sleep 2
if ps -p $PID > /dev/null 2>&1; then
    echo -e "${RED}Failed to stop application!${NC}"
    exit 1
else
    echo -e "${GREEN}Application force stopped.${NC}"
    rm -f $PID_FILE
    exit 0
fi
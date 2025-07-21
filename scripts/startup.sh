#!/bin/bash

# ============================================
# GPS Tracker Application Startup Script - RAW TIMESTAMP STORAGE
# Store exactly what device sends - no timezone conversion
# ============================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Application settings
APP_NAME="ThintureGpsTracker"
JAR_FILE="ThintureGpsTracker.jar"
PID_FILE="app.pid"
LOG_DIR="logs"
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}

# ============================================
# FUNCTIONS
# ============================================

print_banner() {
    echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}🚀 THINTURE GPS TRACKER - RAW TIMESTAMP STORAGE${NC}"
    echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}📅 Storage Mode: RAW (No timezone conversion)${NC}"
    echo -e "${BLUE}🌍 Dubai device → Stores Dubai time AS-IS${NC}"
    echo -e "${BLUE}🌍 India device → Stores India time AS-IS${NC}"
    echo -e "${BLUE}🌍 Kenya device → Stores Kenya time AS-IS${NC}"
    echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
}

check_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${YELLOW}⚠️ Application is already running with PID: $PID${NC}"
            echo -e "${YELLOW}   Use './stop.sh' to stop the application first${NC}"
            exit 1
        else
            echo -e "${YELLOW}⚠️ Removing stale PID file${NC}"
            rm -f $PID_FILE
        fi
    fi
}

setup_environment() {
    # Create log directory if not exists
    mkdir -p $LOG_DIR
    
    # Environment selection
    PROFILE=${1:-prod}
    echo -e "${GREEN}📋 Profile: $PROFILE${NC}"
    
    # Set environment-specific settings
    if [ "$PROFILE" = "dev" ]; then
        echo -e "${BLUE}🔧 Development mode: Enhanced logging enabled${NC}"
    else
        echo -e "${BLUE}🔧 Production mode: Optimized for performance${NC}"
    fi
}

configure_jvm() {
    echo -e "${BLUE}⚙️ Configuring JVM for RAW timestamp storage...${NC}"
    
    # 🔧 FIXED: JVM Options optimized for RAW timestamp storage
    JAVA_OPTS="-server"
    
    # Memory settings
    JAVA_OPTS="$JAVA_OPTS -Xms2g -Xmx4g"
    JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
    JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=200"
    JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
    JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=$LOG_DIR/"
    
    # System settings
    JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
    JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
    JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
    
    # ✅ CRITICAL: Timezone settings for RAW storage consistency
    JAVA_OPTS="$JAVA_OPTS -Duser.timezone=UTC"
    JAVA_OPTS="$JAVA_OPTS -Djava.time.zone=UTC"
    
    # ✅ RAW timestamp storage indicators
    JAVA_OPTS="$JAVA_OPTS -Dapp.timestamp.storage=RAW"
    JAVA_OPTS="$JAVA_OPTS -Dapp.timezone.conversion=DISABLED"
    
    # Performance monitoring
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false"
    
    echo -e "${GREEN}✅ JVM configured for RAW timestamp storage${NC}"
    echo -e "${BLUE}   Timezone: UTC (consistency only, no conversion)${NC}"
    echo -e "${BLUE}   Memory: 2GB-4GB heap with G1GC${NC}"
    echo -e "${BLUE}   Storage: RAW mode (exact device timestamps)${NC}"
}

configure_spring() {
    # Spring Boot Options
    SPRING_OPTS="--spring.profiles.active=$PROFILE"
    SPRING_OPTS="$SPRING_OPTS --logging.file.name=$LOG_DIR/gps-tracker-$PROFILE-raw.log"
    
    # Add RAW timestamp specific properties
    SPRING_OPTS="$SPRING_OPTS --app.timestamp.storage.mode=RAW"
    SPRING_OPTS="$SPRING_OPTS --app.timezone.verification.enabled=true"
    
    echo -e "${GREEN}✅ Spring Boot configured for RAW timestamp storage${NC}"
}

check_prerequisites() {
    echo -e "${BLUE}🔍 Checking prerequisites...${NC}"
    
    # Check if JAR exists
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}❌ JAR file not found: $JAR_FILE${NC}"
        echo -e "${YELLOW}💡 Please build the application first:${NC}"
        echo -e "${YELLOW}   mvn clean package${NC}"
        echo -e "${YELLOW}   or${NC}"
        echo -e "${YELLOW}   ./mvnw clean package${NC}"
        exit 1
    fi
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java not found. Please install Java 17+${NC}"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}✅ Java version: $JAVA_VERSION${NC}"
    echo -e "${GREEN}✅ JAR file found: $JAR_FILE${NC}"
}

start_application() {
    echo -e "${GREEN}🚀 Starting $APP_NAME with RAW timestamp storage...${NC}"
    echo -e "${BLUE}📊 Startup logs: $LOG_DIR/startup-$PROFILE-raw.log${NC}"
    
    # Start application in background
    nohup $JAVA_HOME/bin/java $JAVA_OPTS -jar $JAR_FILE $SPRING_OPTS > $LOG_DIR/startup-$PROFILE-raw.log 2>&1 &
    
    # Get PID
    PID=$!
    echo $PID > $PID_FILE
    
    echo -e "${GREEN}📋 Application PID: $PID${NC}"
}

wait_for_startup() {
    echo -e "${BLUE}⏳ Waiting for application to start...${NC}"
    echo -n "   "
    
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:8182/actuator/health > /dev/null 2>&1; then
            echo -e "\n${GREEN}✅ Application started successfully!${NC}"
            
            # Get health status
            HEALTH_STATUS=$(curl -s http://localhost:8182/actuator/health 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            echo -e "${GREEN}🏥 Health status: $HEALTH_STATUS${NC}"
            
            # Show application info
            echo -e "${CYAN}════════════════════════════════════════${NC}"
            echo -e "${GREEN}🎉 GPS Tracker is now running!${NC}"
            echo -e "${BLUE}📡 URL: http://localhost:8182${NC}"
            echo -e "${BLUE}🏥 Health: http://localhost:8182/actuator/health${NC}"
            echo -e "${BLUE}📊 Metrics: http://localhost:8182/actuator/metrics${NC}"
            echo -e "${BLUE}📋 PID: $PID${NC}"
            echo -e "${BLUE}📁 Logs: $LOG_DIR/gps-tracker-$PROFILE-raw.log${NC}"
            echo -e "${CYAN}════════════════════════════════════════${NC}"
            echo -e "${YELLOW}💡 Monitor logs with: tail -f $LOG_DIR/gps-tracker-$PROFILE-raw.log${NC}"
            echo -e "${YELLOW}💡 Stop application with: ./stop.sh${NC}"
            echo -e "${CYAN}════════════════════════════════════════${NC}"
            
            # Check for RAW timestamp verification in logs
            sleep 2
            if grep -q "RAW TIMESTAMP STORAGE" $LOG_DIR/startup-$PROFILE-raw.log 2>/dev/null; then
                echo -e "${GREEN}✅ RAW timestamp storage verification found in logs${NC}"
            else
                echo -e "${YELLOW}⚠️ Check logs for RAW timestamp verification results${NC}"
            fi
            
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo -e "\n${RED}❌ Application failed to start within $((max_attempts * 2)) seconds${NC}"
    echo -e "${RED}🔍 Check the startup logs:${NC}"
    echo -e "${RED}   tail -f $LOG_DIR/startup-$PROFILE-raw.log${NC}"
    echo -e "${RED}   cat $LOG_DIR/startup-$PROFILE-raw.log${NC}"
    
    # Show last few lines of log
    if [ -f "$LOG_DIR/startup-$PROFILE-raw.log" ]; then
        echo -e "${YELLOW}📄 Last 10 lines of startup log:${NC}"
        tail -10 "$LOG_DIR/startup-$PROFILE-raw.log"
    fi
    
    exit 1
}

show_usage() {
    echo -e "${CYAN}Usage: $0 [PROFILE]${NC}"
    echo -e "${BLUE}PROFILE:${NC}"
    echo -e "${BLUE}  dev  - Development mode (default)${NC}"
    echo -e "${BLUE}  prod - Production mode${NC}"
    echo -e "${BLUE}${NC}"
    echo -e "${BLUE}Examples:${NC}"
    echo -e "${BLUE}  $0        # Start with dev profile${NC}"
    echo -e "${BLUE}  $0 dev    # Start with dev profile${NC}"
    echo -e "${BLUE}  $0 prod   # Start with prod profile${NC}"
}

# ============================================
# MAIN EXECUTION
# ============================================

# Check for help
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_usage
    exit 0
fi

# Main execution flow
print_banner
check_running
setup_environment $1
configure_jvm
configure_spring
check_prerequisites
start_application
wait_for_startup

echo -e "${GREEN}🎯 GPS Tracker with RAW timestamp storage is ready!${NC}"
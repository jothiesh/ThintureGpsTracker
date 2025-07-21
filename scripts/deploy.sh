#!/bin/bash

# ============================================
# GPS Tracker Deployment Script
# ============================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="ThintureGpsTracker"
JAR_FILE="ThintureGpsTracker.jar"
DEPLOY_USER="ubuntu"
BACKUP_DIR="/opt/backups"
APP_DIR="/opt/gps-tracker"
SERVICE_NAME="gps-tracker"

# Parse arguments
ENVIRONMENT=${1:-prod}
DEPLOY_HOST=${2:-}
SKIP_TESTS=${3:-false}

# Function to print colored output
print_status() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✓ $1${NC}"
}

print_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠ $1${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed"
        exit 1
    fi
    
    # Check Git
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed"
        exit 1
    fi
    
    print_success "All prerequisites met"
}

# Function to run tests
run_tests() {
    if [ "$SKIP_TESTS" = "true" ]; then
        print_warning "Skipping tests as requested"
        return 0
    fi
    
    print_status "Running tests..."
    if mvn test; then
        print_success "All tests passed"
    else
        print_error "Tests failed. Aborting deployment."
        exit 1
    fi
}

# Function to build application
build_application() {
    print_status "Building application for $ENVIRONMENT environment..."
    
    # Clean previous builds
    mvn clean
    
    # Build with profile
    if mvn package -P${ENVIRONMENT} -DskipTests=${SKIP_TESTS}; then
        print_success "Build successful"
        
        # Check if JAR exists
        if [ ! -f "target/$JAR_FILE" ]; then
            print_error "JAR file not found after build"
            exit 1
        fi
    else
        print_error "Build failed"
        exit 1
    fi
}

# Function for local deployment
deploy_local() {
    print_status "Deploying locally..."
    
    # Stop existing service
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_status "Stopping existing service..."
        sudo systemctl stop $SERVICE_NAME
    fi
    
    # Backup current version
    if [ -f "$APP_DIR/$JAR_FILE" ]; then
        print_status "Backing up current version..."
        sudo mkdir -p $BACKUP_DIR
        sudo cp "$APP_DIR/$JAR_FILE" "$BACKUP_DIR/$JAR_FILE.$(date +%Y%m%d_%H%M%S)"
    fi
    
    # Create app directory
    sudo mkdir -p $APP_DIR
    
    # Copy new JAR
    print_status "Copying new JAR..."
    sudo cp "target/$JAR_FILE" "$APP_DIR/"
    
    # Copy scripts
    print_status "Copying scripts..."
    sudo cp scripts/*.sh "$APP_DIR/"
    sudo chmod +x "$APP_DIR"/*.sh
    
    # Copy configuration
    print_status "Copying configuration..."
    sudo cp src/main/resources/application-${ENVIRONMENT}.properties "$APP_DIR/"
    
    # Start service
    print_status "Starting service..."
    sudo systemctl start $SERVICE_NAME
    
    # Wait for service to be ready
    sleep 5
    
    # Check if service is running
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_success "Service started successfully"
    else
        print_error "Service failed to start"
        sudo journalctl -u $SERVICE_NAME -n 50
        exit 1
    fi
}

# Function for remote deployment
deploy_remote() {
    if [ -z "$DEPLOY_HOST" ]; then
        print_error "Remote host not specified"
        echo "Usage: $0 <environment> <host> [skip-tests]"
        exit 1
    fi
    
    print_status "Deploying to remote host: $DEPLOY_HOST"
    
    # Create deployment package
    print_status "Creating deployment package..."
    mkdir -p deploy-package
    cp target/$JAR_FILE deploy-package/
    cp scripts/*.sh deploy-package/
    cp src/main/resources/application-${ENVIRONMENT}.properties deploy-package/
    
    # Create deployment script
    cat > deploy-package/remote-deploy.sh << 'EOF'
#!/bin/bash
set -e

SERVICE_NAME="gps-tracker"
APP_DIR="/opt/gps-tracker"
BACKUP_DIR="/opt/backups"

# Stop service
if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Stopping service..."
    sudo systemctl stop $SERVICE_NAME
fi

# Backup
if [ -f "$APP_DIR/ThintureGpsTracker.jar" ]; then
    echo "Creating backup..."
    sudo mkdir -p $BACKUP_DIR
    sudo cp "$APP_DIR/ThintureGpsTracker.jar" "$BACKUP_DIR/ThintureGpsTracker.jar.$(date +%Y%m%d_%H%M%S)"
fi

# Deploy
echo "Deploying new version..."
sudo mkdir -p $APP_DIR
sudo cp ThintureGpsTracker.jar "$APP_DIR/"
sudo cp *.sh "$APP_DIR/"
sudo cp *.properties "$APP_DIR/"
sudo chmod +x "$APP_DIR"/*.sh

# Start service
echo "Starting service..."
sudo systemctl start $SERVICE_NAME

# Check status
sleep 5
if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Service started successfully"
else
    echo "Service failed to start"
    sudo journalctl -u $SERVICE_NAME -n 50
    exit 1
fi
EOF
    
    chmod +x deploy-package/remote-deploy.sh
    
    # Copy files to remote
    print_status "Copying files to remote host..."
    scp -r deploy-package/* $DEPLOY_USER@$DEPLOY_HOST:/tmp/
    
    # Execute remote deployment
    print_status "Executing remote deployment..."
    ssh $DEPLOY_USER@$DEPLOY_HOST "cd /tmp && ./remote-deploy.sh"
    
    # Cleanup
    rm -rf deploy-package
    
    print_success "Remote deployment completed"
}

# Function to verify deployment
verify_deployment() {
    print_status "Verifying deployment..."
    
    # Get target host
    if [ -z "$DEPLOY_HOST" ]; then
        TARGET_HOST="localhost:8182"
    else
        TARGET_HOST="$DEPLOY_HOST:8182"
    fi
    
    # Check health endpoint
    print_status "Checking health endpoint..."
    HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://$TARGET_HOST/actuator/health || echo "000")
    
    if [ "$HEALTH_CHECK" = "200" ]; then
        print_success "Health check passed"
        
        # Get detailed health
        HEALTH_STATUS=$(curl -s http://$TARGET_HOST/actuator/health | grep -o '"status":"[^"]*"' || echo "unknown")
        print_status "Health status: $HEALTH_STATUS"
    else
        print_error "Health check failed (HTTP $HEALTH_CHECK)"
        exit 1
    fi
    
    # Check metrics endpoint
    print_status "Checking metrics endpoint..."
    METRICS_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://$TARGET_HOST/actuator/metrics || echo "000")
    
    if [ "$METRICS_CHECK" = "200" ]; then
        print_success "Metrics endpoint available"
    else
        print_warning "Metrics endpoint not available (HTTP $METRICS_CHECK)"
    fi
}

# Function to create systemd service
create_systemd_service() {
    print_status "Creating systemd service..."
    
    sudo tee /etc/systemd/system/gps-tracker.service > /dev/null << EOF
[Unit]
Description=Thinture GPS Tracker Service
After=network.target

[Service]
Type=simple
User=$DEPLOY_USER
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/java -jar $APP_DIR/$JAR_FILE --spring.profiles.active=$ENVIRONMENT
ExecStop=/bin/kill -15 \$MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=gps-tracker

# Memory settings
Environment="JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

[Install]
WantedBy=multi-user.target
EOF
    
    # Reload systemd
    sudo systemctl daemon-reload
    sudo systemctl enable gps-tracker
    
    print_success "Systemd service created"
}

# Function to rollback deployment
rollback() {
    print_warning "Initiating rollback..."
    
    # Find latest backup
    LATEST_BACKUP=$(ls -t $BACKUP_DIR/$JAR_FILE.* 2>/dev/null | head -1)
    
    if [ -z "$LATEST_BACKUP" ]; then
        print_error "No backup found for rollback"
        exit 1
    fi
    
    print_status "Rolling back to: $LATEST_BACKUP"
    
    # Stop service
    sudo systemctl stop $SERVICE_NAME
    
    # Restore backup
    sudo cp "$LATEST_BACKUP" "$APP_DIR/$JAR_FILE"
    
    # Start service
    sudo systemctl start $SERVICE_NAME
    
    # Verify
    sleep 5
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_success "Rollback completed successfully"
    else
        print_error "Rollback failed"
        exit 1
    fi
}

# Main deployment flow
main() {
    echo "============================================"
    echo "GPS Tracker Deployment Script"
    echo "Environment: $ENVIRONMENT"
    echo "Host: ${DEPLOY_HOST:-localhost}"
    echo "Skip Tests: $SKIP_TESTS"
    echo "============================================"
    echo ""
    
    # Check if rollback requested
    if [ "$ENVIRONMENT" = "rollback" ]; then
        rollback
        exit 0
    fi
    
    # Run deployment steps
    check_prerequisites
    run_tests
    build_application
    
    # Check if systemd service exists
    if [ ! -f "/etc/systemd/system/gps-tracker.service" ]; then
        create_systemd_service
    fi
    
    # Deploy
    if [ -z "$DEPLOY_HOST" ]; then
        deploy_local
    else
        deploy_remote
    fi
    
    # Verify
    verify_deployment
    
    print_success "Deployment completed successfully!"
    echo ""
    echo "Next steps:"
    echo "1. Monitor logs: sudo journalctl -u gps-tracker -f"
    echo "2. Check metrics: http://${DEPLOY_HOST:-localhost}:8182/actuator/metrics"
    echo "3. View health: http://${DEPLOY_HOST:-localhost}:8182/actuator/health"
}

# Run main function
main

exit 0
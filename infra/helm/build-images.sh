#!/usr/bin/env bash
# build-images.sh
#
# Builds all Spring Boot service images into the Docker Desktop daemon.
# Must be run after ./gradlew bootJar succeeds.
#
# Usage:
#   ./infra/helm/build-images.sh              # build all services
#   ./infra/helm/build-images.sh api-gateway  # build a single service

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

GREEN='\033[0;32m'
NC='\033[0m'
log() { echo -e "${GREEN}[deli]${NC} $*"; }

SERVICES=(
    api-gateway
    route-service
    delivery-service
    location-service
    notification-service
)

build_service() {
    local svc="$1"
    local service_dir="$PROJECT_ROOT/services/$svc"

    if [[ ! -f "$service_dir/Dockerfile" ]]; then
        echo "No Dockerfile found for $svc — skipping"
        return
    fi

    log "Building deli/$svc:local ..."

    # Build the fat JAR first
    "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" ":services:$svc:bootJar" -q

    # Build the Docker image
    docker build \
        -t "deli/$svc:local" \
        -f "$service_dir/Dockerfile" \
        "$service_dir"

    log "deli/$svc:local built successfully."
}

if [[ $# -gt 0 ]]; then
    # Build specific service(s) passed as arguments
    for svc in "$@"; do
        build_service "$svc"
    done
else
    # Build all services
    log "Building all service images..."
    for svc in "${SERVICES[@]}"; do
        build_service "$svc"
    done
    log "All images built."
fi

log ""
log "Verify with: docker images | grep deli"

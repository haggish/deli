#!/usr/bin/env bash
# deploy-local.sh
#
# Deploys the full Deli platform to Docker Desktop Kubernetes.
# Run from the project root: ./infra/helm/deploy-local.sh
#
# Prerequisites:
#   - Docker Desktop running with Kubernetes enabled
#   - kubectl context set to docker-desktop
#   - Helm 3.x installed
#   - nginx ingress controller installed (see README)
#   - Service images built: docker build -t deli/<service>:local ./services/<service>
#
# Usage:
#   ./infra/helm/deploy-local.sh             # deploy everything
#   ./infra/helm/deploy-local.sh --infra     # deploy only infrastructure
#   ./infra/helm/deploy-local.sh --services  # deploy only services
#   ./infra/helm/deploy-local.sh --down      # uninstall everything

set -euo pipefail

NAMESPACE="delivery"
PLATFORM_RELEASE="deli-platform"
SERVICES_RELEASE="deli-services"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_CHART="$SCRIPT_DIR/deli-platform"
SERVICES_CHART="$SCRIPT_DIR/deli-services"

# ── Colours ───────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[deli]${NC} $*"; }
warn() { echo -e "${YELLOW}[warn]${NC} $*"; }
fail() { echo -e "${RED}[fail]${NC} $*"; exit 1; }

# ── Preflight checks ──────────────────────────────────────────────────────────
preflight() {
    log "Running preflight checks..."

    command -v kubectl >/dev/null || fail "kubectl not found"
    command -v helm    >/dev/null || fail "helm not found"

    local ctx
    ctx=$(kubectl config current-context 2>/dev/null || echo "none")
    if [[ "$ctx" != "docker-desktop" ]]; then
        warn "Current kubectl context is '$ctx', not 'docker-desktop'."
        warn "Switch with: kubectl config use-context docker-desktop"
        read -rp "Continue anyway? [y/N] " answer
        [[ "$answer" =~ ^[Yy]$ ]] || exit 0
    fi

    # Check nginx ingress is installed
    if ! kubectl get deployment ingress-nginx-controller -n ingress-nginx &>/dev/null; then
        warn "nginx ingress controller not found. Installing..."
        kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml
        log "Waiting for ingress controller to be ready..."
        kubectl wait --namespace ingress-nginx \
            --for=condition=ready pod \
            --selector=app.kubernetes.io/component=controller \
            --timeout=120s
    fi

    log "Preflight checks passed."
}

# ── Deploy infrastructure ─────────────────────────────────────────────────────
deploy_platform() {
    log "Updating Helm dependencies for deli-platform..."
    helm dependency update "$PLATFORM_CHART"

    log "Deploying infrastructure ($PLATFORM_RELEASE)..."
    helm upgrade --install "$PLATFORM_RELEASE" "$PLATFORM_CHART" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        -f "$PLATFORM_CHART/values-local.yaml" \
        --timeout 15m \
        --wait-for-jobs=false

    log "Infrastructure deployed. Waiting for Kafka to be ready..."
    kubectl wait pod \
        -l "app.kubernetes.io/name=kafka" \
        -n "$NAMESPACE" \
        --for=condition=Ready \
        --timeout=300s || warn "Kafka readiness timeout — it may still be starting"

    log "Platform ready."
}

# ── Deploy services ───────────────────────────────────────────────────────────
deploy_services() {
    log "Updating Helm dependencies for deli-services..."
    # Update deli-common dependency for each service chart
    for svc_chart in "$SERVICES_CHART"/charts/*/; do
        [[ -f "$svc_chart/Chart.yaml" ]] && helm dependency update "$svc_chart" 2>/dev/null || true
    done
    helm dependency update "$SERVICES_CHART"

    log "Deploying services ($SERVICES_RELEASE)..."
    helm upgrade --install "$SERVICES_RELEASE" "$SERVICES_CHART" \
        --namespace "$NAMESPACE" \
        -f "$SERVICES_CHART/values-local.yaml" \
        --timeout 5m \
        --wait

    log "Services deployed."
}

# ── Tear down ─────────────────────────────────────────────────────────────────
tear_down() {
    warn "Uninstalling all Deli Helm releases..."
    helm uninstall "$SERVICES_RELEASE" -n "$NAMESPACE" 2>/dev/null || true
    helm uninstall "$PLATFORM_RELEASE" -n "$NAMESPACE" 2>/dev/null || true
    warn "Releases removed. PersistentVolumeClaims are retained."
    warn "To also delete PVCs: kubectl delete pvc --all -n $NAMESPACE"
}

# ── Status ────────────────────────────────────────────────────────────────────
show_status() {
    log "=== Pod status ==="
    kubectl get pods -n "$NAMESPACE"
    log "=== Services ==="
    kubectl get svc -n "$NAMESPACE"
    log "=== Ingress ==="
    kubectl get ingress -n "$NAMESPACE"
}

# ── Main ──────────────────────────────────────────────────────────────────────
MODE="${1:-all}"

case "$MODE" in
    --infra)
        preflight
        deploy_platform
        show_status
        ;;
    --services)
        preflight
        deploy_services
        show_status
        ;;
    --down)
        tear_down
        ;;
    --status)
        show_status
        ;;
    all|"")
        preflight
        deploy_platform
        deploy_services
        show_status

        log ""
        log "╔══════════════════════════════════════════════════════╗"
        log "║  Deli platform deployed to Docker Desktop            ║"
        log "║                                                      ║"
        log "║  Add to C:\\Windows\\System32\\drivers\\etc\\hosts :     ║"
        log "║    127.0.0.1  api.courier.local                      ║"
        log "║    127.0.0.1  ws.courier.local                       ║"
        log "║    127.0.0.1  minio.courier.local                    ║"
        log "║                                                      ║"
        log "║  REST API  →  http://api.courier.local               ║"
        log "║  WebSocket →  ws://ws.courier.local                  ║"
        log "║  MinIO     →  http://minio.courier.local             ║"
        log "╚══════════════════════════════════════════════════════╝"
        ;;
    *)
        echo "Usage: $0 [--infra|--services|--down|--status]"
        exit 1
        ;;
esac

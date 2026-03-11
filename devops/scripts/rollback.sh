#!/usr/bin/env bash
set -euo pipefail

ENVIRONMENT="${1:?Usage: rollback.sh <environment>}"

: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"
: "${K8S_DEPLOYMENT_NAME:?K8S_DEPLOYMENT_NAME is required}"

echo "=== Rollback start ==="
echo "Environment: ${ENVIRONMENT}"
echo "Namespace:   ${K8S_NAMESPACE}"
echo "Service:     ${K8S_DEPLOYMENT_NAME}"

ACTIVE_COLOR="$(kubectl -n "${K8S_NAMESPACE}" get deploy "${K8S_DEPLOYMENT_NAME}" -o jsonpath='{.metadata.labels.color}')"
if [[ "${ACTIVE_COLOR}" == "blue" ]]; then
  ROLLBACK_COLOR="green"
else
  ROLLBACK_COLOR="blue"
fi

echo "Active color:   ${ACTIVE_COLOR:-unknown}"
echo "Rollback color: ${ROLLBACK_COLOR}"

SERVICE_PATCH="$(printf '{"spec":{"selector":{"app":"%s","color":"%s"}}}' \
  "${K8S_DEPLOYMENT_NAME}" "${ROLLBACK_COLOR}")"

kubectl -n "${K8S_NAMESPACE}" patch service "${K8S_DEPLOYMENT_NAME}" -p "${SERVICE_PATCH}"
kubectl -n "${K8S_NAMESPACE}" label deployment "${K8S_DEPLOYMENT_NAME}" "color=${ROLLBACK_COLOR}" --overwrite

echo "=== Rollback complete ==="

#!/usr/bin/env bash
set -euo pipefail

ENVIRONMENT="${1:?Usage: deploy-blue-green.sh <environment> <image_tag>}"
IMAGE_TAG="${2:?Usage: deploy-blue-green.sh <environment> <image_tag>}"

: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"
: "${K8S_DEPLOYMENT_NAME:?K8S_DEPLOYMENT_NAME is required}"
: "${K8S_CONTAINER_NAME:?K8S_CONTAINER_NAME is required}"
: "${REGISTRY_IMAGE:?REGISTRY_IMAGE is required}"
: "${SMOKE_TEST_URL:?SMOKE_TEST_URL is required}"

SMOKE_TEST_RETRIES="${SMOKE_TEST_RETRIES:-12}"
SMOKE_TEST_DELAY_SECONDS="${SMOKE_TEST_DELAY_SECONDS:-5}"
SMOKE_TEST_EXPECTED_TEXT="${SMOKE_TEST_EXPECTED_TEXT:-}"

switch_service_selector() {
  local color="$1"
  kubectl -n "${K8S_NAMESPACE}" patch service "${K8S_DEPLOYMENT_NAME}" \
    -p "{\"spec\":{\"selector\":{\"app\":\"${K8S_DEPLOYMENT_NAME}\",\"color\":\"${color}\"}}}"
}

run_smoke_test() {
  local attempt=1
  while [ "${attempt}" -le "${SMOKE_TEST_RETRIES}" ]; do
    local response
    response="$(curl -fsS --max-time 10 "${SMOKE_TEST_URL}" || true)"

    if [ -n "${response}" ]; then
      if [ -z "${SMOKE_TEST_EXPECTED_TEXT}" ] || printf "%s" "${response}" | grep -q "${SMOKE_TEST_EXPECTED_TEXT}"; then
        echo "Smoke test passed on attempt ${attempt}"
        return 0
      fi
    fi

    echo "Smoke test failed on attempt ${attempt}/${SMOKE_TEST_RETRIES}, retrying in ${SMOKE_TEST_DELAY_SECONDS}s"
    sleep "${SMOKE_TEST_DELAY_SECONDS}"
    attempt=$((attempt + 1))
  done

  return 1
}

echo "=== Blue/Green deploy start ==="
echo "Environment:    ${ENVIRONMENT}"
echo "Image tag:      ${IMAGE_TAG}"
echo "Namespace:      ${K8S_NAMESPACE}"
echo "Deployment:     ${K8S_DEPLOYMENT_NAME}"
echo "Smoke URL:      ${SMOKE_TEST_URL}"

ACTIVE_COLOR="$(kubectl -n "${K8S_NAMESPACE}" get deploy "${K8S_DEPLOYMENT_NAME}" -o jsonpath='{.metadata.labels.color}')"
if [[ "${ACTIVE_COLOR}" == "blue" ]]; then
  TARGET_COLOR="green"
else
  TARGET_COLOR="blue"
fi

echo "Active color:   ${ACTIVE_COLOR:-unknown}"
echo "Target color:   ${TARGET_COLOR}"

TARGET_DEPLOYMENT="${K8S_DEPLOYMENT_NAME}-${TARGET_COLOR}"
IMAGE_REF="${REGISTRY_IMAGE}:${IMAGE_TAG}"

echo "Updating target deployment image -> ${IMAGE_REF}"
kubectl -n "${K8S_NAMESPACE}" set image "deployment/${TARGET_DEPLOYMENT}" "${K8S_CONTAINER_NAME}=${IMAGE_REF}"

echo "Waiting rollout for ${TARGET_DEPLOYMENT}"
kubectl -n "${K8S_NAMESPACE}" rollout status "deployment/${TARGET_DEPLOYMENT}" --timeout=180s

echo "Running readiness check on ${TARGET_DEPLOYMENT}"
kubectl -n "${K8S_NAMESPACE}" wait --for=condition=available "deployment/${TARGET_DEPLOYMENT}" --timeout=180s

echo "Switching service selector to color=${TARGET_COLOR}"
switch_service_selector "${TARGET_COLOR}"

echo "Running post-switch smoke test"
if ! run_smoke_test; then
  echo "Smoke test failed after traffic switch. Reverting selector to color=${ACTIVE_COLOR}"
  switch_service_selector "${ACTIVE_COLOR}"
  exit 1
fi

echo "Updating active deployment label color=${TARGET_COLOR}"
kubectl -n "${K8S_NAMESPACE}" label deployment "${K8S_DEPLOYMENT_NAME}" "color=${TARGET_COLOR}" --overwrite

echo "=== Blue/Green deploy complete ==="

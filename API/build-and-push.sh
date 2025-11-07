#!/bin/bash

set -e  # Exit on any error

# Base image name
# IMAGE="registry.gitlab.com/adisa-shobi-group/adisa-shobi-project"
IMAGE="ghcr.io/adisa-shobi/aquaforecast-api-ghcr"

# Generate timestamp: YYYYMMDD-HHMMSS
TIMESTAMP=$(date '+%Y%m%d-%H%M%S')
TAGGED_IMAGE="$IMAGE:$TIMESTAMP"
LATEST_IMAGE="$IMAGE:latest"

echo "Building multi-stage image: $TAGGED_IMAGE"
DOCKER_BUILDKIT=1 docker build \
    --platform linux/amd64 \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    -t "$LATEST_IMAGE" \
    .

echo "Pushing $LATEST_IMAGE"
docker push "$LATEST_IMAGE"

# echo "Pushing $TAGGED_IMAGE"
# docker push "$TAGGED_IMAGE"

echo "Build and push completed: $LATEST_IMAGE"
# echo "Build and push completed: $TAGGED_IMAGE"
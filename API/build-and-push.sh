#!/bin/bash

set -e  # Exit on any error

# Base image name
IMAGE="registry.gitlab.com/adisa-shobi-group/adisa-shobi-project"

# Generate timestamp: YYYYMMDD-HHMMSS
TIMESTAMP=$(date '+%Y%m%d-%H%M%S')
TAGGED_IMAGE="$IMAGE:$TIMESTAMP"
LATEST_IMAGE="$IMAGE:latest"

echo "Building image: $TAGGED_IMAGE"
docker build --platform linux/amd64 -t "$TAGGED_IMAGE" .

echo "Pushing $TAGGED_IMAGE"
docker push "$TAGGED_IMAGE"
#!/bin/bash

# Endpoint
ENDPOINT="http://localhost:8080/api/v1/shorturl/"

# Array de URLs
URLS=(
  # General Interest
  "https://www.google.com"
  "https://www.wikipedia.org"
  "https://www.reddit.com"
  "https://www.youtube.com"
  # News
  "https://www.nytimes.com"
  "https://www.bbc.com"
  "https://www.cnn.com"
  # Social Media
  "https://www.facebook.com"
  "https://www.twitter.com"
  "https://www.instagram.com"
  # E-commerce
  "https://www.amazon.com"
  "https://www.ebay.com"
  "https://www.target.com"
  # Technology
  "https://www.github.com"
  "https://www.stackoverflow.com"
  "https://www.mozilla.org"
  # Random & Unique
  "https://www.zombo.com"
  "https://www.theuselessweb.com"
)

# Loop e invocación de curl
for url in "${URLS[@]}"; do
  echo "Sending: $url"
  curl --silent --show-error --location "$ENDPOINT" \
    --header 'Content-Type: application/json' \
    --data "{\"originalUrl\": \"$url\"}"
  echo -e "\n"
done

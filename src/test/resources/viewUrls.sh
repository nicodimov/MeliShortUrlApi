#!/bin/bash

# Endpoint base
BASE_URL="http://localhost:8080/api/v1/shorturl/view"

# Códigos
CODES=(
  "0dkT0p"
  "70gvG7"
  "h6ksQU"
  "Iu28Xw"
  "yXVO8n"
  "1mFtC6"
  "07Yuj4"
  "Dm1PJv"
  "hUEo1Q"
  "Yh5Fqg"
  "PxLpaV"
  "66W67s"
  "2V2yoH"
  "NvQvDj"
  "vcwINF"
)

# Loop e invocación
for code in "${CODES[@]}"; do
  echo "Viewing: $code"
  curl --silent --show-error --location "$BASE_URL/$code"
  echo -e "\n"
done

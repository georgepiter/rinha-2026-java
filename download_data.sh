#!/bin/bash
mkdir -p data
echo "Baixando arquivos de referência..."
curl -L https://github.com/zanfranceschi/rinha-de-backend-2026/raw/main/resources/normalization.json -o data/normalization.json
curl -L https://github.com/zanfranceschi/rinha-de-backend-2026/raw/main/resources/mcc_risk.json -o data/mcc_risk.json
curl -L https://github.com/zanfranceschi/rinha-de-backend-2026/raw/main/resources/references.json.gz -o data/references.json.gz
echo "Download concluído."

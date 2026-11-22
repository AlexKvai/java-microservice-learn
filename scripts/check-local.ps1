$ErrorActionPreference = 'Stop'

function Check-Endpoint([string]$Name, [string]$Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
        Write-Host "[OK] $Name : HTTP $($response.StatusCode)"
    } catch {
        Write-Host "[FAIL] $Name : $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

Write-Host 'Checking Docker Compose services...'
docker compose ps
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose failed' }

Write-Host 'Checking Redis...'
docker compose exec -T redis redis-cli PING
if ($LASTEXITCODE -ne 0) { throw 'Redis is unavailable' }

Write-Host 'Checking Kafka topics...'
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
if ($LASTEXITCODE -ne 0) { throw 'Kafka is unavailable' }

Check-Endpoint 'EventManager OpenAPI' 'http://localhost:8080/v3/api-docs'
Check-Endpoint 'EventNotificator OpenAPI' 'http://localhost:8081/v3/api-docs'
Write-Host '[OK] Basic infrastructure checks passed' -ForegroundColor Green

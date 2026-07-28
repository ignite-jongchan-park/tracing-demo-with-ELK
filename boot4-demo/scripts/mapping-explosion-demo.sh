#!/usr/bin/env bash
# ELK 매핑 폭발(Field explosion) 재현 데모
# 전제: boot4-demo 의 Elasticsearch 가 localhost:9200 에 떠 있어야 함
#   (cd boot4-demo && docker compose up -d elasticsearch  또는 전체 up)
set -uo pipefail
ES=${ES:-http://localhost:9200}

echo "=== 0) ES 상태 확인 ==="
if ! curl -sf "$ES/_cluster/health" >/dev/null; then
  echo "  ✗ ES 응답 없음 ($ES). boot4-demo 에서 docker compose up 먼저 하세요."
  exit 1
fi
curl -s "$ES/_cluster/health?pretty" | grep -E '"status"'

# ---------------- 1) 좋은 패턴: traceId 를 '값(value)'으로 ----------------
echo
echo "=== 1) 좋은 패턴: traceId 를 값(value)으로 넣기 ==="
curl -s -X DELETE "$ES/trace-good" >/dev/null 2>&1
for i in 1 2 3; do
  curl -s -X POST "$ES/trace-good/_doc" -H 'Content-Type: application/json' \
    -d "{\"service\":\"order\",\"traceId\":\"trace-$i\",\"userId\":\"u$i\",\"message\":\"order created\"}" >/dev/null
done
echo "→ 문서 3개 삽입 완료. 매핑 필드 개수:"
curl -s "$ES/trace-good/_mapping" | python3 -c '
import sys, json
m = json.load(sys.stdin)
props = list(m.values())[0]["mappings"]["properties"]
print(f"   {len(props)}개 필드: {list(props)}")
print("   → traceId 가 아무리 많아도 필드는 그대로. 안전.")
'

# ---------------- 2) 나쁜 패턴: traceId/userId 를 '키'로 → 필드 폭발 ----------------
echo
echo "=== 2) 나쁜 패턴: 동적 값을 필드 '키'로 넣기 → 필드 폭발 ==="
curl -s -X DELETE "$ES/trace-bad" >/dev/null 2>&1
curl -s -X PUT "$ES/trace-bad" -H 'Content-Type: application/json' -d '{}' >/dev/null
echo "→ index.mapping.total_fields.limit 기본값 = 1000"
echo "→ 요청마다 traceId 를 필드명으로 쓴다고 가정하고, 서로 다른 필드 1001개 삽입 시도..."
python3 - "$ES" <<'PY'
import sys, json, urllib.request, urllib.error
es = sys.argv[1]
doc = {f"user_{i}": "x" for i in range(1001)}   # 1001개의 서로 다른 필드명
req = urllib.request.Request(f"{es}/trace-bad/_doc",
                             data=json.dumps(doc).encode(),
                             headers={"Content-Type": "application/json"},
                             method="POST")
try:
    r = urllib.request.urlopen(req)
    print("   (예상 밖) 성공:", r.status)
except urllib.error.HTTPError as e:
    body = json.loads(e.read())
    err = body["error"]
    print("   ✗ HTTP", e.code)
    print("   error.type   :", err["type"])
    print("   error.reason :", err["reason"])
PY

echo
echo "=== 정리 (원하면) ==="
echo "  curl -X DELETE $ES/trace-good $ES/trace-bad"

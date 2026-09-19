#!/usr/bin/env bash
#
# Full-stack smoke pass (TCM-30): every role's core journey, run against a
# stack that is already up.
#
#   docker compose up --build -d
#   ./scripts/smoke.sh
#
# It uses the seeded demo accounts and creates a handful of rows of its own
# (a student, a course, a session), so it is safe to re-run - each run works
# with its own timestamped records. Exits non-zero if anything fails.
#
# Set API to point somewhere other than a local stack:
#   API=${API:-http://localhost:8080/api/v1} ./scripts/smoke.sh
set -uo pipefail
API=${API:-http://localhost:8080/api/v1}
PASS=0; FAIL=0

check() { # check <name> <expected-substring-or-status> <actual>
  if [[ "$3" == *"$2"* ]]; then printf '  PASS  %s\n' "$1"; PASS=$((PASS+1));
  else printf '  FAIL  %s\n        expected %s in: %s\n' "$1" "$2" "${3:0:300}"; FAIL=$((FAIL+1)); fi
}
login() { curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])'; }
get()  { curl -s -H "Authorization: Bearer $1" "$API$2"; }
post() { curl -s -X POST -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -d "$3" "$API$2"; }
# code <method> <token> <path> [json-body]
code() { curl -s -o /dev/null -w '%{http_code}' -X "$1" -H "Authorization: Bearer $2" \
  -H 'Content-Type: application/json' ${4:+-d "$4"} "$API$3"; }
jq_() { python3 -c "import sys,json;d=json.load(sys.stdin);print($1)"; }

echo "== Auth =="
ADMIN=$(login admin@tcm.local 'ChangeMe123!')
TINA=$(login tina.trainer@tcm.local 'ChangeMe123!')
SAM=$(login sam.student@tcm.local 'ChangeMe123!')
check "admin logs in"   "ey" "$ADMIN"
check "trainer logs in" "ey" "$TINA"
check "student logs in" "ey" "$SAM"

echo "== Seeded demo data =="
check "3 demo courses"        '"totalElements": 3' "$(get "$ADMIN" '/courses?size=50' | python3 -m json.tool)"
check "demo students listed"  'sam.student@tcm.local' "$(get "$ADMIN" '/students?size=50')"
check "schedule seeded"       'Room A' "$(get "$ADMIN" '/sessions?size=50&from=2000-01-01')"

echo "== Admin journey =="
STAMP=$(date +%s)
NEW_USER=$(post "$ADMIN" /users "{\"firstName\":\"Smoke\",\"lastName\":\"Student\",\"email\":\"smoke-$STAMP@tcm.local\",\"password\":\"Secret123!\",\"role\":\"STUDENT\"}")
NEW_USER_ID=$(echo "$NEW_USER" | jq_ 'd["id"]')
check "creates a user" "smoke-$STAMP@tcm.local" "$NEW_USER"
NEW_COURSE=$(post "$ADMIN" /courses "{\"code\":\"SMOKE-$STAMP\",\"name\":\"Smoke Course\",\"durationHours\":8,\"capacity\":5,\"price\":100.00,\"status\":\"PUBLISHED\",\"primaryTrainerId\":\"11111111-1111-4111-8111-111111111101\"}")
COURSE_ID=$(echo "$NEW_COURSE" | jq_ 'd["id"]')
check "creates a course" "SMOKE-$STAMP" "$NEW_COURSE"

SMOKE=$(login "smoke-$STAMP@tcm.local" 'Secret123!')
ENROLL=$(post "$SMOKE" /enrollments "{\"courseId\":\"$COURSE_ID\"}")
ENROLL_ID=$(echo "$ENROLL" | jq_ 'd["id"]')
check "student self-enrolls" '"PENDING"' "$ENROLL"
check "admin approves"       '"APPROVED"' "$(post "$ADMIN" "/enrollments/$ENROLL_ID/decision" '{"status":"APPROVED"}')"
check "approval raises an invoice" "$COURSE_ID" "$(get "$ADMIN" "/payments?studentId=$NEW_USER_ID")"

SESSION=$(post "$ADMIN" /sessions "{\"courseId\":\"$COURSE_ID\",\"trainerId\":\"11111111-1111-4111-8111-111111111101\",\"classroom\":\"Room S\",\"sessionDate\":\"2027-06-01\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}")
SESSION_ID=$(echo "$SESSION" | jq_ 'd["id"]')
check "schedules a session" 'Room S' "$SESSION"
check "double-booking is refused" '409' "$(code POST "$ADMIN" /sessions '{"courseId":"'$COURSE_ID'","trainerId":"11111111-1111-4111-8111-111111111101","classroom":"Room S","sessionDate":"2027-06-01","startTime":"10:00","endTime":"12:00"}')"
check "a missing body is 400, not 500" '400' "$(code POST "$ADMIN" /sessions)"

echo "== Trainer journey =="
check "trainer sees own schedule only" 'Room A' "$(get "$TINA" '/sessions?size=50&from=2000-01-01')"
ROSTER=$(get "$TINA" "/sessions/$SESSION_ID/attendance")
check "reads the roster" "smoke-$STAMP@tcm.local" "$ROSTER"
check "marks attendance" '"PRESENT"' "$(post "$TINA" "/sessions/$SESSION_ID/attendance" "{\"entries\":[{\"studentId\":\"$NEW_USER_ID\",\"status\":\"PRESENT\"}]}")"
check "course attendance report" '"attendanceRate": 100.0' "$(get "$TINA" "/courses/$COURSE_ID/attendance-report" | python3 -m json.tool)"
check "records a grade" '"percentage": 90.0' "$(post "$TINA" /grades "{\"studentId\":\"$NEW_USER_ID\",\"courseId\":\"$COURSE_ID\",\"assessmentType\":\"EXAM\",\"title\":\"Smoke exam\",\"score\":18,\"maxScore\":20,\"weight\":100}" | python3 -m json.tool)"
check "gradebook lists the student" 'Smoke Student' "$(get "$TINA" "/courses/$COURSE_ID/grades")"

QR=$(post "$TINA" "/sessions/$SESSION_ID/qr" '')
QR_TOKEN=$(echo "$QR" | jq_ 'd["token"]')
check "issues a QR code" '/attend/' "$QR"
check "QR image is a PNG" 'iVBOR' "$QR"
check "student checks in by QR" '"QR"' "$(post "$SMOKE" /attendance/qr-checkin "{\"sessionId\":\"$SESSION_ID\",\"token\":\"$QR_TOKEN\"}")"
post "$TINA" "/sessions/$SESSION_ID/qr" '' > /dev/null  # a newer code retires the one just used
check "a retired QR is 410" '410' "$(code POST "$SMOKE" /attendance/qr-checkin "{\"sessionId\":\"$SESSION_ID\",\"token\":\"$QR_TOKEN\"}")"

echo "== Payments =="
PAYMENT_ID=$(get "$ADMIN" "/payments?studentId=$NEW_USER_ID" | jq_ 'd["content"][0]["id"]')
check "records a payment" '"PAID"' "$(post "$ADMIN" "/payments/$PAYMENT_ID/transactions" '{"amount":100.00,"paymentMethod":"Cash"}')"
check "student sees own invoices" '"PAID"' "$(get "$SMOKE" /payments/mine)"

echo "== Certificates =="
check "ineligible is refused with a reason" 'COMPLETED' "$(post "$ADMIN" /certificates/generate "{\"studentId\":\"$NEW_USER_ID\",\"courseId\":\"$COURSE_ID\"}")"
check "enrollment completes" '"COMPLETED"' "$(post "$ADMIN" "/enrollments/$ENROLL_ID/complete" '')"
CERT=$(post "$ADMIN" /certificates/generate "{\"studentId\":\"$NEW_USER_ID\",\"courseId\":\"$COURSE_ID\"}")
CERT_ID=$(echo "$CERT" | jq_ 'd["id"]')
check "certificate is issued" 'CERT-' "$CERT"
check "duplicate is refused" '409' "$(code POST "$ADMIN" /certificates/generate "{\"studentId\":\"$NEW_USER_ID\",\"courseId\":\"$COURSE_ID\"}")"
curl -s -H "Authorization: Bearer $SMOKE" "$API/certificates/$CERT_ID/download" -o /tmp/smoke-cert.pdf
check "student downloads a real PDF" 'PDF document' "$(file -b /tmp/smoke-cert.pdf)"

echo "== Dashboards & student views =="
check "admin dashboard"   '"activeStudents"' "$(get "$ADMIN" /dashboard/summary)"
check "trainer dashboard" '"myCourses"' "$(get "$TINA" /dashboard/trainer-summary)"
check "student summary is complete" '"overallGrade"' "$(get "$SAM" "/students/$(get "$SAM" /auth/me | jq_ 'd["id"]')/summary")"
check "student sees own grades" '"weightedAverage"' "$(get "$SMOKE" "/students/$NEW_USER_ID/grades")"
check "student sees own certificates" 'CERT-' "$(get "$SMOKE" /certificates)"
check "student cannot read the payments ledger" '403' "$(code GET "$SMOKE" /payments)"
check "trainer cannot read the admin dashboard" '403' "$(code GET "$TINA" /dashboard/summary)"

echo
echo "PASS=$PASS FAIL=$FAIL"
[[ $FAIL -eq 0 ]]

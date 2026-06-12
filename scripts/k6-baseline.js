// PERF-16 — baseline load test for the billing backend.
//
// Run after each Tier-1 perf fix and record p50/p95 here as a diff log.
// Targets the read endpoints that the SPA hits most on first paint.
//
// Install k6 once:   choco install k6   (Windows)
//                    brew install k6    (macOS)
//                    https://k6.io/docs/get-started/installation/
//
// Run against a local backend:
//   k6 run -e BASE=http://localhost:8080 scripts/k6-baseline.js
//
// Or against the deployed server:
//   k6 run -e BASE=https://your-tunnel.ngrok-free.app scripts/k6-baseline.js
//
// VU = "virtual user". The stages below ramp from 0 -> 50 -> 100 -> 200
// concurrent and hold each level for 30s. Watch for the p95 cliff —
// that's where the backend starts queueing.

import http from "k6/http";
import { check, sleep } from "k6";

const BASE = __ENV.BASE || "http://localhost:8080";

export const options = {
  // Ramp up to find the latency cliff. Hold each plateau long enough
  // that JIT warm-up and Hikari ramp-up are out of the picture.
  stages: [
    { duration: "30s", target: 50 },
    { duration: "30s", target: 100 },
    { duration: "30s", target: 200 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    // Edit these as you optimise — start lenient so the test isn't
    // failing red the first time you run it.
    "http_req_duration": ["p(95)<2000"],
    "http_req_failed":   ["rate<0.05"],
  },
};

// The endpoints the frontend hits on every navigation. Each VU iteration
// roughly mirrors one user opening the dashboard, then a list page.
const READ_PATHS = [
  "/tenant/current",
  "/permanent-table",
  "/contracts-table",
  "/inward",
  "/outward",
  "/dyed",
  "/dyeing/issues",
  "/invoices",
  "/inventory",
];

export default function () {
  for (const path of READ_PATHS) {
    const res = http.get(`${BASE}${path}`, { tags: { name: path } });
    check(res, {
      [`${path} status is 200`]: (r) => r.status === 200,
    });
  }
  sleep(1);
}

// Record results here after each run so you can see the trend:
//
//   Date        Commit    p50 (ms)  p95 (ms)  Fail %   Notes
//   ----------  --------  --------  --------  -------  ----------------------
//   2026-06-12  21e84b0   ???       ???       ???      Pre Tier-1 baseline
//   ----        --------  --------  --------  -------  After PERF-1 + PERF-3
//   ----        --------  --------  --------  -------  After PERF-2 (N+1)

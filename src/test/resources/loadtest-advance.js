import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// Custom metric for latency
let latency = new Trend('http_req_duration', true);

export let options = {
  stages: [
    { duration: '1m', target: 50 },   // ramp up to 50 users
    { duration: '2m', target: 100 },  // stay at 100 users
    { duration: '2m', target: 200 },  // ramp up to 200 users
    { duration: '2m', target: 300 },  // ramp up to 300 users
    { duration: '2m', target: 0 },    // ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(90)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.01'],   // <1% errors
  },
};

export default function () {
  const url = 'http://localhost:8080/api/v1/shorturl/view/h6ksQU'; // Change as needed
  let res = http.get(url);
  latency.add(res.timings.duration);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1); // Adjust as needed
}
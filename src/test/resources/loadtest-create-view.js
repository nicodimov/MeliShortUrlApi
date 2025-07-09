import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

let latency = new Trend('http_req_duration', true);

export let options = {
    stages: [
      { duration: '1m', target: 50 },
      { duration: '2m', target: 100 },
      { duration: '2m', target: 200 },
      { duration: '2m', target: 300 },
      { duration: '2m', target: 0 },
    ],
    thresholds: {
      http_req_duration: ['p(90)<500'], // 90% of requests should be below 500ms
      http_req_failed: ['rate<0.01'],
    },
  };
  
  let shortUrls = [];
  
  export default function () {
    if (Math.random() < 0.5) {
      let originalUrl = `http://test.com/resource/${__VU}-${__ITER}-${Math.random()}`;
      let payload = JSON.stringify({ originalUrl: originalUrl });
      let res = http.post('http://localhost:8080/api/v1/shorturl', payload, {
        headers: { 'Content-Type': 'application/json' },
      });
      check(res, { 'POST status is 200': (r) => r.status === 200 });
      // Extract the short code from the response (adjust regex as needed)
      let match = res.body.match(/shorturl.*\/(\w+)/i);
      if (match) {
        shortUrls.push(match[1]);
      }
      latency.add(res.timings.duration);
    } else if (shortUrls.length > 0) {
      let idx = Math.floor(Math.random() * shortUrls.length);
      let code = shortUrls[idx];
      let res = http.get(`http://localhost:8080/api/v1/shorturl/view/${code}`);
      check(res, { 'GET status is 200': (r) => r.status === 200 });
      latency.add(res.timings.duration);
    }
    sleep(1);
  }
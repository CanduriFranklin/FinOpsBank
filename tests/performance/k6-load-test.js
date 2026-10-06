import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up a 50 Virtual Users
    { duration: '1m', target: 200 },  // Carga constante de 200 usuarios simultaneos
    { duration: '30s', target: 0 },   // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // El 95% de las peticiones debe responder en menos de 200ms
    http_req_failed: ['rate<0.001'],  // Tasa de error menor a 0.1%
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

export default function () {
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer YOUR_JWT_TOKEN_HERE',
    },
  };

  // 1. Consulta de saldo (Lectura)
  const resGet = http.get(\/accounts/ACC-100200300, params);
  check(resGet, {
    'Status es 200 u OK': (r) => r.status === 200,
  });

  sleep(0.5);
}
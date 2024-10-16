/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
import http from "k6/http";
import { check } from "k6";
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.3.0/index.js';

export const options = {
  vus: 3,
  duration: `${__ENV.DURATION_SECONDS || 10}s`,
  thresholds: {
    http_req_failed: ["rate<0.01"], // http errors should be less than 1%
    http_req_duration: [
      "p(99)<3000",
      "p(90)<2000",
      "avg<70",
      "med<70",
      "min<1000",
    ],
  },
  insecureSkipTLSVerify: true,
};

function generatePServer(someInt) {
  return JSON.stringify({
      'hostname': someInt,
      'ptnii-equip-name': `example-ptnii-equip-name-val-${someInt}`,
      'number-of-cpus': someInt,
      'disk-in-gigabytes': someInt,
      'ram-in-megabytes': someInt,
      'equip-type': `example-equip-type-val-${someInt}`,
      'equip-vendor': `example-equip-vendor-val-${someInt}`,
      'equip-model': `example-equip-model-val-${someInt}`,
      'fqdn': `example-fqdn-val-${someInt}`,
      'pserver-selflink': `example-pserver-selflink-val-${someInt}`,
      'ipv4-oam-address': `example-ipv4-oam-address-val-${someInt}`,
      'serial-number': `example-serial-number-val-${someInt}`,
      'ipaddress-v4-loopback-0': `example-ipaddress-v4-loopback0-val-${someInt}`,
      'ipaddress-v6-loopback-0': `example-ipaddress-v6-loopback0-val-${someInt}`,
      'ipaddress-v4-aim': `example-ipaddress-v4-aim-val-${someInt}`,
      'ipaddress-v6-aim': `example-ipaddress-v6-aim-val-${someInt}`,
      'ipaddress-v6-oam': `example-ipaddress-v6-oam-val-${someInt}`,
      'inv-status': `example-inv-status-val-${someInt}`,
      'pserver-id': `example-pserver-id-val-${someInt}`,
      'internet-topology': `example-internet-topology-val-${someInt}`,
      'in-maint': true,
      'pserver-name2': `example-pserver-name2-val-${someInt}`,
      'purpose': `example-purpose-val-${someInt}`,
      'prov-status': `example-prov-status-val-${someInt}`,
      'management-option': `example-management-option-val-${someInt}`,
      'host-profile': `example-host-profile-val-${someInt}`
  });
}

const baseUrl = `http://localhost:${__ENV.API_PORT || 8447}/aai/${__ENV.API_VERSION || 'v29'}`;
const path = `/cloud-infrastructure/pservers/pserver`;
const url = baseUrl + path;
const encodedCredentials = 'QUFJOkFBSQ==';
const httpOpts = {
  headers: {
    Accept: "application/json",
    Authorization: `Basic ${encodedCredentials}`,
    "X-FromAppId": "k6",
    "X-TransactionId": "someTransaction",
  },
};

export function setup() {
  // Perform a warmup with 100 requests
  for (let i = 0; i < 100; i++) {
      const someInt = randomIntBetween(10000, 1000000);
      const payload = generatePServer(someInt);
      const pserverUrl = url + `/${someInt}`
      const res = http.put(pserverUrl, payload, httpOpts);

      if (res.status != 201) {
        console.error(res);
      }
  }
}

export default function () {
  const someInt = randomIntBetween(10000, 1000000);
  const pserverUrl = url + `/${someInt}`
  const payload = generatePServer();
  const res = http.put(pserverUrl, payload, httpOpts);

  if (res.status != 201) {
    console.error(res);
  }

  check(res, {
    "status was 201": (r) => r.status == 201,
  });
}

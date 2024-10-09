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

export const options = {
  vus: 3,
  duration: `${__ENV.DURATION_SECONDS}s`,
  thresholds: {
    http_req_failed: ["rate<0.01"], // http errors should be less than 1%
    http_req_duration: [
      "p(99)<3000",
      "p(90)<2000",
      "avg<1000",
      "med<1000",
      "min<1000",
    ],
  },
  insecureSkipTLSVerify: true,
};

export default function () {
  const encodedCredentials = 'QUFJOkFBSQ==';
  const options = {
    headers: {
      Accept: "application/json",
      Authorization: `Basic ${encodedCredentials}`,
      "X-FromAppId": "k6",
      "X-TransactionId": "someTransaction",
    },
  };
  const pserverCount = parseInt(`${__ENV.N_PSERVERS}`, 10);
  const baseUrl = `http://localhost:${__ENV.API_PORT}/aai/${__ENV.API_VERSION}`;
  const url = `/cloud-infrastructure/pservers`;
  const res = http.get(baseUrl + url, options);

  if (res.status != 200) {
    console.error(res);
  }

  const parsedResponse = JSON.parse(res.body);
  if (parsedResponse.pserver.length != pserverCount) {
    console.error(`Expected ${pserverCount} results, got ${parsedResponse.pserver.length}`);
  }
  check(res, {
    "status was 200": (r) => r.status == 200,
    "returned correct number of results": () => parsedResponse.pserver.length == pserverCount,
  });
}

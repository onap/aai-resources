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
package org.onap.aai.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class PServer {
  String resourceVersion;
  String hostname;
  String ptniiEquipName;
  long numberOfCpus;
  long diskInGigabytes;
  long ramInMegabytes;
  String equipType;
  String equipVendor;
  String equipModel;
  String fqdn;
  String pserverSelflink;
  String ipv4OamAddress;
  String serialNumber;
  @JsonProperty("ipaddress-v4-loopback-0")
  String ipaddressV4Loopback0;
  @JsonProperty("ipaddress-v6-loopback-0")
  String ipaddressV6Loopback0;
  String ipaddressV4Aim;
  String ipaddressV6Aim;
  String ipaddressV6Oam;
  String invStatus;
  String pserverId;
  String internetTopology;
  boolean inMaint;
  String pserverName2;
  String purpose;
  String provStatus;
  String managementOption;
  String hostProfile;
}

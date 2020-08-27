/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
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
package org.onap.aai.rest.demoSpecific;

import org.onap.aai.rest.data.Permissions;
import org.onap.aai.rest.data.Resource;
import org.onap.aai.rest.data.Role;
import io.vavr.collection.HashSet;
import java.util.List;
import java.util.UUID;

public class DemoData {

    private DemoData() {
        super();
    }

    private static final Resource RESOURCE01 = Resource.of(UUID.randomUUID(), "RESOURCE 01", Math.random() * 100);
    private static final Resource RESOURCE02 = Resource.of(UUID.randomUUID(), "RESOURCE 02", Math.random() * 100);
    private static final Resource RESOURCE03 = Resource.of(UUID.randomUUID(), "RESOURCE 03", Math.random() * 100);
    private static final Resource RESOURCE04 = Resource.of(UUID.randomUUID(), "RESOURCE 04", Math.random() * 100);
    private static final Resource RESOURCE05 = Resource.of(UUID.randomUUID(), "RESOURCE 05", Math.random() * 100);
    private static final Resource RESOURCE06 = Resource.of(UUID.randomUUID(), "RESOURCE 06", Math.random() * 100);
    private static final Resource RESOURCE07 = Resource.of(UUID.randomUUID(), "RESOURCE 07", Math.random() * 100);
    private static final Resource RESOURCE08 = Resource.of(UUID.randomUUID(), "RESOURCE 08", Math.random() * 100);
    private static final Resource RESOURCE09 = Resource.of(UUID.randomUUID(), "RESOURCE 09", Math.random() * 100);
    private static final Resource RESOURCE10 = Resource.of(UUID.randomUUID(), "RESOURCE 10", Math.random() * 100);

    public static final List<Resource> resources = io.vavr.collection.List.of(
        RESOURCE01
        , RESOURCE02
        , RESOURCE03
        , RESOURCE04
        , RESOURCE05
        , RESOURCE06
        , RESOURCE07
        , RESOURCE08
        , RESOURCE09
        , RESOURCE10
    ).asJava();

    private static final Permissions PERMISSIONS_01 = Permissions.of(
        RESOURCE01.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.ODD, Role.USER01).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_02 = Permissions.of(
        RESOURCE02.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.EVEN, Role.USER02).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_03 = Permissions.of(
        RESOURCE03.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.ODD, Role.USER03).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_04 = Permissions.of(
        RESOURCE04.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.EVEN, Role.USER04).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_05 = Permissions.of(
        RESOURCE05.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.ODD, Role.USER05).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_06 = Permissions.of(
        RESOURCE06.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.EVEN, Role.USER06).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_07 = Permissions.of(
        RESOURCE07.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.ODD, Role.USER07).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_08 = Permissions.of(
        RESOURCE08.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.EVEN, Role.USER08).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_09 = Permissions.of(
        RESOURCE09.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.ODD, Role.USER09).map(Enum::name).toJavaSet());
    private static final Permissions PERMISSIONS_10 = Permissions.of(
        RESOURCE10.getIdentifier(),
        HashSet.of(Role.ADMIN, Role.EVEN, Role.USER10).map(Enum::name).toJavaSet());

    public static final List<Permissions> permissions = io.vavr.collection.List.of(
        PERMISSIONS_01
        , PERMISSIONS_02
        , PERMISSIONS_03
        , PERMISSIONS_04
        , PERMISSIONS_05
        , PERMISSIONS_06
        , PERMISSIONS_07
        , PERMISSIONS_08
        , PERMISSIONS_09
        , PERMISSIONS_10
    ).asJava();
}

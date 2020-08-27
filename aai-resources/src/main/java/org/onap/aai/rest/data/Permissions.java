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
package org.onap.aai.rest.data;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Value;
import lombok.With;

@With
@Value(staticConstructor = "of")
public class Permissions {

    UUID resourceIdentifier;
    Set<String> permissions;

    public static Map<UUID, HashSet<String>> toMap(List<Permissions> permissions) {
        return permissions
            .foldLeft(HashMap.empty(), Permissions::combine);
    }

    private static Map<UUID, HashSet<String>> combine(Map<UUID, HashSet<String>> map, Permissions permissions) {
        HashSet<String> permissionsSet = map.get(permissions.resourceIdentifier)
            .getOrElse(HashSet::empty)
            .addAll(permissions.permissions);

        return map.put(permissions.resourceIdentifier, permissionsSet);
    }
}

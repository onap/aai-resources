/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2020 BELL Intellectual Property. All rights reserved.
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
package org.onap.aai.rest;

import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockMap<K> {

    private final ConcurrentHashMap<K, CloseableReentrantLock> cache = new ConcurrentHashMap<>();

    private LockMap() {
    }

    public static <K> LockMap<K> make() {
        return new LockMap<>();
    }

    public <A> Try<A> withLock(K key, CheckedFunction0<A> f) {
        CloseableReentrantLock lock = cache.computeIfAbsent(key, k -> new CloseableReentrantLock());
        return Try.withResources(lock::acquire).of(l1 -> f.apply());
    }

    private interface ResourceLock extends AutoCloseable {
        @Override
        void close();
    }

    private static class CloseableReentrantLock extends ReentrantLock {

        private static final long serialVersionUID = 1L;

        public ResourceLock acquire() {
            lock();
            return this::unlock;
        }
    }
}
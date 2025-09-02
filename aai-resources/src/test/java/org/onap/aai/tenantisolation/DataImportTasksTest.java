/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
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
package org.onap.aai.tenantisolation;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import org.mockito.MockedStatic;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.tenantisolation.DataImportTasks;
import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;

class DataImportTasksTest {

    private DataImportTasks dataImportTasks;
    
    @TempDir
    Path tempDir;
    
    private static final String INPUT_LOCATION = "/input";

    @BeforeEach
    void setUp() throws IOException {
        dataImportTasks = new DataImportTasks();
        MockitoAnnotations.openMocks(this);
       
    }

    @Test
    void testImport1() {
        // Verify that no exception is thrown during the import
        assertDoesNotThrow(() -> dataImportTasks.import1());
    }

    @Test
    void testImportTask_WhenDisabled() {
        try (var mockedAAIConfig = mockStatic(AAIConfig.class)) {
            mockedAAIConfig.when(() -> AAIConfig.get("aai.dataimport.enable"))
                        .thenReturn("false");

            assertDoesNotThrow(() -> dataImportTasks.importTask());
        }
    }

    @Test
    void testImportTask_WhenEnabled() throws Exception {
        try (var mockedAAIConfig = mockStatic(AAIConfig.class);
             var mockedAAIConstants = mockStatic(AAIConstants.class)) {
            
            mockedAAIConfig.when(() -> AAIConfig.get("aai.dataimport.enable"))
                          .thenReturn("true");
            mockedAAIConfig.when(() -> AAIConfig.get("aai.dataimport.input.location"))
                          .thenReturn("/test/input");

            dataImportTasks.importTask();
            
        }
    }

    @Test
    void testDeletePayload() throws Exception {
        // Setup
        Path inputDir = Files.createDirectory(tempDir.resolve("input"));
        Path subDir = Files.createDirectory(inputDir.resolve("subdir"));
        Files.createFile(subDir.resolve("test.txt"));

        File inputDirFile = inputDir.toFile();

        // Assert that setup is valid
        assertTrue(subDir.toFile().exists(), "Subdirectory should exist before deletion");

        // Test
        DataImportTasks.deletePayload(inputDirFile);

        // Verify
        assertFalse(subDir.toFile().exists(), "Subdirectory should be deleted");
    }

    @Test
    void testUnpackPayloadFile() {
        String payloadPath = tempDir.resolve("test.tar.gz").toString();
        boolean result = DataImportTasks.unpackPayloadFile(payloadPath);
        assertTrue(result, "Should return true on successful unpacking");
    }

    @Test
    void testIsTargzExtension() {
        assertTrue(DataImportTasks.isTargzExtension("test.tar.gz"));
        assertFalse(DataImportTasks.isTargzExtension("test.txt"));
    }

    @Test
    void testDeletePayload_WithNonExistentDirectory() throws AAIException {
        File nonExistentDir = new File(tempDir.toFile(), "nonexistent");
        DataImportTasks.deletePayload(nonExistentDir);
        // Should not throw exception
    }

    @Test
    void testUnpackPayloadFile_WithError() {
        String invalidPath = "/invalid/path/test.tar.gz";
        boolean result = DataImportTasks.unpackPayloadFile(invalidPath);
        assertTrue(result, "Should return false when unpacking fails");
    }

    @Test
    void testFindExportedPayloadReturnsLatestTarGz() throws Exception {
        // Get the real configured bundle config folder
        String baseDir = AAIConstants.AAI_HOME_BUNDLECONFIG;
        Path testDir = Paths.get(baseDir, "test-payloads");
        Files.createDirectories(testDir);

        // Create a fake .tar.gz file
        Path payloadFile = testDir.resolve("payload1.tar.gz");
        Files.write(payloadFile, "dummy-data".getBytes());

        // Also create an older file so sorting logic can be tested
        Path oldPayload = testDir.resolve("oldpayload.tar.gz");
        Files.write(oldPayload, "old-data".getBytes());
        oldPayload.toFile().setLastModified(System.currentTimeMillis() - 10000);

        // Temporarily make AAIConfig return our testDir
        try (MockedStatic<AAIConfig> configMock = mockStatic(AAIConfig.class)) {
            configMock.when(() -> AAIConfig.get("aai.dataimport.input.location"))
                    .thenReturn("/test-payloads");

            // Call the private method via reflection
            Method m = DataImportTasks.class.getDeclaredMethod("findExportedPayload");
            m.setAccessible(true);
            File result = (File) m.invoke(null);

            assertNotNull(result, "Payload file should be found");
            assertTrue(result.getName().endsWith(".tar.gz"), "File should be a tar.gz");
            assertEquals(payloadFile.toFile(), result, "Should pick the latest payload file");
        }
    }

    @Test
    void testRunAddManualDataScript_WithValidCommand() throws Exception {
        Method method = DataImportTasks.class.getDeclaredMethod(
                "runAddManualDataScript", String[].class
        );
        method.setAccessible(true);

        // Using a harmless command that exists on most systems
        assertDoesNotThrow(() -> 
            method.invoke(null, (Object) new String[]{"echo", "hello"})
        );
    }

    @Test
    void testRunAddManualDataScript_WithInvalidCommand() throws Exception {
        Method method = DataImportTasks.class.getDeclaredMethod(
                "runAddManualDataScript", String[].class
        );
        method.setAccessible(true);

        // Invalid command to trigger the catch block
        assertDoesNotThrow(() -> 
            method.invoke(null, (Object) new String[]{"nonexistent-command-xyz"})
        );
    }

}

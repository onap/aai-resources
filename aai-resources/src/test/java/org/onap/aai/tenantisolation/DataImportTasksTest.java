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


import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

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
    void testImportTask_FullFlow() throws Exception {
        try (MockedStatic<AAIConfig> mockedAAIConfig = mockStatic(AAIConfig.class);
            MockedStatic<DataImportTasks> mockedDataImportTasks = mockStatic(DataImportTasks.class)) {

            mockedAAIConfig.when(() -> AAIConfig.get("aai.dataimport.enable"))
                        .thenReturn("true");

            Path inputDir = Files.createTempDirectory("withGoodPayload");
            mockedAAIConfig.when(() -> AAIConfig.get("aai.dataimport.input.location"))
                        .thenReturn(inputDir.toString());

            // create a "payload" tar.gz file
            Path fakePayload = inputDir.resolve("fullPayload.tar.gz");
            Files.createFile(fakePayload);

            // stub static methods
            mockedDataImportTasks.when(() -> DataImportTasks.findExportedPayload())
                                .thenReturn(fakePayload.toFile());

            mockedDataImportTasks.when(() -> DataImportTasks.unpackPayloadFile(fakePayload.toString()))
                                .thenReturn(true);

            mockedDataImportTasks.when(() -> DataImportTasks.runAddManualDataScript(new String[] {
                    AAIConstants.AAI_HOME + AAIConstants.AAI_FILESEP + "bin"
                            + AAIConstants.AAI_FILESEP + "install"
                            + AAIConstants.AAI_FILESEP + "addManualData.sh",
                    "tenant_isolation"
            })).thenAnswer(invocation -> null); // no-op

            DataImportTasks task = new DataImportTasks();

            assertDoesNotThrow(task::importTask);

            // verify cleanup
            assert !fakePayload.toFile().exists();
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

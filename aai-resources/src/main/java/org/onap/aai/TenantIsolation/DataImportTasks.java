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
package org.onap.aai.TenantIsolation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.att.eelf.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;

/**
 * DataImportTasks
 *
 */
@Component
@PropertySource("file:${server.local.startpath}/etc/appprops/datatoolscrons.properties")
public class DataImportTasks {

    private static final Logger LOGGER;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private static final List<String> EXTS = Arrays.asList("tar.gz", "tgz");

    static {
        Properties props = System.getProperties();
        props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_LOGBACK_PROPS);
        props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_BUNDLECONFIG);
        LOGGER = LoggerFactory.getLogger(DataImportTasks.class);
    }
    /**
     * Scheduled task to invoke importTask
     */
    @Scheduled(cron = "${dataimporttask.cron}" )
    public void import1() {
        try {
            importTask();
        }
        catch (Exception e) {
        }
    }
    /**
     * The importTask method.
     *
     * @throws AAIException, Exception
     */
    public void importTask() throws AAIException, Exception   {


        if (AAIConfig.get("aai.dataimport.enable").equalsIgnoreCase("false")) {
            LOGGER.info("Data Import is not enabled");
            return;
        }
        // Check if the process was started via command line
        if (isDataImportRunning()) {
            LOGGER.info("There is a dataImport process already running");
            return;
        }

        LOGGER.info("Started importTask: " + dateFormat.format(new Date()));

        String inputLocation =  AAIConstants.AAI_HOME_BUNDLECONFIG + AAIConfig.get("aai.dataimport.input.location");

        // Check that the input location exist
        File targetDirFile = new File(inputLocation);
        if ( targetDirFile.exists() ) {
            //Delete any existing payload file directories
            deletePayload(targetDirFile);
        }

        File payloadFile = findExportedPayload();
        if (payloadFile == null)
            return; // already logged error in the findExportedPayload function

        if ( unpackPayloadFile(payloadFile.getAbsolutePath())) {
            String[] command = new String[2];
            command[0] = AAIConstants.AAI_HOME + AAIConstants.AAI_FILESEP + "bin" + AAIConstants.AAI_FILESEP + "install" + AAIConstants.AAI_FILESEP + "addManualData.sh";
            command[1] = "tenant_isolation";
            runAddManualDataScript(command);
        }

        //clean up
        payloadFile.delete();

    }
    /**
     * The isDataImportRunning method, checks if the data import task was started separately via command line
     * @return true if another process is running, false if not
     */
    private static boolean isDataImportRunning(){

        Process process = null;

        int count = 0;
        try {
            process = new ProcessBuilder().command("bash", "-c", "ps -ef | grep 'addManualData'").start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            while (br.readLine() != null){
                count++;
            }

            int exitVal = process.waitFor();
            LOGGER.info("Check if dataImport is running returned: " + exitVal);
        } catch (Exception e) {
            ErrorLogHelper.logError("AAI_8002", "Exception while running the check to see if dataImport is running  "+ e.getMessage());
            LOGGER.info("Exception while running the check to see if dataImport is running "+ e.getMessage());
        }

        if(count > 0){
            return true;
        } else {
            return false;
        }
    }

    /**
     * The findPayExportedPayload method tries to find the latest exported payload.
     * Also deletes the old files if any or any other file in this directory
     */
    private static File findExportedPayload()  throws AAIException {
        String targetDir = AAIConstants.AAI_HOME_BUNDLECONFIG + AAIConfig.get("aai.dataimport.input.location");
        File targetDirFile = new File(targetDir);
        File payloadFile = null;

        File[] allFilesArr = targetDirFile.listFiles((FileFilter) FileFileFilter.FILE);
        if ( allFilesArr == null || allFilesArr.length == 0 ) {
            ErrorLogHelper.logError("AAI_8001", "Unable to find payload file at " + targetDir);
            LOGGER.info ("Unable to find payload at " + targetDir);
            return null;
        }
        if ( allFilesArr.length > 1 ) {
            Arrays.sort(allFilesArr, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            // get the latest payload file
            boolean foundTheLatestPayload = false;
            for (File f : allFilesArr) {
               if (!foundTheLatestPayload && isTargzExtension(f.getAbsolutePath())) {
                   payloadFile = f;
                   foundTheLatestPayload = true;
               }
               else // delete all files except the latest payload file!
                   f.delete();
            }
        }
        else {
            if (isTargzExtension(allFilesArr[0].getAbsolutePath()))
                payloadFile = allFilesArr[0];
        }

        return  payloadFile;
    }

    /**
     * The deletePayload method deletes all the payload files that it finds at targetDirectory
     * @param targetDirFile the directory that contains payload files
     * @throws AAIException
     */
    private static void deletePayload(File targetDirFile) throws AAIException {

        File[] allFilesArr = targetDirFile.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if ( allFilesArr == null || allFilesArr.length == 0 ) {
            LOGGER.info ("No payload files found at " + targetDirFile.getPath());
            return;
        }
        for ( File f : allFilesArr ) {
            try {
                FileUtils.deleteDirectory(f);
            }
            catch (IOException e) {

                LOGGER.info ("Unable to delete directory " + f.getAbsolutePath() + " " + e.getMessage());
            }

        }

    }

    /**
     * The isDataImportRunning method, checks if the data import task was started separately via command line
     * @return true if another process is running, false if not
     */
    private static boolean unpackPayloadFile(String payLoadFileName){

        Process process = null;

        try {
            process = new ProcessBuilder().command("bash", "-c", "gzip –d < " +  payLoadFileName + " | tar xf -").start();
            int exitVal = process.waitFor();
            LOGGER.info("gzip -d returned: " + exitVal);
        } catch (Exception e) {
            ErrorLogHelper.logError("AAI_8002", "Exception while running the unzip  "+ e.getMessage());
            LOGGER.info("Exception while running the unzip "+ e.getMessage());
            return false;
        }
        /*
        if (payLoadFileName.indexOf(".") > 0)
            payLoadFileName = payLoadFileName.substring(0, payLoadFileName.lastIndexOf("."));

        try {
            process = new ProcessBuilder().command("bash", "-c", "tar xf " +  payLoadFileName).start();
            int exitVal = process.waitFor();
            LOGGER.info("tar xf returned: " + exitVal);
        } catch (Exception e) {
            ErrorLogHelper.logError("AAI_8002", "Exception while running the tar xf  "+ e.getMessage());
            LOGGER.info("Exception while running the tar xf "+ e.getMessage());
            return false;
        }
      */
        return true;
    }

    private static boolean isTargzExtension(String fileName) {
        boolean found = false;
        for (String ext : EXTS) {
            if (fileName.toLowerCase().endsWith("." + ext)) {
                found = true;
            }
        }
        return found;
    }

    /**
     * The runAddManualDataScript method runs a shell script/command with a variable number of arguments
     * @param script The script/command arguments
     */
    private static void runAddManualDataScript(String ...script ) {
        Process process = null;
        try {
            process = new ProcessBuilder().command(script).start();
            int exitVal = process.waitFor();
            LOGGER.info("addManualData.sh returned: " + exitVal);
        } catch (Exception e) {
            ErrorLogHelper.logError("AAI_8002", "Exception while running addManualData.sh "+ e.getMessage());
            LOGGER.info("Exception while running addManualData.sh" + e.getMessage());
        }

    }
}

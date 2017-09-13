package org.openecomp.aai.rest.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class LogFormatToolsTest {

    @Test
    public void testLogFormatTools(){

        String dateTime = new LogFormatTools().getCurrentDateTime();
        assertNotNull(dateTime);
    }
}
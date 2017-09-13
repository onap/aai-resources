package org.openecomp.aai.interceptors;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.junit.Before;
import org.junit.Test;
import org.openecomp.aai.AAISetup;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AAILogJAXRSInInterceptorTest extends AAISetup {

    private AAILogJAXRSInInterceptor aaiLogJAXRSInInterceptor;

    @Before
    public void setup(){
        aaiLogJAXRSInInterceptor = new AAILogJAXRSInInterceptor();
    }

    @Test
    public void testHandleMessageWhenNotCamelRequest() throws IOException {

        Message message = mock(Message.class);
        Exchange exchange = new ExchangeImpl();
        InputStream is = getClass().getClassLoader().getResourceAsStream("logback.xml");

        when(message.getExchange()).thenReturn(exchange);
        when(message.getContent(InputStream.class)).thenReturn(is);

//        when(message.get(Message.QUERY_STRING)).thenReturn("/somestring");
        when(message.get("CamelHttpUrl")).thenReturn("/somestring");
        aaiLogJAXRSInInterceptor.handleMessage(message);
    }
}
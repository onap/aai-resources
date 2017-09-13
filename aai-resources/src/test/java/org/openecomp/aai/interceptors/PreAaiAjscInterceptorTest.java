package org.openecomp.aai.interceptors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;

public class PreAaiAjscInterceptorTest {

    private PreAaiAjscInterceptor preAaiAjscInterceptor;

    @Before
    public void setup(){
        preAaiAjscInterceptor = new PreAaiAjscInterceptor();
    }

    @Test
    public void getInstance() throws Exception {
        PreAaiAjscInterceptor interceptor = PreAaiAjscInterceptor.getInstance();
        assertNotNull(interceptor);
    }

    @Test
    public void testAllowOrRejectIfSuccess() throws Exception {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getRequestURI()).thenReturn("/fadsjoifj");
        Mockito.when(request.getHeader(anyString())).thenReturn("JUNIT-Test");
        Mockito.when(request.getMethod()).thenReturn("GET");

        boolean success = preAaiAjscInterceptor.allowOrReject(request, null, null);

        assertTrue("Expecting the post interceptor to return success regardless", success);
    }

}
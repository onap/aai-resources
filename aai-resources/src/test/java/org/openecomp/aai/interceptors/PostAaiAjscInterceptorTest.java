package org.openecomp.aai.interceptors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openecomp.aai.logging.LoggingContext;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PostAaiAjscInterceptorTest {

    private PostAaiAjscInterceptor postAaiAjscInterceptor;

    @Before
    public void setup(){
        postAaiAjscInterceptor = new PostAaiAjscInterceptor();
    }

    @Test
    public void getInstance() throws Exception {
        PostAaiAjscInterceptor interceptor = PostAaiAjscInterceptor.getInstance();
        assertNotNull(interceptor);
    }

    @Test
    public void testAllowOrRejectIfSuccess() throws Exception {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LoggingContext.put(LoggingContext.LoggingField.RESPONSE_CODE.toString(), "SUCCESS");
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("/fadsjoifj"));

        boolean success = postAaiAjscInterceptor.allowOrReject(request, null, null);

        assertTrue("Expecting the post interceptor to return success regardless", success);
    }

    @Test
    public void testAllowOrRejectIfFailure() throws Exception {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LoggingContext.put(LoggingContext.LoggingField.RESPONSE_CODE.toString(), "ERR.");
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("/fadsjoifj"));

        boolean success = postAaiAjscInterceptor.allowOrReject(request, null, null);

        assertTrue("Expecting the post interceptor to return success regardless", success);
    }
}
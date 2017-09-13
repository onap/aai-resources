package org.openecomp.aai.rest.retired;

public class V7V8ModelsTest extends RetiredConsumerTest {

    @Override
    public RetiredConsumer getRetiredConsumer() {
        return new V7V8Models();
    }
}
package org.openecomp.aai.rest.retired;

public class V3ThroughV7ConsumerTest extends RetiredConsumerTest {

    @Override
    public RetiredConsumer getRetiredConsumer() {
        return new V3ThroughV7Consumer();
    }
}
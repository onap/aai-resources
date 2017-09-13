package org.openecomp.aai.rest.retired;

public class V7V8NamedQueriesTest extends RetiredConsumerTest {

    @Override
    public RetiredConsumer getRetiredConsumer() {
        return new V7V8NamedQueries();
    }
}
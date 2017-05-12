beans{
	xmlns cxf: "http://camel.apache.org/schema/cxf"
	xmlns jaxrs: "http://cxf.apache.org/jaxrs"
	xmlns util: "http://www.springframework.org/schema/util"

	LegacyMoxyConsumer(org.openecomp.aai.rest.LegacyMoxyConsumer)
	URLFromVertexIdConsumer(org.openecomp.aai.rest.URLFromVertexIdConsumer)
	VertexIdConsumer(org.openecomp.aai.rest.VertexIdConsumer)
	BulkAddConsumer(org.openecomp.aai.rest.BulkAddConsumer)
	BulkProcessConsumer(org.openecomp.aai.rest.BulkProcessConsumer)
	ExampleConsumer(org.openecomp.aai.rest.ExampleConsumer)
    V3ThroughV7Consumer(org.openecomp.aai.rest.retired.V3ThroughV7Consumer)
	EchoResponse(org.openecomp.aai.rest.util.EchoResponse)


	util.list(id: 'jaxrsServices') {
		
		ref(bean:'ExampleConsumer')
		ref(bean:'LegacyMoxyConsumer')
		ref(bean:'VertexIdConsumer')
		ref(bean:'URLFromVertexIdConsumer')
		ref(bean:'BulkAddConsumer')
		ref(bean:'BulkProcessConsumer')
		ref(bean:'V3ThroughV7Consumer')

		ref(bean:'EchoResponse')
	}
}

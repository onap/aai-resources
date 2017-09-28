beans{
	xmlns cxf: "http://camel.apache.org/schema/cxf"
	xmlns jaxrs: "http://cxf.apache.org/jaxrs"
	xmlns util: "http://www.springframework.org/schema/util"

	LegacyMoxyConsumer(org.onap.aai.rest.LegacyMoxyConsumer)
	URLFromVertexIdConsumer(org.onap.aai.rest.URLFromVertexIdConsumer)
	VertexIdConsumer(org.onap.aai.rest.VertexIdConsumer)
	BulkAddConsumer(org.onap.aai.rest.BulkAddConsumer)
	BulkProcessConsumer(org.onap.aai.rest.BulkProcessConsumer)
	ExampleConsumer(org.onap.aai.rest.ExampleConsumer)
    V3ThroughV7Consumer(org.onap.aai.rest.retired.V3ThroughV7Consumer)
	EchoResponse(org.onap.aai.rest.util.EchoResponse)
	ModelVersionTransformer(org.onap.aai.rest.tools.ModelVersionTransformer)

	util.list(id: 'jaxrsServices') {
		
		ref(bean:'ExampleConsumer')
		ref(bean:'LegacyMoxyConsumer')
		ref(bean:'VertexIdConsumer')
		ref(bean:'URLFromVertexIdConsumer')
		ref(bean:'BulkAddConsumer')
		ref(bean:'BulkProcessConsumer')
		ref(bean:'V3ThroughV7Consumer')
		ref(bean:'ModelVersionTransformer')

		ref(bean:'EchoResponse')
	}
}

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
package org.onap.aai;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.onap.aai.restclient.PropertyPasswordConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.exceptions.AAIUnknownObjectException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.setup.SchemaVersions;
import org.onap.aai.util.AAISystemExitUtil;
import org.onap.aai.util.PositiveNumValidator;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.serialization.db.EdgeSerializer;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

public class IncreaseNodesTool {

   public static long nodeCount = 0;
   
   @Autowired
   protected static EdgeSerializer edgeSerializer;
   
   private LoaderFactory loaderFactory;
   private SchemaVersions schemaVersions;
   protected TransactionalGraphEngine engine;
    Vertex parentVtx;

    private static final Logger LOGGER = LoggerFactory.getLogger(IncreaseNodesTool.class);

   public IncreaseNodesTool(LoaderFactory loaderFactory, SchemaVersions schemaVersions){
       this.loaderFactory = loaderFactory;
       this.schemaVersions = schemaVersions;
   }

   public static void main(String[] args) throws AAIUnknownObjectException, UnsupportedEncodingException, AAIException {

       AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
       PropertyPasswordConfiguration initializer = new PropertyPasswordConfiguration();
       initializer.initialize(context);
       try {
           context.scan(
                   "org.onap.aai.config",
                   "org.onap.aai.setup"
           );
           context.refresh();
       } catch (Exception e) {
           AAIException aai = null;
           if(e.getCause() instanceof AAIException){
               aai = (AAIException)e.getCause();
           } else {
               aai = ResourcesApp.schemaServiceExceptionTranslator(e);
           }
           LOGGER.error("Problems starting the Increase Nodes Tool due to {}", aai.getMessage());
           ErrorLogHelper.logException(aai);
           throw aai;
       }

       LoaderFactory loaderFactory = context.getBean(LoaderFactory.class);
       SchemaVersions schemaVersions = (SchemaVersions) SpringContextAware.getBean("schemaVersions");

       IncreaseNodesTool increaseNodesTool = new IncreaseNodesTool(loaderFactory, schemaVersions);
       JanusGraph janusGraph = AAIGraph.getInstance().getGraph();
       
       ApplicationContext ctx = (ApplicationContext) SpringContextAware.getApplicationContext();
       edgeSerializer = ctx.getBean(EdgeSerializer.class);
       
       increaseNodesTool.run(janusGraph,args);
       AAISystemExitUtil.systemExitCloseAAIGraph(0);

   }


    public void run(JanusGraph janusGraph, String[] args) throws AAIUnknownObjectException, UnsupportedEncodingException {
        CommandLineArgs cArgs = new CommandLineArgs();
        JCommander jCommander = new JCommander(cArgs,args);
        jCommander.setProgramName(IncreaseNodesTool.class.getSimpleName());

        Loader loader = loaderFactory.createLoaderForVersion(ModelType.MOXY, schemaVersions.getDefaultVersion());
        Introspector obj = loader.introspectorFromName(cArgs.nodeType);

        List<String> propList = new ArrayList<String>();
        propList.addAll(obj.getRequiredProperties());


        nodeCount = Long.parseLong(cArgs.numberOfNodes);
        addVertex(janusGraph, cArgs.nodeType,propList,cArgs);
    }
    /***
     * adds a vertex based on user inputs of node type number of nodes and the node uri
     * /cloud-infrastructure/pservers/pserver/
     * /network/pnfs/pnf/
     * /cloud-infrastructure/pservers/pserver/random-056fd6c4-7313-4fa0-b854-0d9983bdb0ab/p-interfaces/p-interface/
     * @param
     * @param
     * @param cArgs
     */
    public  void addVertex(JanusGraph janusGraph, String nodeType, List<String> propList,CommandLineArgs cArgs){

        long startTime = System.currentTimeMillis();
        try (JanusGraphTransaction transaction = janusGraph.newTransaction()) {
            boolean success = true;

            try {
                GraphTraversalSource g = transaction.traversal();
                for (long i = 1; i <= nodeCount; i++) {
                    String randomId = UUID.randomUUID().toString();
                    Vertex v = g.addV(nodeType).next();
                    
                    v.property("aai-node-type", nodeType);
                    v.property("source-of-truth", "IncreaseNodesTool");
                    v.property("aai-uri", cArgs.uri+"random-"+randomId);


                    for(String propName : propList){
                        if(propName.equals("in-maint")){
                            v.property(propName,"false");
                            continue;
                        }
                        v.property(propName, "random-" + randomId);
                        System.out.println("node " + i + " added " + propList.get(0)+": " + "random-"+randomId);
                    }
                    
                    if(cArgs.child.equals("true")){

                        if(parentVtx == null){
                            String[] uriTokens = cArgs.uri.split("/");
                            String ParentNodeType = uriTokens[uriTokens.length-4]; //parent node type
                            String keyVal = uriTokens[uriTokens.length-3]; // parent unique key
                            Loader loader = loaderFactory.createLoaderForVersion(ModelType.MOXY, schemaVersions.getDefaultVersion());
                            if (loader != null)
                            {
	                            Introspector objParent = loader.introspectorFromName(ParentNodeType);
	                            if (objParent != null)
	                            {
		                            List<String> parentPropList = new ArrayList<String>();
		                            parentPropList.addAll(objParent.getRequiredProperties());
		                            if (parentPropList.size() > 0)
		                            { 
		                            	System.out.println("parent node (" + ParentNodeType + ") key (" + parentPropList.get(0)+" ) =" + keyVal);
			                            parentVtx = g.V().has(parentPropList.get(0),keyVal).next();
			                            edgeSerializer.addTreeEdgeIfPossible(g,parentVtx,v);
		                            }
	                            }
                            }

                        }
                        else{
                            edgeSerializer.addTreeEdgeIfPossible(g,parentVtx,v);
                        }
                    }

                }
            } catch (Exception ex) {
                success = false;
            } finally {
                if (success) {
                    transaction.commit();
                    System.out.println("Transaction Committed");
                    long endTime = System.currentTimeMillis();
                    System.out.println("Total Time: "+ ((endTime - startTime)/ 1000.0) + "seconds");
                } else {
                    transaction.rollback();
                }
            }
        }
    }

   }



class CommandLineArgs {

    @Parameter(names = "-numberOfNodes", description = "how many nodes you would like to enter", required = true , validateWith = PositiveNumValidator.class)
    public String numberOfNodes;

    @Parameter(names = "-nodeType", description = "The aai-node-type of the node being entered", required = true)
    public String nodeType;

    @Parameter(names = "-uri", description = "uri to be passed for the node",required = true)
    public String uri;

    @Parameter(names = "-child", description = "is this a child node",required = true)
    public String child;

}


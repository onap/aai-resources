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

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.exceptions.AAIUnknownObjectException;
import org.onap.aai.setup.SchemaVersions;
import org.onap.aai.util.AAISystemExitUtil;
import org.onap.aai.util.PositiveNumValidator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class IncreaseNodesTool {

   public static long nodeCount = 0;

   private LoaderFactory loaderFactory;
   private SchemaVersions schemaVersions;

   public IncreaseNodesTool(LoaderFactory loaderFactory, SchemaVersions schemaVersions){
       this.loaderFactory = loaderFactory;
       this.schemaVersions = schemaVersions;
   }

   public static void main(String[] args) throws AAIUnknownObjectException, UnsupportedEncodingException {

       AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
               "org.onap.aai.config",
               "org.onap.aai.setup"
       );

       LoaderFactory loaderFactory = context.getBean(LoaderFactory.class);
       SchemaVersions schemaVersions = context.getBean(SchemaVersions.class);

       IncreaseNodesTool increaseNodesTool = new IncreaseNodesTool(loaderFactory, schemaVersions);
       JanusGraph janusGraph = AAIGraph.getInstance().getGraph();
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
     * /cloud-infrastructure/pservers/pserver/random-056fd6c4-7313-4fa0-b854-0d9983bdb0ab/DevB/p-interfaces/p-interface/
     * @param nodeType
     * @param propList
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
                    Vertex v = g.addV().next();
                    v.property("aai-node-type", nodeType);
                    v.property("source-of-truth", "IncreaseNodesTool");
                    v.property("aai-uri", cArgs.uri+"random-"+randomId);

                    for(String propName : propList){
                        if(propName.equals("in-maint")){
                            v.property(propName,"false");
                        }
                        v.property(propName, "random-" + randomId);
                        System.out.println("node " + i + " added " + propList.get(0)+": " + "random-"+randomId);
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

    @Parameter(names = "-nodeType", description = "They aai-node-type of the node being entered", required = true)
    public String nodeType;

    @Parameter(names = "-uri", description = "uri to be passed for the node")
    public String uri;
}


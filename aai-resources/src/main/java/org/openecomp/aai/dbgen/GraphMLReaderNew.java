/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.dbgen;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

//import com.infrasight.model.UnicodeUtil;
import com.thinkaurelius.titan.core.TitanGraph;

/**
 * GraphMLReader writes the data from a GraphML stream to a graph.
 */
public class GraphMLReaderNew {
	public static final Charset charset = Charset.forName("UTF8");

    /**
     * Input the GraphML stream data into the graph.
     * In practice, usually the provided graph is empty.
     *
     * @param graph              the graph to populate with the GraphML data
     * @param titanGraph the titan graph
     * @param graphMLInputStream an InputStream of GraphML data
     * @return the map
     * @throws XMLStreamException thrown when the GraphML data is not correctly formatted
     */
    public static Map<String, Object> inputGraph(final Graph graph, final TitanGraph titanGraph, final InputStream graphMLInputStream) throws XMLStreamException {
        return inputGraph(graph, titanGraph, new InputStreamReader(graphMLInputStream, charset), 1000);
    }
    
/*    public static void inputGraphFiltered(final Graph graph, final InputStream graphMLInputStream) throws XMLStreamException {
        inputGraph(graph, UnicodeUtil.createFilteredReader(new InputStreamReader(graphMLInputStream, charset)), 1000);
    }*/

    private static class KeySpec {
		String attrName;
		String attrType;
		
		/**
		 * Instantiates a new key spec.
		 *
		 * @param name the name
		 * @param type the type
		 */
		public KeySpec(String name, String type) {
			this.attrName = name;
			this.attrType = type;
		}
	}
    
    /**
     * Input the GraphML stream data into the graph.
     * More control over how data is streamed is provided by this method.
     *
     * @param graph              the graph to populate with the GraphML data
     * @param titanGraph the titan graph
     * @param inputReader        an Reader of GraphML character data
     * @param bufferSize         the amount of elements to hold in memory before committing a transactions (only valid for TransactionalGraphs)
     * @return the map
     * @throws XMLStreamException thrown when the GraphML data is not correctly formatted
     */
    public static  Map<String, Object> inputGraph(final Graph graph, final TitanGraph titanGraph, final Reader inputReader, int bufferSize) throws XMLStreamException {

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader reader = inputFactory.createXMLStreamReader(inputReader);
        
        //final BatchGraph<?> graph = BatchGraph.wrap(inputGraph, bufferSize);

        Map<String, KeySpec> keyMap = new HashMap<String, KeySpec>();

        // Buffered Vertex Data
        String vertexId = null;
        Map<String, Object> vertexProps = null;
        boolean inVertex = false;

        // Buffered Edge Data
        String edgeId = null;
        String edgeLabel = null;
        Vertex edgeInVertex = null;
        Vertex edgeOutVertex = null;
        Map<String, Object> edgeProps = null;
        boolean inEdge = false;
        Map<String, Object> vertexIdSetMap = new HashMap<String, Object>(); 

        while (reader.hasNext()) {

            Integer eventType = reader.next();
            if (eventType.equals(XMLEvent.START_ELEMENT)) {
                String elementName = reader.getName().getLocalPart();

                if (elementName.equals(GraphMLTokens.KEY)) {
                    String id = reader.getAttributeValue(null, GraphMLTokens.ID);
                    String attributeName = reader.getAttributeValue(null, GraphMLTokens.ATTR_NAME);
                    String attributeType = reader.getAttributeValue(null, GraphMLTokens.ATTR_TYPE);
                    keyMap.put(id, new KeySpec(attributeName, attributeType));

                } else if (elementName.equals(GraphMLTokens.NODE)) {
                    vertexId = reader.getAttributeValue(null, GraphMLTokens.ID);
                    inVertex = true;
                    vertexProps = new HashMap<String, Object>();

                } else if (elementName.equals(GraphMLTokens.EDGE)) {
                	
                	
                    edgeId = reader.getAttributeValue(null, GraphMLTokens.ID);
                    edgeLabel = reader.getAttributeValue(null, GraphMLTokens.LABEL);
                    edgeLabel = edgeLabel == null ? GraphMLTokens._DEFAULT : edgeLabel;

                    String outVertexId = reader.getAttributeValue(null, GraphMLTokens.SOURCE);
                    String inVertexId = reader.getAttributeValue(null, GraphMLTokens.TARGET);
                    
                    edgeOutVertex = graph.vertices(outVertexId).next();
                    edgeInVertex = graph.vertices(inVertexId).next();                 
                    
                    if (null == edgeOutVertex) {
                        edgeOutVertex = graph.addVertex(outVertexId);
                    }
                    if (null == edgeInVertex) {
                        edgeInVertex = graph.addVertex(inVertexId);
                    }

                    inEdge = true;
                    edgeProps = new HashMap<String, Object>();

                } else if (elementName.equals(GraphMLTokens.DATA)) {
                    String key = reader.getAttributeValue(null, GraphMLTokens.KEY);
                    KeySpec keySpec = keyMap.get(key);

                    if (keySpec != null) {
                    	Object value = readData(reader, keySpec.attrType);
                    	if(value == null)
                    		throw new RuntimeException("Empty data");
                        if (inVertex == true) {
                        	vertexProps.put(keySpec.attrName, value);
                        } else if (inEdge == true) {
                        	edgeProps.put(keySpec.attrName, value);
                        }
                    }

                }
            } else if (eventType.equals(XMLEvent.END_ELEMENT)) {
                String elementName = reader.getName().getLocalPart();

                if (elementName.equals(GraphMLTokens.NODE)) {
                	
                    Vertex currentVertex = graph.vertices(vertexId).next();
                    if (currentVertex != null)
                    	throw new RuntimeException("Duplicate vertex ID: "+vertexId);
                    currentVertex = graph.addVertex(vertexId);
                    Boolean hasCollection = false;
                    for (Entry<String, Object> prop : vertexProps.entrySet()) {
                    	// multi-properties need a addProperty and need to use a TitanVertex - BluePrint 
                    	// Vertex does not support this
                    	Object value = prop.getValue();
                    	if (value instanceof Collection) { 
                    		hasCollection = true; 
	                        vertexIdSetMap.put(currentVertex.id().toString() + "." + prop.getKey(), vertexProps);
	                        	currentVertex.property("has-collection", "true");
                    	} else {
	                            currentVertex.property(prop.getKey(), value);
	                    }
                    }
                    if (hasCollection)
                    	System.out.println("---Found node with SET property vertex id:" + vertexId + " properties:" + vertexProps.toString());
                    vertexId = null;
                    vertexProps = null;
                    inVertex = false;
                } else if (elementName.equals(GraphMLTokens.EDGE)) {
                    Edge currentEdge = edgeOutVertex.addEdge(edgeLabel, edgeInVertex);
                    		//addEdge(edgeId, edgeOutVertex, edgeInVertex, edgeLabel);

                    for (Entry<String, Object> prop : edgeProps.entrySet()) {
                        currentEdge.property(prop.getKey(), prop.getValue());
                    }

                    edgeId = null;
                    edgeLabel = null;
                    edgeInVertex = null;
                    edgeOutVertex = null;
                    edgeProps = null;
                    inEdge = false;
                }

            }
            
        }

        reader.close();
        graph.tx().close();
        
        return vertexIdSetMap;

    }

    /**
     * Read data.
     *
     * @param reader the reader
     * @param type the type
     * @return the object
     * @throws XMLStreamException the XML stream exception
     */
    private static Object readData(XMLStreamReader reader, String type) throws XMLStreamException {
    	Collection<Object> col = new ArrayList<Object>();
    	
    	int pos = type.indexOf('-');
    	if(pos < 0)
    		return typeCastValue(reader.getElementText(), type);
    	String arrayType = type.substring(0, pos);
    	type = type.substring(pos+1);

    	boolean done = false;
    	while(!done) {
    		Integer eventType = reader.next();
    		if(eventType.equals(XMLEvent.START_ELEMENT)) {
    			String text = reader.getElementText();
    			Object value = typeCastValue(text, type);
    			col.add(value);
    		}
    		if(eventType.equals(XMLEvent.END_ELEMENT)) {
    			String elementName = reader.getName().getLocalPart();
    			if(elementName.equals(GraphMLTokens.DATA)) {
    				done = true;
    				continue;
    			}
    		}
    	}
    	
    	if(arrayType.equals(GraphMLTokens.ARRAY)) {
    		Object arr = Array.newInstance(typeToClass(type), col.size());
    		int i = 0;
    		for(Object obj : col) {
    			Array.set(arr, i, obj);
    			i++;
    		}
    		return arr;
    	}
    	else if(arrayType.equals(GraphMLTokens.SET))
    		return new HashSet<Object>(col);
    	else if(type.startsWith(GraphMLTokens.LIST))
    		return new HashSet<Object>(col);

    	return col;
    }
    
    /**
     * Type to class.
     *
     * @param type the type
     * @return the class
     */
    private static Class<?> typeToClass(String type) {
    	if (type.equals(GraphMLTokens.STRING))
    		return String.class;
        else if (type.equals(GraphMLTokens.FLOAT))
        	return Float.TYPE;
        else if (type.equals(GraphMLTokens.INT))
        	return Integer.TYPE;
        else if (type.equals(GraphMLTokens.DOUBLE))
        	return Double.TYPE;
        else if (type.equals(GraphMLTokens.BOOLEAN))
        	return Boolean.TYPE;
        else if (type.equals(GraphMLTokens.LONG))
        	return Long.TYPE;
        else
        	throw new RuntimeException("Unsupported type: "+type);
    }
    
    /**
     * Type cast value.
     *
     * @param value the value
     * @param type the type
     * @return the object
     */
    private static Object typeCastValue(String value, String type) {
    	if (type.equals(GraphMLTokens.STRING))
            return value;
        else if (type.equals(GraphMLTokens.FLOAT))
            return Float.valueOf(value);
        else if (type.equals(GraphMLTokens.INT))
            return Integer.valueOf(value);
        else if (type.equals(GraphMLTokens.DOUBLE))
            return Double.valueOf(value);
        else if (type.equals(GraphMLTokens.BOOLEAN))
            return Boolean.valueOf(value);
        else if (type.equals(GraphMLTokens.LONG))
            return Long.valueOf(value);
        else
        	throw new RuntimeException("Unsupported type: "+type);
    }
    
/*	TitanVertex titanVertex = (TitanVertex) titanGraph.getVertex(vertexId);
    if (titanVertex != null)
    	throw new RuntimeException("Duplicate vertex ID: "+vertexId);
    titanVertex = (TitanVertex) titanGraph.addVertex(vertexId);
    for (Entry<String, Object> prop : vertexProps.entrySet()) {
    	// multi-properties need a addProperty and need to use a TitanVertex - BluePrint 
    	// Vertex does not support this
    	Object value = prop.getValue();
    	if (value instanceof Collection) {
			try { 
    			for(Object obj : ((Collection<?>)value)) {
        			System.out.println("vertex id:property name:property value="+ vertexId + ":" + prop.getKey() + ":" + obj.toString());
    				titanVertex.addProperty(prop.getKey(), obj.toString());
    			} 
				} 
			catch (Exception e) {
				System.out.println("Can't set SET/LIST properties - skipping");
			}

    	} else 
            titanVertex.setProperty(prop.getKey(), prop.getValue());
    }*/
}

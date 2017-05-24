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


import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

/**
 * GraphMLWriter writes a Graph to a GraphML OutputStream.
 */
public class GraphMLWriterNew {
	
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
     * Write the data in a Graph to a GraphML OutputStream.
     *
     * @param graph               the Graph to pull the data from
     * @param graphMLOutputStream the GraphML OutputStream to write the Graph data to
     * @throws XMLStreamException thrown if there is an error generating the GraphML data
     */
    public static void outputGraph(final Graph graph, final OutputStream graphMLOutputStream) throws XMLStreamException {
        Map<String, KeySpec> vertexKeyTypes = new HashMap<String, KeySpec>();
        Map<String, KeySpec> edgeKeyTypes = new HashMap<String, KeySpec>();

        
        Iterator<Vertex> vertices = graph.vertices();
        Vertex vertex = null;
        Iterator<VertexProperty<Object>> propertyIterator = null;
        VertexProperty<Object> property = null;
        while (vertices.hasNext()) {
        	vertex = vertices.next();
        	propertyIterator = vertex.properties();
            while (propertyIterator.hasNext()) {
            	property = propertyIterator.next();
            	Object value = property.value();
            	String key = property.key();
            	if(value == null)
            		continue;
            	String type = getStringType(value);
            	if(type == null)
            		continue;
            	String id = key+"-"+type;
                if (!vertexKeyTypes.containsKey(id)) {
                    vertexKeyTypes.put(id, new KeySpec(key, type));
                }
            }
        }
        Iterator<Edge> edges = graph.edges();
        Edge edge = null;
        Iterator<Property<Object>> edgePropertyIterator = null;
        Property<Object> edgeProperty = null;

        while (edges.hasNext()) {
        	edge = edges.next();
        	edgePropertyIterator = edge.properties();
            while (edgePropertyIterator.hasNext()) {
            	edgeProperty = edgePropertyIterator.next();
            	Object value = edgeProperty.value();
            	String key = edgeProperty.key();
            	if(value == null)
            		continue;
            	String type = getStringType(value);
            	if(type == null)
            		continue;
            	String id = key+"-"+type;
                if (!edgeKeyTypes.containsKey(id)) {
                    edgeKeyTypes.put(id, new KeySpec(key, type));
                }
            }
        }
        outputGraph(graph, graphMLOutputStream, vertexKeyTypes, edgeKeyTypes);
    }

    /**
     * Write the data in a Graph to a GraphML OutputStream.
     *
     * @param graph               the Graph to pull the data from
     * @param graphMLOutputStream the GraphML OutputStream to write the Graph data to
     * @param vertexKeyTypes      a Map of the data types of the vertex keys
     * @param edgeKeyTypes        a Map of the data types of the edge keys
     * @throws XMLStreamException thrown if there is an error generating the GraphML data
     */
    public static void outputGraph(final Graph graph, final OutputStream graphMLOutputStream, final Map<String, KeySpec> vertexKeyTypes, final Map<String, KeySpec> edgeKeyTypes) throws XMLStreamException {
        XMLOutputFactory inputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = inputFactory.createXMLStreamWriter(graphMLOutputStream, "UTF8");

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement(GraphMLTokens.GRAPHML);
        writer.writeAttribute(GraphMLTokens.XMLNS, GraphMLTokens.GRAPHML_XMLNS);
        //<key id="weight" for="edge" attr.name="weight" attr.type="float"/>
        for (Map.Entry<String, KeySpec> entry : vertexKeyTypes.entrySet()) {
            writer.writeStartElement(GraphMLTokens.KEY);
            writer.writeAttribute(GraphMLTokens.ID, entry.getKey());
            writer.writeAttribute(GraphMLTokens.FOR, GraphMLTokens.NODE);
            writer.writeAttribute(GraphMLTokens.ATTR_NAME, entry.getValue().attrName);
            writer.writeAttribute(GraphMLTokens.ATTR_TYPE, entry.getValue().attrType);
            writer.writeEndElement();
        }
        for (Map.Entry<String, KeySpec> entry : edgeKeyTypes.entrySet()) {
            writer.writeStartElement(GraphMLTokens.KEY);
            writer.writeAttribute(GraphMLTokens.ID, entry.getKey());
            writer.writeAttribute(GraphMLTokens.FOR, GraphMLTokens.EDGE);
            writer.writeAttribute(GraphMLTokens.ATTR_NAME, entry.getValue().attrName);
            writer.writeAttribute(GraphMLTokens.ATTR_TYPE, entry.getValue().attrType);
            writer.writeEndElement();
        }

        writer.writeStartElement(GraphMLTokens.GRAPH);
        writer.writeAttribute(GraphMLTokens.ID, GraphMLTokens.G);
        writer.writeAttribute(GraphMLTokens.EDGEDEFAULT, GraphMLTokens.DIRECTED);
        Iterator<Vertex> vertices = graph.vertices();
        Vertex vertex = null;
        Iterator<VertexProperty<Object>> propertyIterator = null;
        VertexProperty<Object> property = null;
        while (vertices.hasNext()) {
        	vertex = vertices.next();
            writer.writeStartElement(GraphMLTokens.NODE);
            writer.writeAttribute(GraphMLTokens.ID, vertex.id().toString());
            propertyIterator = vertex.properties();
            while (propertyIterator.hasNext()) {
            	property = propertyIterator.next();
            	String key = property.key();
            	writeData(writer, key, property.value());
            }
            writer.writeEndElement();
        }

        Iterator<Edge> edges = graph.edges();
        Edge edge = null;
        Iterator<Property<Object>> edgePropertyIterator = null;
        Property<Object> edgeProperty = null;

        while (edges.hasNext()) {
        	edge = edges.next();
        	edgePropertyIterator = edge.properties();
            writer.writeStartElement(GraphMLTokens.EDGE);
            writer.writeAttribute(GraphMLTokens.ID, edge.id().toString());
            writer.writeAttribute(GraphMLTokens.SOURCE, edge.outVertex().id().toString());
            writer.writeAttribute(GraphMLTokens.TARGET, edge.inVertex().id().toString());
            writer.writeAttribute(GraphMLTokens.LABEL, edge.label());

            while (edgePropertyIterator.hasNext()) {
            	edgeProperty = edgePropertyIterator.next();
            	String key = edgeProperty.key();
            	writeData(writer, key, edgeProperty.value());
            }
            writer.writeEndElement();
        }

        writer.writeEndElement(); // graph
        writer.writeEndElement(); // graphml
        writer.writeEndDocument();

        writer.flush();
        writer.close();

    }
    
    /**
     * Write data.
     *
     * @param writer the writer
     * @param key the key
     * @param value the value
     * @throws XMLStreamException the XML stream exception
     */
    private static void writeData(XMLStreamWriter writer, String key, Object value) throws XMLStreamException {
    	if(value == null)
    		return;
    	String type = getStringType(value);
    	if(type == null)
    		return;
    	String id = key+"-"+type;

        writer.writeStartElement(GraphMLTokens.DATA);
        writer.writeAttribute(GraphMLTokens.KEY, id);
        if(type.startsWith(GraphMLTokens.ARRAY) ||
        		type.startsWith(GraphMLTokens.LIST) ||
        		type.startsWith(GraphMLTokens.SET))
        	writeDataArray(writer, value);
        else
        	writer.writeCharacters(propertyValueToString(value));
        writer.writeEndElement();
    }

    /**
     * Write data array.
     *
     * @param writer the writer
     * @param value the value
     * @throws XMLStreamException the XML stream exception
     */
    private static void writeDataArray(XMLStreamWriter writer, Object value) throws XMLStreamException {
    	if(value.getClass().isArray()) {
			for(int i = 0; i < Array.getLength(value); i++) {
				writer.writeStartElement(GraphMLTokens.ITEM);
				writer.writeCharacters(Array.get(value, i).toString());
				writer.writeEndElement();
			}
		}
		else if(value instanceof Collection) {
			for(Object obj : ((Collection<?>)value)) {
				writer.writeStartElement(GraphMLTokens.ITEM);
				writer.writeCharacters(obj.toString());
				writer.writeEndElement();
			}
		}
    }
    
    /**
     * Gets the string type.
     *
     * @param object the object
     * @return the string type
     */
    private static String getStringType(final Object object) {
    	if(object.getClass().isArray())
    		return GraphMLTokens.ARRAY+"-"+getStringType(object.getClass().getComponentType());
    	if(object instanceof Collection) {
    		String colType;
    		if(object instanceof Set)
    			colType = GraphMLTokens.SET;
    		else if(object instanceof List)
    			colType = GraphMLTokens.LIST;
    		else
    			throw new RuntimeException("Unhandled Collection type: "+object.getClass());
    				
    		Collection<?> col = (Collection<?>)object;
    		if(col.size() == 0)
    			return null;
    		return colType+"-"+getStringType(col.iterator().next());
    	}
        if (object instanceof String)
            return GraphMLTokens.STRING;
        else if (object instanceof Integer)
            return GraphMLTokens.INT;
        else if (object instanceof Long)
            return GraphMLTokens.LONG;
        else if (object instanceof Float)
            return GraphMLTokens.FLOAT;
        else if (object instanceof Double)
            return GraphMLTokens.DOUBLE;
        else if (object instanceof Boolean)
            return GraphMLTokens.BOOLEAN;
        else
        	throw new RuntimeException("Unsupported type: "+object.getClass());
    }
    
    /**
     * Gets the string type.
     *
     * @param clazz the clazz
     * @return the string type
     */
    private static String getStringType(final Class<?> clazz) {
    	if(clazz.equals(String.class))
            return GraphMLTokens.STRING;
    	else if(clazz.equals(Integer.TYPE) || clazz.equals(Integer.class))
            return GraphMLTokens.INT;
    	else if(clazz.equals(Long.TYPE) || clazz.equals(Long.class))
            return GraphMLTokens.LONG;
    	else if(clazz.equals(Float.TYPE) || clazz.equals(Float.class))
            return GraphMLTokens.FLOAT;
    	else if(clazz.equals(Double.TYPE) || clazz.equals(Double.class))
            return GraphMLTokens.DOUBLE;
    	else if(clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class))
            return GraphMLTokens.BOOLEAN;
        else
        	throw new RuntimeException("Unsupported array item type: "+clazz);
    }
    
    /**
     * Property value to string.
     *
     * @param value the value
     * @return the string
     */
    private static String propertyValueToString(Object value) {
		if(value == null)
			return null;

		if(value.getClass().isArray()) {
			String str = "[";
			for(int i = 0; i < Array.getLength(value); i++) {
				if(i > 0)
					str += ", ";
				str += Array.get(value, i);
			}
			str += "]";
			return str;	
		}
		else if(value instanceof Collection) {
			String str = "[";
			int i = 0;
			for(Object obj : ((Collection<?>)value)) {
				if(i > 0)
					str += ", ";
				str += obj.toString();
				i++;
			}
			str += "]";
			return str;	
		}
		return value.toString();
	}

}

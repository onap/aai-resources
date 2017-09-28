/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.migration;

import java.util.Iterator;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.serialization.db.DBSerializer;
import org.onap.aai.serialization.db.EdgeRules;
import org.onap.aai.serialization.db.EdgeType;
import org.onap.aai.serialization.db.exceptions.NoEdgeRuleFoundException;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * This class defines an A&AI Migration
 */
public abstract class Migrator implements Runnable {
	
	protected EELFLogger logger = null;

	protected DBSerializer serializer = null;
	protected Loader loader = null;

	protected TransactionalGraphEngine engine;
	protected NotificationHelper notificationHelper;
	
	public Migrator() {
		//used for not great reflection implementation
	}
	/**
	 * Instantiates a new migrator.
	 *
	 * @param g the g
	 */
	public Migrator(TransactionalGraphEngine engine){
        this.engine = engine;
        initDBSerializer();
        this.notificationHelper = new NotificationHelper(loader, serializer, engine, "AAI-MIGRATION", this.getMigrationName());
		logger = EELFManager.getInstance().getLogger(this.getClass().getSimpleName());
		logger.info("\tInitilization of " + this.getClass().getSimpleName() + " migration script complete.");
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public abstract Status getStatus();

	/**
	 * Rollback.
	 */
	public void rollback() {
        engine.rollback();
	}

	/**
	 * Commit.
	 */
	public void commit() {
        engine.commit();
	}

	/**
	 * Gets the priority.
	 *
	 * Lower number has higher priority
	 * 
	 * @return the priority
	 */
	public abstract int getPriority();

	/**
	 * The larger the number, the more danger
	 * 
	 * Range is 0-10
	 * 
	 * @return danger rating
	 */
	public abstract int getDangerRating();
	/**
	 * As string.
	 *
	 * @param v the v
	 * @return the string
	 */
	protected String asString(Vertex v) {
		final JSONObject result = new JSONObject();
		Iterator<VertexProperty<Object>> properties = v.properties();
		Property<Object> pk = null;
		try {
			while (properties.hasNext()) {
				pk = properties.next();
				result.put(pk.key(), pk.value());
			}
		} catch (JSONException e) {
			logger.error("Warning error reading vertex: " + e);
		}

		return result.toString();
	}

	/**
	 * As string.
	 *
	 * @param edge the edge
	 * @return the string
	 */
	protected String asString(Edge edge) {
		final JSONObject result = new JSONObject();
		Iterator<Property<Object>> properties = edge.properties();
		Property<Object> pk = null;
		try {
			while (properties.hasNext()) {
				pk = properties.next();
				result.put(pk.key(), pk.value());
			}
		} catch (JSONException e) {
			logger.error("Warning error reading edge: " + e);
		}

		return result.toString();
	}
	/**
	 * Checks for edge between.
	 *
	 * @param vertex a
	 * @param vertex b
	 * @param direction d
	 * @param edgeLabel the edge label
	 * @return true, if successful
	 */
	protected boolean hasEdgeBetween(Vertex a, Vertex b, Direction d, String edgeLabel) {

		if (d.equals(Direction.OUT)) {
			return engine.asAdmin().getReadOnlyTraversalSource().V(a).out(edgeLabel).where(__.otherV().hasId(b)).hasNext();
		} else {
			return engine.asAdmin().getReadOnlyTraversalSource().V(a).in(edgeLabel).where(__.otherV().hasId(b)).hasNext();
		}

	}
	
	/**
	 * Creates the edge
	 *
	 * @param edgeType the edge type - COUSIN or TREE
	 * @param out the out
	 * @param in the in
	 * @return the edge
	 */
	protected Edge createEdge(EdgeType type, Vertex out, Vertex in) throws AAIException {
		Edge newEdge = null;
		try {
			if (type.equals(EdgeType.COUSIN)){
				newEdge = EdgeRules.getInstance().addEdge(this.engine.asAdmin().getTraversalSource(), out, in);
			} else {
				newEdge = EdgeRules.getInstance().addTreeEdge(this.engine.asAdmin().getTraversalSource(), out, in);
			}
		} catch (NoEdgeRuleFoundException e) {
			throw new AAIException("AAI_6129", e);
		}
		return newEdge;
	}

	/**
	 * Creates the TREE edge 
	 *
	 * @param out the out
	 * @param in the in
	 * @return the edge
	 */
	protected Edge createTreeEdge(Vertex out, Vertex in) throws AAIException {
		Edge newEdge = createEdge(EdgeType.TREE, out, in);
		return newEdge;
	}
	
	/**
	 * Creates the COUSIN edge 
	 *
	 * @param out the out
	 * @param in the in
	 * @return the edge
	 */
	protected Edge createCousinEdge(Vertex out, Vertex in) throws AAIException {
		Edge newEdge = createEdge(EdgeType.COUSIN, out, in);
		return newEdge;
	}

	protected Edge createCousinEdgeBestEffort(Vertex out, Vertex in) throws AAIException {
		return EdgeRules.getInstance().addEdgeIfPossible(this.engine.asAdmin().getTraversalSource(), out, in);
	}
	private void initDBSerializer() {
		Version version = AAIProperties.LATEST;
		ModelType introspectorFactoryType = ModelType.MOXY;
		loader = LoaderFactory.createLoaderForVersion(introspectorFactoryType, version);
		try {
			this.serializer = new DBSerializer(version, this.engine, introspectorFactoryType, this.getMigrationName());
		} catch (AAIException e) {
			throw new RuntimeException("could not create seralizer", e);
		}
	}
	
	/**
	 * These are the node types you would like your traversal to process
	 * @return
	 */
	public abstract Optional<String[]> getAffectedNodeTypes();
	
	/**
	 * used as the "fromAppId" when modifying vertices
	 * @return
	 */
	public abstract String getMigrationName();
	
	/**
	 * updates all internal vertex properties
	 * @param v
	 * @param isNewVertex
	 */
	protected void touchVertexProperties(Vertex v, boolean isNewVertex) {
		this.serializer.touchStandardVertexProperties(v, isNewVertex);
	}
	
	public NotificationHelper getNotificationHelper() {
		return this.notificationHelper;
	}
}

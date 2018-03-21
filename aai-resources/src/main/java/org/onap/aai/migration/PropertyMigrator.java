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
package org.onap.aai.migration;

import java.util.Optional;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

import org.janusgraph.core.Cardinality;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphManagement;

/**
 * A migration template for migrating a property from one name to another
 */
@MigrationPriority(0)
@MigrationDangerRating(1)
public abstract class PropertyMigrator extends Migrator {

	protected final String OLD_FIELD;
	protected final String NEW_FIELD;
	protected final Class<?> fieldType;
	protected final Cardinality cardinality;
	protected final JanusGraphManagement graphMgmt;

	public PropertyMigrator(TransactionalGraphEngine engine, String oldName, String newName, Class<?> type, Cardinality cardinality) {
		super(engine);
		this.OLD_FIELD = oldName;
		this.NEW_FIELD = newName;
		this.fieldType = type;
		this.cardinality = cardinality;
		this.graphMgmt = engine.asAdmin().getManagementSystem();
	}

	/**
	 * Do not override this method as an inheritor of this class
	 */
	@Override
	public void run() {
		
		modifySchema();
		executeModifyOperation();
		
	}

	protected void modifySchema() {
		this.addIndex(this.addProperty());
		graphMgmt.commit();
	}
	
	/**
	 * This is where inheritors should add their logic
	 */
	protected void executeModifyOperation() {
		changePropertyName();
	}
	
	protected void changePropertyName() {
		GraphTraversal<Vertex, Vertex> g = this.engine.asAdmin().getTraversalSource().V();
		if (this.getAffectedNodeTypes().isPresent()) {
			g.has(AAIProperties.NODE_TYPE, P.within(this.getAffectedNodeTypes().get()));
		}
		g.has(OLD_FIELD).sideEffect(t -> {
			final Vertex v = t.get();
			final String value = v.value(OLD_FIELD);
			v.property(OLD_FIELD).remove();
			v.property(NEW_FIELD, value);
			this.touchVertexProperties(v, false);
		}).iterate();
	}
	
	@Override
	public Status getStatus() {
		GraphTraversal<Vertex, Vertex> g = this.engine.asAdmin().getTraversalSource().V();
		if (this.getAffectedNodeTypes().isPresent()) {
			g.has(AAIProperties.NODE_TYPE, P.within(this.getAffectedNodeTypes().get()));
		}
		long result = g.has(OLD_FIELD).count().next();
		if (result == 0) {
			return Status.SUCCESS;
		} else {
			return Status.FAILURE;
		}
	}

	protected Optional<PropertyKey> addProperty() {

		if (!graphMgmt.containsPropertyKey(this.NEW_FIELD)) {
			logger.info(" PropertyKey  [" + this.NEW_FIELD + "] created in the DB. ");
			return Optional.of(graphMgmt.makePropertyKey(this.NEW_FIELD).dataType(this.fieldType).cardinality(this.cardinality)
					.make());
		} else {
			logger.info(" PropertyKey  [" + this.NEW_FIELD + "] already existed in the DB. ");
			return Optional.empty();
		}

	}
	
	protected void addIndex(Optional<PropertyKey> key) {
		if (isIndexed() && key.isPresent()) {
			if (graphMgmt.containsGraphIndex(key.get().name())) {
				logger.debug(" Index  [" + key.get().name() + "] already existed in the DB. ");
			} else {
				logger.info("Add index for PropertyKey: [" + key.get().name() + "]");
				graphMgmt.buildIndex(key.get().name(), Vertex.class).addKey(key.get()).buildCompositeIndex();
			}
		}
	}
	public abstract boolean isIndexed();
	
}

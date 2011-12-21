/**
 * Copyright 2011 the original author or authors.
 *
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
 */

package org.springframework.data.neo4j.support.node;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import org.springframework.data.neo4j.fieldaccess.DefaultEntityState;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class NodeEntityState extends DefaultEntityState<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeEntityState.class);
    private final Neo4jTemplate template;

    public NodeEntityState(final Node underlyingState, final Object entity, final Class<? extends Object> type, final Neo4jTemplate template, final DelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory, Neo4jPersistentEntity<Object> persistentEntity) {
        super(underlyingState, entity, type, nodeDelegatingFieldAccessorFactory,persistentEntity);
        this.template = template;
    }

    @Override
    public void createAndAssignState() {
        if (hasPersistentState()) {
            if (LOG.isInfoEnabled()) LOG.info("Entity {} already has persistent state {}", entity.getClass(), getPersistentState());
            return;
        }
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Node node = template.getNode(((Number) id).longValue());
                setPersistentState(node);
                if (LOG.isInfoEnabled())
                    LOG.info("Entity reattached {}; used Node [{}];", entity.getClass(), getPersistentState());
                return;
            }

            final Node node = template.createNode();
            setPersistentState(node);
            if (LOG.isInfoEnabled()) LOG.info("User-defined constructor called on class {}; created Node [{}]; Updating metamodel", entity.getClass(), getPersistentState());
            template.postEntityCreation(node, type);
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    @Override
    public void setPersistentState(Node node) {
        if (!(entity instanceof ManagedEntity)) {
            template.setPersistentState(entity, node);
        }
        super.setPersistentState(node);
    }

    @Override
    public Object persist() {
        if (getPersistentState() == null) {
            createAndAssignState();
        }
        return entity;
    }
}

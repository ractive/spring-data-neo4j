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
package org.springframework.data.neo4j.repository.query;

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.util.Assert;

import java.util.ArrayList;

/**
 * Representation of a Cypher {@literal start} clause.
 * 
 * @author Oliver Gierke
 */
class StartClause {

    private static final String TEMPLATE = "%s=(%s,?%d)";

    private final PersistentPropertyPath<Neo4jPersistentProperty> path;
    private final String variable;
    private final int index;

    /**
     * Creates a new {@link StartClause} from the given {@link Neo4jPersistentProperty}, variable and the given
     * parameter index.
     * 
     * @param property must not be {@literal null}.
     * @param variable must not be {@literal null} or empty.
     * @param index
     */
    public StartClause(PersistentPropertyPath<Neo4jPersistentProperty> property, String variable, int index) {

        Assert.notNull(property);
        Assert.hasText(variable);

        this.path = property;
        this.variable = variable;
        this.index = index;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final Neo4jPersistentProperty leafProperty = path.getLeafProperty();
        return String.format("%s=node:%s(%s={_%d})", variable, leafProperty.getIndexInfo().getIndexName(), leafProperty.getNeo4jPropertyName(), index);
    }
}
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

import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to build the {@literal match} clause of a Cypher query.
 * 
 * @author Oliver Gierke
 */
class MatchClause {

    private final PersistentPropertyPath<Neo4jPersistentProperty> properties;
    private final boolean hasRelationShip;

    /**
     * Creates a new {@link MatchClause} using the given {@link PersistentPropertyPath}.
     * 
     * @param properties must not be {@literal null}.
     */
    public MatchClause(PersistentPropertyPath<Neo4jPersistentProperty> properties) {

        Assert.notNull(properties);
        this.properties = properties;

        for (Neo4jPersistentProperty property : properties) {
            if (property.isRelationship()) {
                this.hasRelationShip = true;
                return;
            }
        }

        this.hasRelationShip = false;
    }

    /**
     * Returns whether the match clause actually deals with a relationship.
     * 
     * @return
     */
    public boolean hasRelationship() {
        return hasRelationShip;
    }


    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        String intermediate = null;

        for (Neo4jPersistentProperty property : properties) {

            if (!property.isRelationship()) {
                return intermediate == null ? "" : intermediate;
            }

            RelationshipInfo info = property.getRelationshipInfo();
            Class<?> ownerType = property.getOwner().getType();

            intermediate = intermediate == null ? StringUtils.uncapitalize(ownerType.getSimpleName()) : intermediate;
            intermediate = String.format(getPattern(info), intermediate, info.getType(), property.getName());
        }

        return intermediate.toString();
    }

    /**
     * Returns the clause pattern for the given {@link RelationshipInfo}.
     * 
     * @param info must not be {@literal null}.
     * @return
     */
    private static String getPattern(RelationshipInfo info) {

        switch (info.getDirection()) {
        case OUTGOING:
            return "%s-[:%s]->%s";
        case INCOMING:
            return "%s<-[:%s]-%s";
        case BOTH:
            return "%s-[:%s]-%s";
        default:
            throw new IllegalArgumentException("Unsupported direction!");
        }
    }
}
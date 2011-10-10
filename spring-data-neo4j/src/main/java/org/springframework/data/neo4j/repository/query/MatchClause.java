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

    private final PersistentPropertyPath<Neo4jPersistentProperty> path;

    /**
     * Creates a new {@link MatchClause} using the given {@link PersistentPropertyPath}.
     * 
     * @param path must not be {@literal null}.
     */
    public MatchClause(PersistentPropertyPath<Neo4jPersistentProperty> path) {
        Assert.notNull(path);
        this.path = relationshipPath(path);
    }

    private PersistentPropertyPath<Neo4jPersistentProperty> relationshipPath(PersistentPropertyPath<Neo4jPersistentProperty> path) {
        if (path.length()==1 || path.getLeafProperty().isRelationship()) return path;
        return relationshipPath(path.getParentPath());
    }

    /**
     * Returns whether the match clause actually deals with a relationship.
     */
    public boolean hasRelationship() {
        for (Neo4jPersistentProperty property : path) {
            if (property.isRelationship()) {
                return true;
            }
        }
        return false;
    }


    private static String getArrow(RelationshipInfo info) {

        switch (info.getDirection()) {
        case OUTGOING:
            return "-[:"+info.getType() +"]->";
        case INCOMING:
            return "<-[:"+info.getType() +"]-";
        case BOTH:
            return "-[:"+info.getType() +"]-";
        default:
            throw new IllegalArgumentException("Unsupported direction!");
        }
    }

    public String toString(VariableContext variableContext) {
        return matchPattern(variableContext,path);
    }

    private String matchPattern(VariableContext variableContext, PersistentPropertyPath<Neo4jPersistentProperty> relPath) {
        if (relPath.length()==1) {
           final Neo4jPersistentProperty property = relPath.getBaseProperty();
           return variableContext.getVariableFor(property.getOwner()) + getArrow(property.getRelationshipInfo()) + variableContext.getVariableFor(relPath);
        }
        final RelationshipInfo info = relPath.getLeafProperty().getRelationshipInfo();
        return matchPattern(variableContext, relPath.getParentPath()) + getArrow(info) + variableContext.getVariableFor(relPath);
    }
}
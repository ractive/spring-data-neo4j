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

import static org.springframework.util.StringUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to create Cypher queries.
 *
 * @author Oliver Gierke
 */
public class CypherQueryBuilder {

    private final static String DEFAULT_START_CLAUSE_TEMPLATE = "%s=(__types__,className,%s)";

    private final Neo4jMappingContext context;

    private final String defaultStartClause;

    private final VariableContext variableContext;
    private final List<MatchClause> matchClauses;
    private final List<StartClause> startClauses;
    private final List<WhereClause> whereClauses;

    private int index = 0;

    /**
     * Creates a new {@link CypherQueryBuilder}.
     * 
     * @param context must not be {@literal null}.
     * @param type must not be {@literal null}.
     */
    public CypherQueryBuilder(Neo4jMappingContext context, Class<?> type) {

        Assert.notNull(context);
        Assert.notNull(type);

        this.defaultStartClause = String.format(DEFAULT_START_CLAUSE_TEMPLATE,
                StringUtils.uncapitalize(type.getSimpleName()), type.getName());

        this.context = context;
        this.variableContext = new VariableContext();
        this.matchClauses = new ArrayList<MatchClause>();
        this.startClauses = new ArrayList<StartClause>();
        this.whereClauses = new ArrayList<WhereClause>();
    }

    /**
     * Adds the given {@link Part} to the restrictions for the query.
     * 
     * @param part
     * @return
     */
    public CypherQueryBuilder addRestriction(Part part) {

        PersistentPropertyPath<Neo4jPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        String variable = variableContext.getVariableFor(path);

        Neo4jPersistentProperty leafProperty = path.getLeafProperty();

        if (leafProperty.isIndexed()) {
            startClauses.add(new StartClause(path, variable, index++));
        } else {
            whereClauses.add(new WhereClause(path, variable, part.getType(), index++));
        }

        MatchClause matchClause = new MatchClause(path);

        if (matchClause.hasRelationship()) {
            matchClauses.add(matchClause);
        }

        return this;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        String startClauses = collectionToDelimitedString(this.startClauses, ", ");
        String matchClauses = collectionToDelimitedString(this.matchClauses, ", ");
        String whereClauses = collectionToDelimitedString(this.whereClauses, ", ");

        StringBuilder builder = new StringBuilder("start ");

        if (hasText(startClauses)) {
            builder.append(startClauses);
        } else {
            builder.append(defaultStartClause);
        }

        if (hasText(matchClauses)) {
            builder.append(" match ").append(matchClauses);
        }

        if (hasText(whereClauses)) {
            builder.append(" where ").append(whereClauses);
        }

        return builder.toString().trim();
    }
}

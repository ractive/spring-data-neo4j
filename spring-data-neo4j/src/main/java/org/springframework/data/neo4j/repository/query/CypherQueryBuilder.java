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
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntityImpl;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;

/**
 * Value object to create Cypher queries.
 *
 * @author Oliver Gierke
 */
public class CypherQueryBuilder {

    private final static String DEFAULT_START_CLAUSE_TEMPLATE = "%s=node:__types__(className=\"%s\")";

    private final Neo4jMappingContext context;

    private final VariableContext variableContext = new VariableContext();
    private final List<MatchClause> matchClauses = new ArrayList<MatchClause>();
    private final List<StartClause> startClauses = new ArrayList<StartClause>();
    private final List<WhereClause> whereClauses = new ArrayList<WhereClause>();
    private Sort sort;
    private Pageable pageable;
    private int index = 0;
    private final Neo4jPersistentEntityImpl<?> entity;

    /**
     * Creates a new {@link CypherQueryBuilder}.
     * 
     * @param context must not be {@literal null}.
     * @param type must not be {@literal null}.
     */
    public CypherQueryBuilder(Neo4jMappingContext context, Class<?> type) {
        Assert.notNull(context);
        Assert.notNull(type);

        this.context = context;
        this.entity = context.getPersistentEntity(type);
    }

    public void setPageable(Pageable pageable) {
        this.pageable = pageable;
    }

    public void addSort(Sort sort) {
        if (this.sort == null) {
            this.sort = sort;
        }
        else {
            this.sort = this.sort.and(sort);
        }
    }

    private String defaultStartClause() {
        return String.format(DEFAULT_START_CLAUSE_TEMPLATE, this.variableContext.getVariableFor(entity), entity.getType().getName());
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
        if (!leafProperty.isRelationship()) {
        if (leafProperty.isIndexed()) {
            startClauses.add(new StartClause(path, variable, index++));
        } else {
            whereClauses.add(new WhereClause(path, variable, part.getType(), index++));
        }
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
        String matchClauses = toString(this.matchClauses);
        String whereClauses = collectionToDelimitedString(this.whereClauses, ", ");

        StringBuilder builder = new StringBuilder("start ");

        if (hasText(startClauses)) {
            builder.append(startClauses);
        } else {
            builder.append(defaultStartClause());
        }

        if (hasText(matchClauses)) {
            builder.append(" match ").append(matchClauses);
        }

        if (hasText(whereClauses)) {
            builder.append(" where ").append(whereClauses);
        }
        
        builder.append(" return ").append(variableContext.getVariableFor(entity));

        addSorts(builder);

        if (pageable != null) {
           builder.append(String.format(" skip %d limit %d", pageable.getOffset(), pageable.getPageSize()));
        }

        return builder.toString().trim();
    }

    private void addSorts(StringBuilder builder) {
        final List<String> sorts = formatSorts(sort);
        if (this.pageable !=null) {
            sorts.addAll(formatSorts(this.pageable.getSort()));
        }
        if (!sorts.isEmpty()) {
            builder.append(" order by ").append(collectionToCommaDelimitedString(sorts));
        }
    }

    private List<String> formatSorts(Sort sort) {
        List<String> result=new ArrayList<String>();
        if (sort == null) return result;

        for (Sort.Order order : sort) {
            result.add(String.format("%s %s",order.getProperty(),order.getDirection()));
        }
        return result;
    }

    private String toString(List<MatchClause> matchClauses) {
        List<String> result=new ArrayList<String>(matchClauses.size());
        for (MatchClause matchClause : matchClauses) {
            result.add(matchClause.toString(variableContext));
        }
        return collectionToDelimitedString(result, ", ");
    }
}

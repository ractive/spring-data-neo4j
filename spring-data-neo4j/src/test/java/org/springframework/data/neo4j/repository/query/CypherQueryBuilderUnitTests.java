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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.parser.Part;

/**
 * Unit tests for {@link CypherQueryBuilder}.
 * 
 * @author Oliver Gierke
 */
public class CypherQueryBuilderUnitTests {

    CypherQueryBuilder query;

    @Before
    public void setUp() {
        Neo4jMappingContext context = new Neo4jMappingContext();
        query = new CypherQueryBuilder(context, Person.class);
    }

    @Test
    public void createsQueryForSimplePropertyReference() {

        Part part = new Part("name", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is("start person=(name,?0)"));
    }

    @Test
    public void createsQueryForPropertyOnRelationShipReference() {

        Part part = new Part("group.name", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is("start person_group=(group,name,?0) match person<-[:members]-group"));
    }

    @Test
    public void createsQueryForMultipleStartClauses() {

        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("group.name", Person.class));

        assertThat(query.toString(),
                is("start person=(name,?0), person_group=(group,name,?1) match person<-[:members]-group"));
    }

    @Test
    public void createsSimpleWhereClauseCorrectly() {

        query.addRestriction(new Part("age", Person.class));

        assertThat(query.toString(), is("start person=(__types__,className," + Person.class.getName()
                + ") where person.age = ?0"));
    }


    @Test
    public void buildsComplexQueryCorrectly() {

        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("groupName", Person.class));
        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("groupMembersAge", Person.class));

        System.out.println(query.toString());
    }
}

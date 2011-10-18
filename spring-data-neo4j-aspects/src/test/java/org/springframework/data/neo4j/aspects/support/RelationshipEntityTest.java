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

package org.springframework.data.neo4j.aspects.support;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml"})

public class RelationshipEntityTest extends EntityTestBase {

    @Test
    @Transactional
    public void testRelationshipCreate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Relationship rel = getNodeState(p).getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
        assertEquals(getRelationshipState(f), rel);
        assertEquals(getNodeState(p2), rel.getEndNode());
    }

    @Test
    @Transactional
    public void shouldNotCreateSameRelationshipTwice() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Friendship f2 = p.knows(p2);
        assertEquals(f, f2);
        assertEquals(1, IteratorUtil.count(p.getFriendships()));
    }

    @Test
    @Transactional
    public void testRelationshipSetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setYears(1);
        assertEquals(1, getRelationshipState(f).getProperty("Friendship.years"));
    }

    @Test
    @Transactional
    public void testRelationshipGetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        getRelationshipState(f).setProperty("Friendship.years", 1);
        assertEquals(1, f.getYears());
    }

    @Test
    @Transactional
    public void testRelationshipGetStartNodeAndEndNode() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(p, f.getPerson1());
        assertEquals(p2, f.getPerson2());
    }

    @Test
    @Transactional
    public void testGetRelationshipToReturnsRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(f, neo4jTemplate.getRelationshipBetween(p, p2, Friendship.class, "knows"));
    }
    
    //@Ignore("The NodeBacking.getRelationshipTo() method is broken at the moment")
    @Test
    @Transactional
    public void testGetRelationshipTo() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Relationship frel = p.getRelationshipTo(p2, "knows");
        assertNotNull(frel);
        assertEquals(f.getPersistentState().getId(), frel.getId());
    }

    @Test
    public void testRemoveRelationshipEntity() {
        cleanDb();
        Friendship f;
        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            Person p = persistedPerson("Michael", 35);
            Person p2 = persistedPerson("David", 25);
            f = p.knows(p2);
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        Transaction tx2 = graphDatabaseService.beginTx();
        try
        {
            neo4jTemplate.removeRelationshipEntity(f);
            tx2.success();
        }
        finally
        {
            tx2.finish();
        }
        assertFalse("Unexpected relationship entity found.", friendshipRepository.findAll().iterator().hasNext());
    }

    @Test
    public void testRemoveRelationshipEntityIfNodeEntityIsRemoved() {
        cleanDb();
        Person p;
        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            p = persistedPerson("Michael", 35);
            Person p2 = persistedPerson("David", 25);
            p.knows(p2);
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        Transaction tx2 = graphDatabaseService.beginTx();
        try
        {
            neo4jTemplate.removeNodeEntity(p);
            tx2.success();
        }
        finally
        {
            tx2.finish();
        }
        assertFalse("Unexpected relationship entity found.", friendshipRepository.findAll().iterator().hasNext());
    }
}

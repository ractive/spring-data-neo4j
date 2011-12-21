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

package org.springframework.data.neo4j.aspects.support.typerepresentation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.aspects.Car;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.Toyota;
import org.springframework.data.neo4j.aspects.Volvo;
import org.springframework.data.neo4j.aspects.support.EntityTestBase;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.typerepresentation.SubReferenceNodeTypeRepresentationStrategy;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.Assert.*;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

/**
 * @author mh
 * @since 20.01.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/SubReferenceTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class SubReferenceNodeTypeRepresentationStrategyTest extends EntityTestBase {

	@Autowired
    private SubReferenceNodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;
    @Autowired
    EntityStateHandler entityStateHandler;
    private Node thingNode;
    private Thing thing;
    private SubThing subThing;
    private Node subThingNode;

    @Before
    public void setUp() {
        createThing();
    }

    @Test
    @Transactional
    public void testPostEntityCreation() throws Exception {
        Node typeNode = getInstanceofRelationship(thingNode).getOtherNode(thingNode);
        assertNotNull("type node for thing exists", typeNode);
        assertEquals("type node has property of type Thing.class", Thing.class.getName(), typeNode.getProperty(SubReferenceNodeTypeRepresentationStrategy.SUBREF_CLASS_KEY));
        assertEquals("one thing has been created", 2, typeNode.getProperty(SubReferenceNodeTypeRepresentationStrategy.SUBREFERENCE_NODE_COUNTER_KEY));
    }
    @Test(expected = IllegalArgumentException.class)
    public void gettingTypeFromNonTypeNodeShouldThrowAnDescriptiveException() throws Exception {
        Node referenceNode = neo4jTemplate.getReferenceNode();
        nodeTypeRepresentationStrategy.getJavaType(referenceNode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void gettingTypeFromNullShouldFail() throws Exception {
        nodeTypeRepresentationStrategy.getJavaType(null);
    }

    private void createThing() {
        Transaction tx = neo4jTemplate.beginTx();
        try {
            thingNode = neo4jTemplate.createNode();
            thing = neo4jTemplate.setPersistentState(new Thing(),thingNode);
            nodeTypeRepresentationStrategy.postEntityCreation(thingNode, Thing.class);
            thing.setName("thing");
            subThingNode = neo4jTemplate.createNode();
            subThing = neo4jTemplate.setPersistentState(new SubThing(),subThingNode);
            nodeTypeRepresentationStrategy.postEntityCreation(subThingNode, SubThing.class);
            subThing.setName("subThing");
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private Node node(Thing thing) {
        return getNodeState(thing);
    }

    @Test
    @Transactional
    public void testPreEntityRemoval() throws Exception {
        Node typeNode = getInstanceofRelationship(thingNode).getOtherNode(thingNode);
        nodeTypeRepresentationStrategy.preEntityRemoval(node(thing));
        assertNull("instanceof relationship was removed", getInstanceofRelationship(thingNode));
        assertNotNull("instanceof relationship was removed", getInstanceofRelationship(subThingNode));
        assertEquals("no things left after removal", 1, typeNode.getProperty(SubReferenceNodeTypeRepresentationStrategy.SUBREFERENCE_NODE_COUNTER_KEY));
        nodeTypeRepresentationStrategy.preEntityRemoval(node(subThing));
        assertNull("instanceof relationship was removed", getInstanceofRelationship(subThingNode));
        assertEquals("no things left after removal", 0, typeNode.getProperty(SubReferenceNodeTypeRepresentationStrategy.SUBREFERENCE_NODE_COUNTER_KEY));

    }

    @Transactional
    private Relationship getInstanceofRelationship(Node node) {
        return node.getSingleRelationship(SubReferenceNodeTypeRepresentationStrategy.INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
    }

    @Test
    @Transactional
    public void testCount() throws Exception {
        assertEquals("one thing created", 2, nodeTypeRepresentationStrategy.count(Thing.class));
        assertEquals("one thing created", 1, nodeTypeRepresentationStrategy.count(SubThing.class));
    }

    @Test
    @Transactional
    public void testGetJavaType() throws Exception {
        assertEquals("class in graph is thing", Thing.class, nodeTypeRepresentationStrategy.getJavaType(thingNode));

    }

    @Test
    @Transactional
    public void testFindAllThings() throws Exception {
        Collection<Node> things = IteratorUtil.asCollection(nodeTypeRepresentationStrategy.findAll(Thing.class));
        assertEquals("one thing created and found", 2, things.size());
    }

    @Test
    @Transactional
    public void testFindAllSubThings() {
        Collection<Node> things = IteratorUtil.asCollection(nodeTypeRepresentationStrategy.findAll(SubThing.class));
        assertEquals("one thing created and found", 1,things.size());
        assertEquals("one thing created and found", entityStateHandler.<Node>getPersistentState(subThing), IteratorUtil.first(things));
    }

    @Test
	@Transactional
	public void testInstantiateConcreteClass() {
        Person p = persistedPerson("Michael", 35);
        Car c = persist(new Volvo());
		p.setCar(c);
		assertEquals("Wrong concrete class.", Volvo.class, p.getCar().getClass());
	}

	@Test
	@Transactional
	public void testInstantiateConcreteClassWithFinder() {
        persist(new Volvo());
        GraphRepository<Car> finder = neo4jTemplate.repositoryFor(Car.class);
		assertEquals("Wrong concrete class.", Volvo.class, finder.findAll().iterator().next().getClass());
	}

	@Test
	@Transactional
	public void testCountSubclasses() {
        persist(new Volvo());
        persist(new Toyota());
        assertEquals("Wrong count for Volvo.", 1L, neo4jTemplate.repositoryFor(Volvo.class).count());
        assertEquals("Wrong count for Toyota.", 1L, neo4jTemplate.repositoryFor(Toyota.class).count());
        assertEquals("Wrong count for Car.", 2L, neo4jTemplate.repositoryFor(Car.class).count());
	}
	@Test
	@Transactional
	public void testCountClasses() {
        persistedPerson("Michael", 36);
        persistedPerson("David", 25);
        assertEquals("Wrong Person instance count.", 2L, neo4jTemplate.repositoryFor(Person.class).count());
	}


	@Test
	@Transactional
	public void testCreateEntityAndInferType() throws Exception {
        Thing newThing = neo4jTemplate.createEntityFromStoredType(node(thing), neo4jTemplate.getMappingPolicy(thing));
        assertEquals(thing, newThing);
    }

	@Test
	@Transactional
	public void testCreateEntityAndSpecifyType() throws Exception {
        Thing newThing = neo4jTemplate.createEntityFromState(node(subThing), Thing.class, neo4jTemplate.getMappingPolicy(subThing));
        assertEquals(subThing, newThing);
    }

    @Test
    @Transactional
	public void testProjectEntity() throws Exception {
        Unrelated other = neo4jTemplate.projectTo(thing, Unrelated.class);
        assertEquals("thing", other.getName());
	}

    @NodeEntity
    public static class Unrelated {
        String name;

        public String getName() {
            return name;
        }
    }

    @NodeEntity
    public static class Thing {
        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SubThing extends Thing {
    }
}

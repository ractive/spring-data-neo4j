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

package org.springframework.data.neo4j.model;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.support.index.IndexType;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Map;


@NodeEntity
public class Person {

    public static final String NAME_INDEX = "name_index";
    @GraphId
	private Long graphId;

    @Indexed(indexName = NAME_INDEX)
    @Size(min = 3, max = 20)
	private String name;

	@Indexed
	private String nickname;

	@Indexed(indexType = IndexType.POINT, indexName="personLayer")
    private String wkt;

	@Max(100)
	@Min(0)
    @Indexed
    private int age;

	private Object dynamicProperty;

	private Short height;

	private transient String thought;

	private Personality personality;

	private Date birthdate;

	private Person spouse;

	private Car car;

	private DynamicProperties personalProperties;

	@RelatedTo
	private Person mother;

	@RelatedTo(type = "boss", direction = Direction.INCOMING)
	private Person boss;

    @Fetch
	@RelatedToVia(type = "knows", elementClass = Friendship.class)
	private Iterable<Friendship> friendships;

    @Query("start person=node({self}) match (person)<-[?:boss]-(boss) return boss")
    private Person bossByQuery;

    @Query("start person=node({self}) match (person)<-[?:boss]-(boss) return boss.name")
    private String bossName;

    @Query("start person=node({self}) match (person)<-[:persons]-(team)-[:persons]->(member) return member")
    private Iterable<Person> otherTeamMembers;

    @Query("start person=node({self}) match (person)<-[:persons]-(team)-[:persons]->(member) return member.name, member.age")
    private Iterable<Map<String,Object>> otherTeamMemberData;

    public Person(Node n) {
        this.graphId = n.getId();
    }

    public String getBossName() {
        return bossName;
    }

    public Iterable<Person> getOtherTeamMembers() {
        return otherTeamMembers;
    }

    public Iterable<Map<String, Object>> getOtherTeamMemberData() {
        return otherTeamMemberData;
    }

    public Person getBossByQuery() {
        return bossByQuery;
    }

    public Person() {
    }

    public Person(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Short getHeight() {
		return height;
	}

	public void setHeight(Short height) {
		this.height = height;
	}


	public Person getSpouse() {
		return spouse;
	}

	public void setSpouse(Person spouse) {
		this.spouse = spouse;
	}

	public Person getMother() {
		return mother;
	}

	public void setMother(Person mother) {
		this.mother = mother;
	}

	public Person getBoss() {
		return boss;
	}

	public void setBoss(Person boss) {
		this.boss = boss;
	}
	
	public void setLocation(String locationInWkt) {
        this.wkt = locationInWkt;
    }

    public void setLocation(double lon, double lat) {
        this.wkt = "POINT ( "+lon+" "+lat+" )";
    }

	@Override
	public String toString() {
		return "["+graphId+"] " + name;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;
        if (graphId == null) return super.equals(o);
        return graphId.equals(person.graphId);

    }

    @Override
    public int hashCode() {
        return graphId != null ? graphId.hashCode() : super.hashCode();
    }

    public Iterable<Friendship> getFriendships() {
		return friendships;
	}

	public void setFriendships(Iterable<Friendship> f) {
		friendships = f;
	}

	public Friendship knows(Person p) {
        return new Friendship(this, p, "knows");
	}

	public void setPersonality(Personality personality) {
		this.personality = personality;
	}

	public Personality getPersonality() {
		return personality;
	}

	public void setThought(String thought) {
		this.thought = thought;
	}

	public String getThought() {
		return thought;
	}

	public Date getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(Date birthdate) {
		this.birthdate = birthdate;
	}

	public Long getId() {
		return graphId;
	}

	public void setCar(Car car) {
		this.car = car;
	}

	public Car getCar() {
		return car;
	}

	public void setProperty(String key, Object value) {
		personalProperties.setProperty(key, value);
	}
	
	public Object getProperty(String key) {
		return personalProperties.getProperty(key);
	}
	
	public DynamicProperties getPersonalProperties() {
		return personalProperties;
	}
	
	public void setPersonalProperties(DynamicProperties personalProperties) {
		this.personalProperties = personalProperties;
	}
	
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public void setDynamicProperty(Object dynamicProperty) {
		this.dynamicProperty = dynamicProperty;
	}

	public Object getDynamicProperty() {
		return dynamicProperty;
	}

    public Person(Long graphId) {
        this.graphId = graphId;
    }
}

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

package org.springframework.data.neo4j.fieldaccess;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.lucene.ValueContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;


public class IndexingPropertyFieldAccessorListenerFactory<S extends PropertyContainer, T> implements FieldAccessorListenerFactory {

    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;
    private final Neo4jTemplate template;

    public IndexingPropertyFieldAccessorListenerFactory(final Neo4jTemplate template, final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
        this.template = template;
    	this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return isPropertyField(property) && property.isIndexed();
    }


    private boolean isPropertyField(final Neo4jPersistentProperty property) {
        return propertyFieldAccessorFactory.accept(property) || convertingNodePropertyFieldAccessorFactory.accept(property);
    }

    @Override
    public FieldAccessListener forField(Neo4jPersistentProperty property) {
        return new IndexingPropertyFieldAccessorListener(property, template);
    }


    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingPropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener {

	    protected final String indexKey;
        private final Neo4jPersistentProperty property;
        private final Neo4jTemplate template;

        public IndexingPropertyFieldAccessorListener(final Neo4jPersistentProperty property, Neo4jTemplate template) {
            this.property = property;
            this.template = template;
            indexKey = template.getIndexKey(property);
        }

	    @Override
        public void valueChanged(Object entity, Object oldVal, Object newVal) {
            Index<T> index = template.getIndex(property, entity.getClass());
            if (newVal instanceof Number) newVal = ValueContext.numeric((Number) newVal);

            final T state = template.getPersistentState(entity);
            index.remove(state, indexKey);
            if (newVal != null) {
                index.add(state, indexKey, newVal);
            }
        }
    }
}

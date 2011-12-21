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

import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.util.Set;


class ValidatingNodePropertyFieldAccessorListenerFactory implements FieldAccessorListenerFactory {

    private final Neo4jTemplate template;

    ValidatingNodePropertyFieldAccessorListenerFactory(final Neo4jTemplate template) {
    	this.template = template;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return hasValidationAnnotation(property);
    }

    private boolean hasValidationAnnotation(final Neo4jPersistentProperty property) {
        for (Annotation annotation : property.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Constraint.class)) return true;
        }
        return false;
    }

    @Override
    public FieldAccessListener forField(Neo4jPersistentProperty property) {
        return new ValidatingNodePropertyFieldAccessorListener(property, template.getValidator());
    }


    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class ValidatingNodePropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener {

        private String propertyName;
        private Validator validator;
        private Neo4jPersistentEntity<?> entityType;

        public ValidatingNodePropertyFieldAccessorListener(final Neo4jPersistentProperty field, Validator validator) {
            this.propertyName = field.getName();
            this.entityType = (Neo4jPersistentEntity<?>) field.getOwner();
            this.validator = validator;
        }

	    @Override
        public void valueChanged(Object entity, Object oldVal, Object newVal) {
            if (validator==null) return;
            Set<ConstraintViolation<T>> constraintViolations = validator.validateValue((Class<T>)entityType.getType(), propertyName, newVal);
            if (!constraintViolations.isEmpty()) throw new ValidationException("Error validating field "+propertyName+ " of "+entityType+": "+constraintViolations);
        }
    }
}

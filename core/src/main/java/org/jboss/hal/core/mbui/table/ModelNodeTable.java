/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.hal.core.mbui.table;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import elemental.js.dom.JsElement;
import elemental.js.util.JsArrayOf;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.jboss.hal.ballroom.JsCallback;
import org.jboss.hal.ballroom.JsHelper;
import org.jboss.hal.ballroom.table.Button;
import org.jboss.hal.ballroom.table.Button.Scope;
import org.jboss.hal.ballroom.table.Column;
import org.jboss.hal.ballroom.table.DataTable;
import org.jboss.hal.ballroom.table.GenericOptionsBuilder;
import org.jboss.hal.ballroom.table.Options;
import org.jboss.hal.ballroom.table.RefreshMode;
import org.jboss.hal.core.Core;
import org.jboss.hal.core.CrudOperations.AddCallback;
import org.jboss.hal.dmr.ModelNode;
import org.jboss.hal.dmr.Property;
import org.jboss.hal.meta.AddressTemplate;
import org.jboss.hal.meta.Metadata;
import org.jboss.hal.meta.security.AuthorisationDecision;
import org.jboss.hal.meta.security.ElementGuard;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.UIConstants;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.hal.ballroom.table.RefreshMode.RESET;
import static org.jboss.hal.dmr.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.hal.dmr.ModelDescriptionConstants.NAME;
import static org.jboss.hal.resources.UIConstants.data;

/**
 * @author Harald Pehl
 */
public class ModelNodeTable<T extends ModelNode> extends DataTable<T> {

    @JsType(namespace = "ui", name = "TableBuilder")
    public static class Builder<T extends ModelNode> extends GenericOptionsBuilder<Builder<T>, T> {

        private final String id;
        private final Metadata metadata;
        private final ColumnFactory columnFactory;

        @JsIgnore
        public Builder(@NonNls final String id, final Metadata metadata) {
            this.id = id;
            this.metadata = metadata;
            this.columnFactory = new ColumnFactory();
        }

        @JsIgnore
        public Builder<T> columns(@NonNls String first, @NonNls String... rest) {
            List<String> columns = Lists.asList(first, rest);
            for (String column : columns) {
                column(column);
            }
            return that();
        }

        @JsIgnore
        public Builder<T> columns(Iterable<String> attributes) {
            if (attributes != null) {
                attributes.forEach(this::column);
            }
            return that();
        }

        public Builder<T> column(@NonNls String attribute) {
            Property attributeDescription = metadata.getDescription().findAttribute(ATTRIBUTES, attribute);
            if (attributeDescription != null) {
                Column<T> column = columnFactory.createColumn(attributeDescription);
                return column(column);
            } else {
                logger.error("No attribute description for column '{}' found in resource description\n{}",
                        attribute, metadata.getDescription());
                return that();
            }
        }

        @Override
        protected Builder<T> that() {
            return this;
        }

        @Override
        protected void validate() {
            super.validate();
            if (!metadata.getDescription().hasDefined(ATTRIBUTES)) {
                throw new IllegalStateException(
                        "No attributes found in resource description\n" + metadata.getDescription());
            }
        }

        public ModelNodeTable<T> build() {
            return new ModelNodeTable<>(this);
        }


        // ------------------------------------------------------ JS methods


        @JsFunction
        public interface NameProvider {

            String name();
        }


        @JsMethod(name = "add")
        public Builder<T> jsAdd(final String type, final Object template, final JsArrayOf<String> attributes,
                final AddCallback callback) {
            TableButtonFactory buttonFactory = Core.INSTANCE.tableButtonFactory();
            String id = Ids.build(Ids.uniqueId(), Ids.ADD_SUFFIX);
            Button<T> button = buttonFactory.add(id, type, jsTemplate("add", template), JsHelper.asList(attributes),
                    callback);
            return button(button);
        }

        @JsMethod(name = "remove")
        public Builder<T> jsRemove(final String type, final Object template, final NameProvider nameProvider,
                final JsCallback callback) {
            TableButtonFactory buttonFactory = Core.INSTANCE.tableButtonFactory();
            Button<T> button = buttonFactory.remove(type, jsTemplate("remove", template),
                    table -> nameProvider.name(), callback::execute);
            return button(button);
        }

        @JsMethod(name = "button")
        public Builder<T> jsButton(final String text, final String scope, final JsCallback callback) {
            return button(text, Scope.fromScope(scope), (event, table) -> callback.execute());
        }

        @JsMethod(name = "columns")
        public Builder<T> jsColumns(final JsArrayOf<String> columns) {
            return columns(JsHelper.asList(columns));
        }

        private AddressTemplate jsTemplate(String method, Object template) {
            AddressTemplate t;
            if (template instanceof String) {
                t = AddressTemplate.of(((String) template));
            } else if (template instanceof AddressTemplate) {
                t = (AddressTemplate) template;
            } else {
                throw new IllegalArgumentException(
                        "Invalid 2nd argument: Use TableBuilder." + method + "(String type, (String|AddressTemplate) template, String[] attributes, function(String name, ResourceAddress address) callback)");
            }
            return t;
        }
    }


    @NonNls private static final Logger logger = LoggerFactory.getLogger(ModelNodeTable.class);

    private final Metadata metadata;
    private final Options<T> options;
    private Function<T, String> identifier;
    private boolean identifierChecked;

    private ModelNodeTable(Builder<T> builder) {
        super(builder.id, builder.options());
        this.options = builder.options();
        this.metadata = builder.metadata;
        this.identifier = null;
        this.identifierChecked = false;
    }

    @Override
    @JsMethod
    public void attach() {
        super.attach();
        if (options.buttons.buttons != null) {
            for (int i = 0; i < options.buttons.buttons.length; i++) {
                if (options.buttons.buttons[i].constraint != null) {
                    buttonElement(i).attr(data(UIConstants.CONSTRAINT), options.buttons.buttons[i].constraint);
                }
            }
        }
        applySecurity();
    }

    /**
     * Shortcut for {@code super.select(data, NamedNode::getName)}
     */
    public void select(T data) {
        if (!identifierChecked) {
            checkIdentifier(data);
        }
        select(data, identifier);
    }

    @Override
    public void select(final T data, final Function<T, String> identifier) {
        super.select(data, identifier);
        applySecurity();
    }

    /**
     * Shortcut for {@code super.update(data, NamedNode::getName)}
     */
    public void update(final Iterable<T> data) {
        if (!identifierChecked) {
            checkIdentifier(Iterables.isEmpty(data) ? null : data.iterator().next());
        }
        update(data, RESET, identifier);
    }

    @Override
    public void update(final Iterable<T> data, final RefreshMode mode, final Function<T, String> identifier) {

        super.update(data, mode, identifier);
        applySecurity();
    }

    private void checkIdentifier(T data) {
        if (data != null) {
            if (data.hasDefined(NAME)) {
                identifier = model -> model.get(NAME).asString();
            }
            identifierChecked = true;
        }
    }

    private void applySecurity() {
        AuthorisationDecision ad = AuthorisationDecision.from(Core.INSTANCE.environment(),
                metadata.getSecurityContext());
        ElementGuard.processElements(ad, asElement());
    }


    // ------------------------------------------------------ JS methods

    @JsProperty(name = "element")
    public JsElement jsElement() {
        return (JsElement) asElement();
    }

    @JsProperty(name = "rows")
    public JsArrayOf<T> jsRows() {
        return JsHelper.asJsArray(getRows());
    }

    @JsProperty(name = "selectedRows")
    public JsArrayOf<T> jsSelectedRows() {
        return JsHelper.asJsArray(selectedRows());
    }

    @JsMethod(name = "update")
    public void jsUpdate(JsArrayOf<T> rows) {
        update(JsHelper.asList(rows));
    }
}
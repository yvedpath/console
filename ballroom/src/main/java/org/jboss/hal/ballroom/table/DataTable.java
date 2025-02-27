/*
 *  Copyright 2022 Red Hat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jboss.hal.ballroom.table;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.elemento.Elements;
import org.jboss.hal.ballroom.JQuery;
import org.jboss.hal.ballroom.form.Form;
import org.jboss.hal.ballroom.table.Api.CallbackUnionType;
import org.jboss.hal.ballroom.table.Api.DrawCallback;
import org.jboss.hal.ballroom.table.Api.SelectCallback;
import org.jboss.hal.meta.security.AuthorisationDecision;
import org.jboss.hal.meta.security.ElementGuard;
import org.jboss.hal.resources.Constants;
import org.jboss.hal.resources.UIConstants;

import com.google.gwt.core.client.GWT;

import elemental2.dom.Element;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLTableElement;
import elemental2.dom.NodeList;

import static elemental2.dom.DomGlobal.document;
import static java.util.Arrays.asList;
import static org.jboss.elemento.Elements.asHtmlElement;
import static org.jboss.elemento.Elements.htmlElements;
import static org.jboss.elemento.Elements.table;
import static org.jboss.elemento.EventType.bind;
import static org.jboss.elemento.EventType.click;
import static org.jboss.hal.ballroom.table.RefreshMode.RESET;
import static org.jboss.hal.resources.CSS.columnAction;
import static org.jboss.hal.resources.CSS.dataTable;
import static org.jboss.hal.resources.CSS.table;
import static org.jboss.hal.resources.CSS.tableBordered;
import static org.jboss.hal.resources.CSS.tableHover;
import static org.jboss.hal.resources.CSS.tableStriped;
import static org.jboss.hal.resources.UIConstants.HASH;
import static org.jboss.hal.resources.UIConstants.data;

/**
 * Table element which implements the DataTables plugin for jQuery. Using the data table consists of these steps:
 * <ol>
 * <li>Create an instance passing an id and an {@linkplain Options options} instance</li>
 * <li>Call {@link #attach()} <strong>after</strong> the data table element was added to the DOM</li>
 * <li>Call any of the {@link Table} methods</li>
 * </ol>
 * <p>
 * Sample which uses a {@code FooBar} as the row type:
 *
 * <pre>
 * class FooBar {
 *     final String foo;
 *     final String bar;
 *
 *     FooBar() {
 *         this.foo = "Foo-" + String.valueOf(Random.nextInt(12345));
 *         this.bar = "Bar-" + String.valueOf(Random.nextInt(12345));
 *     }
 * }
 *
 * Options&lt;FooBar&gt; options = new OptionsBuilder&lt;FooBar&gt;()
 *         .button("Click Me", (table) -> Window.alert("Hello"))
 *         .column("foo", "Foo", (cell, type, row, meta) -> row.foo)
 *         .column("bar", "Bar", (cell, type, row, meta) -> row.baz)
 *         .options();
 * DataTable&lt;FooBar&gt; dataTable = new DataTable&lt;&gt;("sample", options);
 * </pre>
 *
 * @param <T> the row type
 *
 * @see <a href="https://datatables.net/">https://datatables.net/</a>
 */
public class DataTable<T> implements Table<T> {

    private static final Constants CONSTANTS = GWT.create(Constants.class);
    private static final String DESELECT = "deselect";
    private static final String DRAW = "draw";
    private static final String ROW = "row";
    private static final String SELECT = "select";
    private static final String WRAPPER_SUFFIX = "_wrapper";

    private final String id;
    private final Options<T> options;
    private final HTMLTableElement tableElement;
    private Api<T> api;

    public DataTable(String id, Options<T> options) {
        this.id = id;
        this.options = options;
        this.tableElement = table().id(id).css(dataTable, table, tableStriped, tableBordered, tableHover).element();
        if (options.buttons != null && options.buttons.buttons != null) {
            for (Api.Button<T> button : options.buttons.buttons) {
                button.table = this;
            }
        }
        initConstants();
    }

    private void initConstants() {
        this.options.language = new Options.Language();
        this.options.language.aria = new Options.Language.Aria();
        this.options.language.paginate = new Options.Language.Paginate();
        this.options.language.aria.sortAscending = CONSTANTS.dataTablesSortAscending();
        this.options.language.aria.sortDescending = CONSTANTS.dataTablesSortDescending();
        this.options.language.decimal = CONSTANTS.dataTablesDecimal();
        this.options.language.emptyTable = CONSTANTS.dataTablesEmptyTable();
        this.options.language.info = CONSTANTS.dataTablesInfo();
        this.options.language.infoEmpty = CONSTANTS.dataTablesInfoEmpty();
        this.options.language.infoFiltered = CONSTANTS.dataTablesInfoFiltered();
        this.options.language.lengthMenu = CONSTANTS.dataTablesLengthMenu();
        this.options.language.loadingRecords = CONSTANTS.dataTablesLoadingRecords();
        this.options.language.paginate.first = CONSTANTS.dataTablesFirst();
        this.options.language.paginate.last = CONSTANTS.dataTablesLast();
        this.options.language.paginate.next = CONSTANTS.dataTablesNext();
        this.options.language.paginate.previous = CONSTANTS.dataTablesPrevious();
        this.options.language.processing = CONSTANTS.dataTablesProcessing();
        this.options.language.thousands = CONSTANTS.dataTablesThousands();
        this.options.language.zeroRecords = CONSTANTS.dataTablesZeroRecords();
    }

    @Override
    public HTMLElement element() {
        return api == null ? tableElement : (HTMLElement) document.getElementById(id + WRAPPER_SUFFIX);
    }

    /**
     * Initialized the {@link Api} instance using the {@link Options} given at constructor argument. Make sure to call this
     * method before using any of the API methods. It's safe to call the methods multiple times (the initialization will happen
     * only once).
     */
    @Override
    public void attach() {
        if (api == null) {
            options.id = id;
            api = Api.<T> select(HASH + id).dataTable(options);
            api.on(DRAW, CallbackUnionType.of((DrawCallback) (evt, settings) -> {
                Map<String, InlineActionHandler<T>> columnActionHandler = options.columnActionHandler;
                elemental2.dom.Element table = document.getElementById(options.id);
                if (table != null && columnActionHandler != null && !columnActionHandler.isEmpty()) {
                    Elements.stream(table.querySelectorAll("." + columnAction))
                            .filter(htmlElements())
                            .map(asHtmlElement())
                            .forEach(link -> {
                                InlineActionHandler<T> columnAction = columnActionHandler.get(link.id);
                                if (columnAction != null) {
                                    bind(link, click, event -> {
                                        event.stopPropagation();
                                        HTMLElement e = link; // find enclosing tr
                                        while (e != null && e != document.body && !"tr".equalsIgnoreCase(
                                                e.tagName)) {
                                            e = (HTMLElement) e.parentNode;
                                        }
                                        if (e != null) {
                                            T[] array = api.rows(e).data().toArray();
                                            if (array.length != 0) {
                                                columnAction.action(array[0]);
                                            }
                                        }
                                    });
                                }
                            });
                }
            }));
        }
    }

    // ------------------------------------------------------ DataTable API access

    /**
     * Getter for the {@link Api} instance.
     *
     * @return The data tables API
     *
     * @throws IllegalStateException if the API wasn't initialized using {@link #attach()}
     */
    private Api<T> api() {
        if (api == null) {
            throw new IllegalStateException(
                    "DataTable('" + id + "') is not attached. Call DataTable.attach() before using any of the API methods!");
        }
        return api;
    }

    protected JQuery buttonElement(int index) {
        return api().button(index).node();
    }

    // ------------------------------------------------------ 'higher' level API

    @Override
    public void show() {
        HTMLElement wrapper = (HTMLElement) document.getElementById(id + WRAPPER_SUFFIX);
        Elements.setVisible(wrapper, true);
    }

    @Override
    public void hide() {
        HTMLElement wrapper = (HTMLElement) document.getElementById(id + WRAPPER_SUFFIX);
        Elements.setVisible(wrapper, false);
    }

    @Override
    public void enableButton(int index, boolean enable) {
        api().button(index).enable(enable);
    }

    /**
     * Binds a form to the table and takes care to view or clear the form upon selection changes
     */
    @Override
    public void bindForm(Form<T> form) {
        onSelectionChange(table -> {
            if (table.hasSelection()) {
                form.view(table.selectedRow());
            } else {
                form.clear();
            }
        });
    }

    @Override
    public void bindForms(Iterable<Form<T>> forms) {
        onSelectionChange(table -> {
            if (table.hasSelection()) {
                T selectedRow = table.selectedRow();
                for (Form<T> form : forms) {
                    form.view(selectedRow);
                }
            } else {
                for (Form<T> form : forms) {
                    form.clear();
                }
            }
        });
    }

    @Override
    public void clear() {
        api().clear();
    }

    @Override
    public List<T> getRows() {
        SelectorModifier selectorModifier = new SelectorModifierBuilder().page(SelectorModifier.Page.all).build();
        return asList(api().rows(selectorModifier).data().toArray());
    }

    @Override
    public void onSelectionChange(SelectionChangeHandler<T> handler) {
        api().on(SELECT, CallbackUnionType.of((SelectCallback) (event, api, type) -> {
            if (ROW.equals(type)) {
                handler.onSelectionChanged(this);
            }
        }));
        api().on(DESELECT, CallbackUnionType.of((SelectCallback) (event, api, type) -> {
            if (ROW.equals(type)) {
                handler.onSelectionChanged(DataTable.this);
            }
        }));
    }

    @Override
    public T selectedRow() {
        return api().selectedRow();
    }

    @Override
    public List<T> selectedRows() {
        return api().selectedRows();
    }

    @Override
    public void select(T data) {
        select(data, null);
    }

    /**
     * Selects the row with the specified data.
     *
     * @param data the data
     * @param identifier a function which must return an unique identifier for a given row.
     */
    @Override
    public void select(T data, Function<T, String> identifier) {
        if (data != null && identifier != null) {
            String id1 = identifier.apply(data);
            Api.RowSelection<T> rows = (idx, d, tr) -> {
                if (d != null) {
                    String id2 = identifier.apply(d);
                    return (id1 != null && id2 != null) && id1.equals(id2);
                }
                return false;
            };
            api().rows(rows).select();
        }
    }

    /**
     * Replaces the existing data with the new one.
     *
     * @param data the new data
     */
    @Override
    public void update(Iterable<T> data) {
        update(data, RESET, null);
    }

    @Override
    public void update(Iterable<T> data, RefreshMode mode) {
        update(data, mode, null);
    }

    @Override
    public void update(Iterable<T> data, Function<T, String> identifier) {
        update(data, RESET, identifier);
    }

    /**
     * Replaces the existing data with the new one. If necessary, restores the current selection based on the specified
     * function.
     *
     * @param data the new data
     * @param identifier a function which must return an unique identifier for a given row. Used to restore the selection after
     *        replacing the data.
     */
    @Override
    public void update(Iterable<T> data, RefreshMode mode, Function<T, String> identifier) {
        List<T> selection = api().selectedRows();
        api().clear().add(data).draw(mode.mode());
        if (identifier != null) {
            if (!selection.isEmpty()) {
                Api.RowSelection<T> rows = (index, d1, tr) -> {
                    if (d1 != null) {
                        String id1 = identifier.apply(d1);
                        return selection.stream().anyMatch(d2 -> {
                            if (d2 != null) {
                                String id2 = identifier.apply(d2);
                                return (id1 != null && id2 != null) && id1.equals(id2);
                            }
                            return false;
                        });
                    }
                    return false;
                };
                api().rows(rows).select();
            }
        }
    }

    public void applySecurity(Map<Integer, String> buttonConstraints, AuthorisationDecision authorisationDecision) {
        buttonConstraints.forEach((index, constraint) -> {
            buttonElement(index).attr(data(UIConstants.CONSTRAINT), constraint);
        });
        NodeList<Element> elements = element().querySelectorAll("[" + data(UIConstants.CONSTRAINT + "]"));
        Elements.stream(elements).forEach(new ElementGuard.Toggle(authorisationDecision));
    }
}

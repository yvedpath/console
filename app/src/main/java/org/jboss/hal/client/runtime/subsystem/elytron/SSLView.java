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
package org.jboss.hal.client.runtime.subsystem.elytron;

import java.util.List;

import javax.inject.Inject;

import elemental2.dom.HTMLElement;
import org.jboss.hal.ballroom.LabelBuilder;
import org.jboss.hal.ballroom.VerticalNavigation;
import org.jboss.hal.ballroom.form.Form;
import org.jboss.hal.ballroom.table.Button;
import org.jboss.hal.ballroom.table.Table;
import org.jboss.hal.core.mbui.form.ModelNodeForm;
import org.jboss.hal.core.mbui.table.ModelNodeTable;
import org.jboss.hal.core.mvp.HalViewImpl;
import org.jboss.hal.dmr.NamedNode;
import org.jboss.hal.meta.Metadata;
import org.jboss.hal.meta.MetadataRegistry;
import org.jboss.hal.meta.security.Constraint;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;

import static java.util.Arrays.asList;
import static org.jboss.gwt.elemento.core.Elements.h;
import static org.jboss.gwt.elemento.core.Elements.p;
import static org.jboss.gwt.elemento.core.Elements.section;
import static org.jboss.hal.ballroom.LayoutBuilder.column;
import static org.jboss.hal.ballroom.LayoutBuilder.row;
import static org.jboss.hal.client.runtime.subsystem.elytron.AddressTemplates.KEY_MANAGER_TEMPLATE;
import static org.jboss.hal.client.runtime.subsystem.elytron.AddressTemplates.SECURITY_DOMAIN_TEMPLATE;
import static org.jboss.hal.client.runtime.subsystem.elytron.AddressTemplates.TRUST_MANAGER_TEMPLATE;
import static org.jboss.hal.dmr.ModelDescriptionConstants.*;
import static org.jboss.hal.resources.CSS.pfIcon;
import static org.jboss.hal.resources.Ids.FORM;
import static org.jboss.hal.resources.Ids.TABLE;

public class SSLView extends HalViewImpl implements SSLPresenter.MyView {

    private final Table<NamedNode> keyManagerTable;
    private final Form<NamedNode> keyManagerForm;
    private final Table<NamedNode> securityDomainTable;
    private final Form<NamedNode> securityDomainForm;
    private final Table<NamedNode> trustManagerTable;
    private final Form<NamedNode> trustManagerForm;
    private SSLPresenter presenter;

    @Inject
    public SSLView(final MetadataRegistry metadataRegistry, final Resources resources) {

        VerticalNavigation nav = new VerticalNavigation();
        LabelBuilder labelBuilder = new LabelBuilder();

        // ----------------- key manager
        Metadata keyManagerMeta = metadataRegistry.lookup(KEY_MANAGER_TEMPLATE);
        keyManagerTable = new ModelNodeTable.Builder<NamedNode>(Ids.build(KEY_MANAGER, TABLE), keyManagerMeta)
                .button(resources.constants().initialize(),
                        table -> presenter.initKeyManager(table.selectedRow().getName()),
                        Constraint.executable(KEY_MANAGER_TEMPLATE, INIT))
                .column(NAME, (cell, t, row, meta) -> row.getName())
                .build();

        keyManagerForm = new ModelNodeForm.Builder<NamedNode>(Ids.build(KEY_MANAGER, FORM), keyManagerMeta)
                .readOnly()
                .includeRuntime()
                .build();

        HTMLElement keyManagerSection = section()
                .add(h(1).textContent(Names.KEY_MANAGER))
                .add(p().textContent(keyManagerMeta.getDescription().getDescription()))
                .add(keyManagerTable)
                .add(keyManagerForm)
                .asElement();

        nav.addPrimary(Ids.ELYTRON_KEY_MANAGER, Names.KEY_MANAGER, pfIcon("settings"), keyManagerSection);

        // ----------------- key manager
        Metadata secDomainMeta = metadataRegistry.lookup(SECURITY_DOMAIN_TEMPLATE);
        securityDomainTable = new ModelNodeTable.Builder<NamedNode>(Ids.build(SECURITY_DOMAIN, TABLE), secDomainMeta)
                .button(resources.constants().readIdentity(),
                        table -> presenter.readIdentity(secDomainMeta, table.selectedRow().getName()),
                        Constraint.executable(SECURITY_DOMAIN_TEMPLATE, READ_IDENTITY))
                .column(NAME, (cell, t, row, meta) -> row.getName())
                .build();

        securityDomainForm = new ModelNodeForm.Builder<NamedNode>(Ids.build(SECURITY_DOMAIN, FORM), secDomainMeta)
                .readOnly()
                .includeRuntime()
                .build();

        HTMLElement secDomainSection = section()
                .add(h(1).textContent(Names.SECURITY_DOMAIN))
                .add(p().textContent(secDomainMeta.getDescription().getDescription()))
                .add(securityDomainTable)
                .add(securityDomainForm)
                .asElement();

        nav.addPrimary(Ids.ELYTRON_SECURITY_DOMAIN, Names.SECURITY_DOMAIN, pfIcon("cluster"), secDomainSection);

        // ----------------- trust manager
        Metadata trustMeta = metadataRegistry.lookup(TRUST_MANAGER_TEMPLATE);
        trustManagerTable = new ModelNodeTable.Builder<NamedNode>(Ids.build(TRUST_MANAGER, TABLE), trustMeta)
                .button(new Button<>(resources.constants().reloadCRL(), labelBuilder.label(RELOAD_CERTIFICATE_REVOCATION_LIST),
                        table -> presenter.reloadCRL(table.selectedRow().getName()),
                        Constraint.executable(TRUST_MANAGER_TEMPLATE, RELOAD_CERTIFICATE_REVOCATION_LIST)))
                .column(NAME, (cell, t, row, meta) -> row.getName())
                .build();

        trustManagerForm = new ModelNodeForm.Builder<NamedNode>(Ids.build(TRUST_MANAGER, FORM), trustMeta)
                .readOnly()
                .includeRuntime()
                .build();

        HTMLElement trustManagerSection = section()
                .add(h(1).textContent(Names.TRUST_MANAGER))
                .add(p().textContent(trustMeta.getDescription().getDescription()))
                .add(trustManagerTable)
                .add(trustManagerForm)
                .asElement();
        nav.addPrimary(Ids.ELYTRON_TRUST_MANAGER, Names.TRUST_MANAGER, pfIcon("resource-pool"), trustManagerSection);

        registerAttachables(asList(nav, keyManagerTable, keyManagerForm, securityDomainTable, securityDomainForm,
                trustManagerTable, trustManagerForm));

        initElement(row()
                .add(column()
                        .addAll(nav.panes())));
    }

    @Override
    public void attach() {
        super.attach();
        keyManagerTable.bindForm(keyManagerForm);
        keyManagerTable.enableButton(0, false);
        keyManagerTable.onSelectionChange(table -> table.enableButton(0, table.hasSelection()));

        securityDomainTable.bindForm(securityDomainForm);
        securityDomainTable.enableButton(0, false);
        securityDomainTable.onSelectionChange(table -> table.enableButton(0, table.hasSelection()));

        trustManagerTable.bindForm(trustManagerForm);
        trustManagerTable.enableButton(0, false);
        trustManagerTable.onSelectionChange(table -> table.enableButton(0, table.hasSelection()));
    }

    @Override
    public void updateKeyManager(List<NamedNode> items) {
        keyManagerForm.clear();
        keyManagerTable.update(items);
    }

    @Override
    public void updateSecurityDomain(List<NamedNode> items) {
        securityDomainForm.clear();
        securityDomainTable.update(items);
    }

    @Override
    public void updateTrustManager(List<NamedNode> items) {
        trustManagerForm.clear();
        trustManagerTable.update(items);
    }

    @Override
    public void setPresenter(SSLPresenter presenter) {
        this.presenter = presenter;
    }
}
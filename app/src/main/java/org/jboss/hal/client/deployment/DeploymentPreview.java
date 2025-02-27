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
package org.jboss.hal.client.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.elemento.Elements;
import org.jboss.hal.ballroom.LabelBuilder;
import org.jboss.hal.config.Environment;
import org.jboss.hal.core.deployment.Deployment;
import org.jboss.hal.core.deployment.Subdeployment;
import org.jboss.hal.core.finder.PreviewAttributes;
import org.jboss.hal.core.finder.PreviewAttributes.PreviewAttribute;
import org.jboss.hal.core.finder.PreviewContent;
import org.jboss.hal.core.runtime.server.ServerActions;
import org.jboss.hal.core.runtime.server.ServerUrl;
import org.jboss.hal.dmr.ModelNode;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;

import elemental2.dom.HTMLElement;

import static java.util.stream.Collectors.toList;
import static org.jboss.elemento.Elements.a;
import static org.jboss.elemento.Elements.asHtmlElement;
import static org.jboss.elemento.Elements.bag;
import static org.jboss.elemento.Elements.h;
import static org.jboss.elemento.Elements.htmlElements;
import static org.jboss.elemento.Elements.li;
import static org.jboss.elemento.Elements.span;
import static org.jboss.elemento.Elements.stream;
import static org.jboss.elemento.Elements.ul;
import static org.jboss.hal.dmr.ModelDescriptionConstants.CONTENT;
import static org.jboss.hal.dmr.ModelDescriptionConstants.CONTEXT_ROOT;
import static org.jboss.hal.dmr.ModelDescriptionConstants.ENABLED;
import static org.jboss.hal.dmr.ModelDescriptionConstants.EXPLODED;
import static org.jboss.hal.dmr.ModelDescriptionConstants.HASH;
import static org.jboss.hal.dmr.ModelDescriptionConstants.LINK;
import static org.jboss.hal.dmr.ModelDescriptionConstants.MANAGED;
import static org.jboss.hal.dmr.ModelDescriptionConstants.STATUS;
import static org.jboss.hal.dmr.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.hal.dmr.ModelDescriptionConstants.UNDERTOW;
import static org.jboss.hal.dmr.ModelNodeHelper.failSafeBoolean;
import static org.jboss.hal.dmr.ModelNodeHelper.failSafeGet;
import static org.jboss.hal.resources.CSS.marginRight5;
import static org.jboss.hal.resources.Icons.flag;

abstract class DeploymentPreview<T extends ModelNode> extends PreviewContent<T> {

    private final LabelBuilder labelBuilder;
    private final ServerActions serverActions;
    private final Environment environment;
    private final Deployment deployment;

    DeploymentPreview(String header, ServerActions serverActions,
            Environment environment, Deployment deployment) {
        super(header);
        this.serverActions = serverActions;
        this.environment = environment;
        this.deployment = deployment;
        this.labelBuilder = new LabelBuilder();
    }

    /** Adds the (e)nabled, (m)anaged and (e)xploded flags to the specified preview attributes. */
    void eme(PreviewAttributes<T> attributes) {
        // There is only "MANAGED" return at domain server group deployment level resource, no "content".
        if (environment.isStandalone()) {
            attributes.append(model -> {
                String label = String.join(", ",
                        labelBuilder.label(ENABLED), labelBuilder.label(MANAGED), labelBuilder.label(EXPLODED));
                Iterable<HTMLElement> elements = bag()
                        .add(span()
                                .title(labelBuilder.label(ENABLED))
                                .css(flag(failSafeBoolean(model, ENABLED)), marginRight5))
                        .add(span()
                                .title(labelBuilder.label(MANAGED))
                                .css(flag(failSafeBoolean(model, MANAGED)), marginRight5))
                        .add(span()
                                .title(labelBuilder.label(EXPLODED))
                                .css(flag(failSafeBoolean(model, EXPLODED))))
                        .elements();
                return new PreviewAttribute(label, elements);
            });
        } else {
            attributes.append(model -> {
                String label = String.join(", ",
                        labelBuilder.label(ENABLED), labelBuilder.label(MANAGED));
                Iterable<HTMLElement> elements = bag()
                        .add(span()
                                .title(labelBuilder.label(ENABLED))
                                .css(flag(failSafeBoolean(model, ENABLED)), marginRight5))
                        .add(span()
                                .title(labelBuilder.label(MANAGED))
                                .css(flag(failSafeBoolean(model, MANAGED))))
                        .elements();
                return new PreviewAttribute(label, elements);
            });
        }
    }

    void status(PreviewAttributes<T> attributes, Deployment deployment) {
        attributes.append(model -> new PreviewAttribute(labelBuilder.label(STATUS), deployment.getStatus().name()));
    }

    void hash(PreviewAttributes<T> attributes, Deployment deployment) {
        // for compatibility reasons "content" is a list but there is only one element in it
        ModelNode content = deployment.get(CONTENT);
        byte[] hashBytes = content.get(0).get(HASH).asBytes();
        StringBuilder hashString = new StringBuilder();
        for (byte b : hashBytes) {
            int i = b;
            if (i < 0) {
                i = i & 0xff;
            }
            String hex = Integer.toHexString(i);
            if (hex.length() == 1) {
                hashString.append('0');
            }
            hashString.append(hex);
        }

        attributes.append(model -> new PreviewAttribute(labelBuilder.label(HASH), hashString.toString()));
    }

    void subDeployments(Deployment deployment) {
        HTMLElement ul;
        previewBuilder()
                .add(h(2).textContent(Names.SUBDEPLOYMENTS))
                .add(ul = ul().element());
        deployment.getSubdeployments().forEach(
                subdeployment -> ul.appendChild(li().textContent(subdeployment.getName()).element()));
    }

    void contextRoot(PreviewAttributes<T> attributes, Deployment deployment) {
        if (deployment.hasSubsystem(UNDERTOW)) {
            ModelNode contextRoot = failSafeGet(deployment, String.join("/", SUBSYSTEM, UNDERTOW, CONTEXT_ROOT));
            if (contextRoot.isDefined()) {
                attributes.append(model -> new PreviewAttribute(Names.CONTEXT_ROOT,
                        span().textContent(contextRoot.asString())
                                .data(LINK, "").element()));
            }

        } else if (deployment.hasNestedSubsystem(UNDERTOW)) {
            HTMLElement ul = ul().element();
            for (Subdeployment subdeployment : deployment.getSubdeployments()) {
                ModelNode contextRoot = failSafeGet(subdeployment, String.join("/", SUBSYSTEM, UNDERTOW, CONTEXT_ROOT));
                if (contextRoot.isDefined()) {
                    SafeHtml contextHtml = SafeHtmlUtils
                            .fromTrustedString(" <span data-link>" + contextRoot.asString() + "</span>");
                    SafeHtml safeHtml = new SafeHtmlBuilder()
                            .appendEscaped(subdeployment.getName() + " ")
                            .appendHtmlConstant("&rarr;") // NON-NLS
                            .append(contextHtml)
                            .toSafeHtml();
                    ul.appendChild(li().innerHtml(safeHtml).element());
                }
            }
            attributes.append(model -> new PreviewAttribute(Names.CONTEXT_ROOTS, ul));
        }
    }

    @Override
    public void attach() {
        super.attach();
        injectUrls();
    }

    private void injectUrls() {
        List<HTMLElement> linkContainers = new ArrayList<>();
        forEach(e -> {
            List<HTMLElement> elements = stream(e.querySelectorAll("[data-" + LINK + "]")) // NON-NLS
                    .filter(htmlElements())
                    .map(asHtmlElement())
                    .collect(toList());
            linkContainers.addAll(elements);
        });
        if (!linkContainers.isEmpty()) {
            String host = deployment.getReferenceServer().getHost();
            String serverGroup = deployment.getReferenceServer().getServerGroup();
            String server = deployment.getReferenceServer().getName();
            // noinspection Duplicates
            serverActions.readUrl(environment.isStandalone(), host, serverGroup, server,
                    new AsyncCallback<ServerUrl>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            // noop
                        }

                        @Override
                        public void onSuccess(ServerUrl url) {
                            for (HTMLElement linkContainer : linkContainers) {
                                String link = linkContainer.textContent;
                                Elements.removeChildrenFrom(linkContainer);
                                linkContainer.appendChild(a(url.getUrl() + link)
                                        .apply(a -> a.target = Ids.hostServer(host, server))
                                        .textContent(link).element());
                            }
                        }
                    });
        }
    }

}

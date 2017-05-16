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
package org.jboss.hal.client.management;

import org.jboss.gwt.elemento.core.Elements;
import org.jboss.hal.ballroom.Alert;
import org.jboss.hal.core.extension.ExtensionRegistry;
import org.jboss.hal.core.extension.InstalledExtension;
import org.jboss.hal.core.finder.PreviewAttributes;
import org.jboss.hal.core.finder.PreviewContent;
import org.jboss.hal.resources.Icons;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;

import static java.util.Arrays.asList;
import static org.jboss.hal.dmr.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
class ExtensionPreview extends PreviewContent<InstalledExtension> {

    private final ExtensionRegistry extensionRegistry;
    private final Alert scriptOk;
    private final Alert scriptError;

    ExtensionPreview(final InstalledExtension extension,
            final ExtensionRegistry extensionRegistry,
            final Resources resources) {

        super(Names.EXTENSION);
        this.extensionRegistry = extensionRegistry;

        scriptOk = new Alert(Icons.OK, resources.messages().extensionOk());
        scriptError = new Alert(Icons.ERROR, resources.messages().extensionScriptError());

        Elements.setVisible(scriptOk.asElement(), false);
        Elements.setVisible(scriptError.asElement(), false);

        PreviewAttributes<InstalledExtension> attributes = new PreviewAttributes<>(extension,
                asList(NAME, VERSION, DESCRIPTION, SCRIPT, STYLESHEETS, EXTENSION_POINT, AUTHOR, HOMEPAGE, LICENSE))
                .end();

        previewBuilder()
                .add(scriptOk)
                .add(scriptError)
                .addAll(attributes);
    }

    @Override
    public void update(final InstalledExtension extension) {
        extensionRegistry.verifyScript(extension.getFqScript(), status -> {
            boolean scriptOk = status >= 200 && status < 400;
            Elements.setVisible(this.scriptOk.asElement(), scriptOk);
            Elements.setVisible(this.scriptError.asElement(), !scriptOk);
        });
    }
}

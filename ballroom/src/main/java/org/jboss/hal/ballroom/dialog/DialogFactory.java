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
package org.jboss.hal.ballroom.dialog;

import com.google.gwt.safehtml.shared.SafeHtml;
import elemental.dom.Element;
import org.jboss.gwt.elemento.core.Elements;

import static org.jboss.hal.ballroom.dialog.Dialog.Size.SMALL;
import static org.jboss.hal.resources.CSS.centerBlock;
import static org.jboss.hal.resources.CSS.spinner;
import static org.jboss.hal.resources.CSS.spinnerLg;

/**
 * @author Harald Pehl
 */
public final class DialogFactory {

    private DialogFactory() {}

    public static Dialog confirmation(String title, SafeHtml question, Dialog.Callback confirm) {
        return confirmation(title, question, null, confirm);
    }

    public static Dialog confirmation(String title, SafeHtml question, Element element, Dialog.Callback confirm) {
        Element content;
        if (element != null) {
            content = new Elements.Builder().div().p().innerHtml(question).end().add(element).end().build();
        } else {
            content = new Elements.Builder().p().innerHtml(question).end().build();
        }

        return new Dialog.Builder(title)
                .closeIcon(true)
                .closeOnEsc(true)
                .yesNo(confirm)
                .size(SMALL)
                .add(content)
                .build();
    }

    public static BlockingDialog blocking(String title, SafeHtml message) {
        Element element = new Elements.Builder()
                .div().css(centerBlock)
                .p().style("text-align: center").innerHtml(message).end()
                .end()
                .build();

        return new BlockingDialog(new Dialog.Builder(title)
                .size(SMALL)
                .add(element));
    }

    public static BlockingDialog longRunning(String title, SafeHtml message) {
        Element element = new Elements.Builder()
                .div().css(centerBlock)
                .p().style("text-align: center").innerHtml(message).end()
                .div().css(spinner, spinnerLg).end()
                .end()
                .build();

        return new BlockingDialog(new Dialog.Builder(title)
                .size(SMALL)
                .add(element));
    }
}
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.wizards.provision;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.ResourceMappingPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class ProvisionWizardBuilder extends AjaxWizardBuilder<ProvisionTO> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ResourceTO resourceTO;

    private final LoadableDetachableModel<List<String>> anyTypes = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<String> load() {
            final List<String> currentlyAdded = new ArrayList<>();

            CollectionUtils.collect(resourceTO.getProvisions(), new Transformer<ProvisionTO, String>() {

                @Override
                public String transform(final ProvisionTO provisionTO) {
                    return provisionTO.getAnyType();
                }
            }, currentlyAdded);

            final List<String> res = new ArrayList<>();

            CollectionUtils.filter(CollectionUtils.collect(new AnyTypeRestClient().list(),
                    EntityTOUtils.<String, AnyTypeTO>keyTransformer(), res),
                    new Predicate<String>() {

                @Override
                public boolean evaluate(final String key) {
                    return !currentlyAdded.contains(key);
                }
            });

            return res;
        }
    };

    /**
     * The object type specification step.
     */
    private final class ObjectType extends WizardStep {

        private static final long serialVersionUID = 1L;

        private static final String ACCOUNT = "__ACCOUNT__";

        private static final String GROUP = "__GROUP__";

        /**
         * Construct.
         */
        ObjectType(final ProvisionTO item) {
            super(new ResourceModel("type.title", StringUtils.EMPTY),
                    new ResourceModel("type.summary", StringUtils.EMPTY), new Model<ProvisionTO>(item));

            final WebMarkupContainer container = new WebMarkupContainer("container");
            container.setOutputMarkupId(true);
            add(container);

            final FieldPanel<String> type = new AjaxDropDownChoicePanel<String>(
                    "type", "type", new PropertyModel<String>(item, "anyType"), false).
                    setChoices(anyTypes).
                    setStyleSheet("form-control").
                    setRequired(true);
            container.add(type);

            final FormComponent<String> clazz = new TextField<String>(
                    "class", new PropertyModel<String>(item, "objectClass")).setRequired(true);

            container.add(clazz);

            type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (AnyTypeKind.USER.name().equals(type.getModelObject())) {
                        clazz.setModelObject(ACCOUNT);
                        target.add(container);
                    } else if (AnyTypeKind.GROUP.name().equals(type.getModelObject())) {
                        clazz.setModelObject(GROUP);
                        target.add(container);
                    }
                }
            });
        }
    }

    /**
     * Mapping definition step.
     */
    private final class Mapping extends WizardStep {

        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         */
        Mapping(final ProvisionTO item) {
            setTitleModel(new ResourceModel("mapping.title", "Mapping"));
            setSummaryModel(new StringResourceModel("mapping.summary", this, new Model<ProvisionTO>(item)));

            add(new ResourceMappingPanel("mapping", resourceTO, item));
        }
    }

    /**
     * AccountLink specification step.
     */
    private final class ConnObjectLink extends WizardStep {

        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         */
        ConnObjectLink(final ProvisionTO item) {
            super(new ResourceModel("link.title", StringUtils.EMPTY),
                    new ResourceModel("link.summary", StringUtils.EMPTY));

            final WebMarkupContainer connObjectLinkContainer = new WebMarkupContainer("connObjectLinkContainer");
            connObjectLinkContainer.setOutputMarkupId(true);
            add(connObjectLinkContainer);

            boolean connObjectLinkEnabled = false;
            if (StringUtils.isNotBlank(item.getMapping().getConnObjectLink())) {
                connObjectLinkEnabled = true;
            }

            final AjaxCheckBoxPanel connObjectLinkCheckbox = new AjaxCheckBoxPanel(
                    "connObjectLinkCheckbox",
                    new ResourceModel("connObjectLinkCheckbox", "connObjectLinkCheckbox").getObject(),
                    new Model<>(connObjectLinkEnabled),
                    false);
            connObjectLinkCheckbox.setEnabled(true);

            connObjectLinkContainer.add(connObjectLinkCheckbox);

            final AjaxTextFieldPanel connObjectLink = new AjaxTextFieldPanel(
                    "connObjectLink",
                    new ResourceModel("connObjectLink", "connObjectLink").getObject(),
                    new PropertyModel<String>(item.getMapping(), "connObjectLink"),
                    false);
            connObjectLink.setEnabled(connObjectLinkEnabled);
            connObjectLinkContainer.add(connObjectLink);

            connObjectLinkCheckbox.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (connObjectLinkCheckbox.getModelObject()) {
                        connObjectLink.setEnabled(Boolean.TRUE);
                        connObjectLink.setModelObject("");
                    } else {
                        connObjectLink.setEnabled(Boolean.FALSE);
                        connObjectLink.setModelObject("");
                    }

                    target.add(connObjectLink);
                }
            });
        }
    }

    /**
     * Construct.
     *
     * @param id The component id
     * @param resurceTO external resource to be updated.
     * @param pageRef Caller page reference.
     */
    public ProvisionWizardBuilder(final String id, final ResourceTO resurceTO, final PageReference pageRef) {
        super(id, new ProvisionTO(), pageRef);
        this.resourceTO = resurceTO;
    }

    @Override
    protected WizardModel buildModelSteps(final ProvisionTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new ObjectType(modelObject));
        wizardModel.add(new Mapping(modelObject));
        wizardModel.add(new ConnObjectLink(modelObject));
        return wizardModel;
    }

    @Override
    protected Serializable onApplyInternal(final ProvisionTO modelObject) {
        return modelObject;
    }
}

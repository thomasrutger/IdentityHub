/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.publisher.did.local;

import org.eclipse.edc.identithub.did.spi.DidConstants;
import org.eclipse.edc.identithub.did.spi.DidDocumentPublisherRegistry;
import org.eclipse.edc.identithub.did.spi.DidWebParser;
import org.eclipse.edc.identithub.did.spi.store.DidResourceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import static org.eclipse.edc.identityhub.publisher.did.local.LocalDidPublisherExtension.NAME;

@Extension(value = NAME)
public class LocalDidPublisherExtension implements ServiceExtension {
    public static final String NAME = "Local DID publisher extension";
    private static final String DID_CONTEXT_ALIAS = "did";
    private static final String DEFAULT_DID_PATH = "/";
    private static final int DEFAULT_DID_PORT = 10100;
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + DID_CONTEXT_ALIAS)
            .contextAlias(DID_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_DID_PATH)
            .defaultPort(DEFAULT_DID_PORT)
            .useDefaultContext(false)
            .name("DID:WEB Endpoint API")
            .build();
    @Inject
    private DidDocumentPublisherRegistry registry;
    @Inject
    private DidResourceStore didResourceStore;
    @Inject
    private WebService webService;
    @Inject
    private WebServiceConfigurer configurator;
    @Inject
    private WebServer webServer;

    /**
     * Allow extensions to contribute their own DID WEB parser, in case some special url modification is needed.
     */
    @Inject(required = false)
    private DidWebParser didWebParser;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var webServiceConfiguration = configurator.configure(context, webServer, SETTINGS);
        var localPublisher = new LocalDidPublisher(didResourceStore, context.getMonitor());
        registry.addPublisher(DidConstants.DID_WEB_METHOD, localPublisher);
        webService.registerResource(webServiceConfiguration.getContextAlias(), new DidWebController(context.getMonitor(), didResourceStore, getDidParser()));
    }

    private DidWebParser getDidParser() {
        return didWebParser != null ? didWebParser : new DidWebParser();
    }
}
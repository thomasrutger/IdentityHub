/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

plugins {
    `java-library`
    id("application")
}

dependencies {
    runtimeOnly(project(":core:identity-hub-did"))
    runtimeOnly(project(":core:identity-hub-core"))
    runtimeOnly(project(":core:identity-hub-participants"))
    runtimeOnly(project(":core:identity-hub-keypairs"))
    runtimeOnly(project(":extensions:did:local-did-publisher"))
    runtimeOnly(project(":extensions:common:credential-watchdog"))
    runtimeOnly(project(":extensions:protocols:dcp:presentation-api"))
    runtimeOnly(project(":extensions:sts:sts-account-provisioner"))
    runtimeOnly(project(":extensions:sts:sts-account-service-remote"))
    runtimeOnly(project(":extensions:api:identity-api:did-api"))
    runtimeOnly(project(":extensions:api:identity-api:participant-context-api"))
    runtimeOnly(project(":extensions:api:identity-api:verifiable-credentials-api"))
    runtimeOnly(project(":extensions:api:identity-api:keypair-api"))
    runtimeOnly(project(":extensions:api:identity-api:api-configuration"))
    runtimeOnly(project(":extensions:api:identityhub-api-authentication"))
    runtimeOnly(project(":extensions:api:identityhub-api-authorization"))
    runtimeOnly(libs.edc.identity.did.core)
    runtimeOnly(libs.edc.core.token)

    // not needed:
    // runtimeOnly(libs.edc.api.version)

    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.edc.jsonld)
    runtimeOnly(libs.bundles.connector)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}

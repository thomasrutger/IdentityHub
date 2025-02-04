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

package org.eclipse.edc.identityhub.tests.fixtures;

import com.nimbusds.jose.jwk.Curve;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.participantcontext.ApiTokenGenerator;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.security.Vault;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Identity Hub end to end context used in tests extended with {@link IdentityHubEndToEndExtension}
 */
public class IdentityHubEndToEndTestContext {

    public static final String SUPER_USER = "super-user";

    private final EmbeddedRuntime runtime;
    private final IdentityHubRuntimeConfiguration configuration;

    public IdentityHubEndToEndTestContext(EmbeddedRuntime runtime, IdentityHubRuntimeConfiguration configuration) {
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public EmbeddedRuntime getRuntime() {
        return runtime;
    }

    public String createParticipant(String participantContextId) {
        return createParticipant(participantContextId, List.of());
    }

    public String createParticipant(String participantContextId, List<String> roles, boolean isActive) {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(participantContextId)
                .active(isActive)
                .roles(roles)
                .serviceEndpoint(new Service("test-service-id", "test-type", "http://foo.bar.com"))
                .did("did:web:" + participantContextId)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(participantContextId + "-alias")
                        .resourceId(participantContextId + "-resource")
                        .keyId(participantContextId + "-key")
                        .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"))
                        .build())
                .build();
        var srv = runtime.getService(ParticipantContextService.class);
        return srv.createParticipantContext(manifest)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .apiKey();
    }


    public String createParticipant(String participantContextId, List<String> roles) {
        return createParticipant(participantContextId, roles, true);
    }

    public VerifiableCredential createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type("test-type")
                .issuanceDate(Instant.now())
                .issuer(new Issuer("did:web:issuer"))
                .credentialSubject(CredentialSubject.Builder.newInstance().id("id").claim("foo", "bar").build())
                .build();
    }

    public String storeCredential(VerifiableCredential credential, String participantContextId) {
        var resource = VerifiableCredentialResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .state(VcStatus.ISSUED)
                .participantContextId(participantContextId)
                .holderId("holderId")
                .issuerId("issuerId")
                .credential(new VerifiableCredentialContainer("rawVc", CredentialFormat.JWT, credential))
                .build();
        runtime.getService(CredentialStore.class).create(resource).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return resource.getId();
    }


    public String createSuperUser() {
        return createParticipant(SUPER_USER, List.of(ServicePrincipal.ROLE_ADMIN));
    }

    public String storeParticipant(ParticipantContext pc) {
        var store = runtime.getService(ParticipantContextStore.class);

        var vault = runtime.getService(Vault.class);
        var token = createTokenFor(pc.getParticipantContextId());
        vault.storeSecret(pc.getApiTokenAlias(), token);
        store.create(pc).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        return token;
    }

    public IdentityHubRuntimeConfiguration.Endpoint getIdentityApiEndpoint() {
        return configuration.getIdentityApiEndpoint();
    }

    public IdentityHubRuntimeConfiguration.Endpoint getPresentationEndpoint() {
        return configuration.getPresentationEndpoint();
    }

    public Collection<DidDocument> getDidForParticipant(String participantContextId) {
        return runtime.getService(DidDocumentService.class).queryDocuments(QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantContextId", "=", participantContextId))
                .build()).getContent();
    }

    public Collection<KeyPairResource> getKeyPairsForParticipant(String participantContextId) {
        return runtime.getService(KeyPairResourceStore.class).query(ParticipantResource.queryByParticipantContextId(participantContextId).build())
                .getContent();
    }

    public KeyDescriptor createKeyPair(String participantContextId) {

        var descriptor = createKeyDescriptor(participantContextId).build();
        return createKeyPair(participantContextId, descriptor);
    }

    public KeyDescriptor createKeyPair(String participantContextId, KeyDescriptor descriptor) {
        var service = runtime.getService(KeyPairService.class);
        service.addKeyPair(participantContextId, descriptor, true)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return descriptor;
    }

    public KeyDescriptor.Builder createKeyDescriptor(String participantContextId) {
        var keyId = "key-id-%s".formatted(UUID.randomUUID());
        return KeyDescriptor.Builder.newInstance()
                .keyId(keyId)
                .active(false)
                .resourceId(UUID.randomUUID().toString())
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", Curve.P_384.getStdName()))
                .privateKeyAlias("%s-%s-alias".formatted(participantContextId, keyId));
    }

    public ParticipantManifest.Builder createNewParticipant() {
        return ParticipantManifest.Builder.newInstance()
                .participantId("another-participant")
                .active(false)
                .did("did:web:another:participant:" + UUID.randomUUID())
                .serviceEndpoint(new Service("test-service", "test-service-type", "https://test.com"))
                .key(createKeyDescriptor().build());
    }

    public KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .privateKeyAlias("another-alias")
                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                .keyId("another-keyid");
    }

    public String createTokenFor(String userId) {
        return new ApiTokenGenerator().generate(userId);
    }

    public ParticipantContext getParticipant(String participantContextId) {
        return runtime.getService(ParticipantContextService.class)
                .getParticipantContext(participantContextId)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public Optional<VerifiableCredentialResource> getCredential(String credentialId) {
        return runtime.getService(CredentialStore.class)
                .query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", credentialId)).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream().findFirst();
    }

    public DidResource getDidResourceForParticipant(String did) {
        return runtime.getService(DidDocumentService.class).findById(did);
    }


}

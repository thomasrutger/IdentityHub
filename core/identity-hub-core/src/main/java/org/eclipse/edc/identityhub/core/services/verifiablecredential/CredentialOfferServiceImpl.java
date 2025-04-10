/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferObservable;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

public class CredentialOfferServiceImpl implements CredentialOfferService {

    private final CredentialOfferStore credentialOfferStore;
    private final TransactionContext transactionContext;
    private final CredentialOfferObservable credentialOfferObservable;

    public CredentialOfferServiceImpl(CredentialOfferStore credentialOfferStore, TransactionContext transactionContext, CredentialOfferObservable credentialOfferObservable) {
        this.credentialOfferStore = credentialOfferStore;
        this.transactionContext = transactionContext;
        this.credentialOfferObservable = credentialOfferObservable;
    }

    @Override
    public ServiceResult<Void> create(CredentialOffer credentialOffer) {
        return transactionContext.execute(() -> {
            try {
                credentialOfferStore.save(credentialOffer);
                credentialOfferObservable.invokeForEach(l -> l.received(credentialOffer));
                return ServiceResult.success();
            } catch (EdcException ex) {
                return ServiceResult.badRequest(ex.getMessage());
            }
        });
    }
}

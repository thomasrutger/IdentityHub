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

package org.eclipse.edc.identityhub.api.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.validator.spi.ValidationResult;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class CredentialObjectValidator extends JsonValidator {
    private final JsonLdNamespace namespace = DSPACE_DCP_NAMESPACE_V_1_0;

    @Override
    public ValidationResult validate(JsonObject input) {
        if (input == null) {
            return failure(violation("CredentialObject was null", "."));
        }

        var credentialType = input.get(namespace.toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM));
        if (isNullObject(credentialType)) {
            return failure(violation("Must contain a '%s' property.".formatted(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), null));
        }

        return success();
    }

}

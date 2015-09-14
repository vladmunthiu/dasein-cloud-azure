/**
 * Copyright (C) 2013-2014 Dell, Inc
 *
 * ====================================================================
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
 * ====================================================================
 */

package org.dasein.cloud.azure.network;

import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.network.VpnCapabilities;
import org.dasein.cloud.network.VpnProtocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

/**
 * Describes the capabilities of Azure with respect to Dasein vpn operations.
 * User: daniellemayne
 * Date: 05/03/2014
 * Time: 14:34
 */
public class AzureVPNCapabilities extends AbstractCapabilities<Azure> implements VpnCapabilities {
    public AzureVPNCapabilities(@Nonnull Azure provider) {
        super(provider);
    }

    @Nonnull
    @Override
    public Iterable<VpnProtocol> listSupportedVpnProtocols() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public VisibleScope getVpnVisibleScope() {
        return VisibleScope.ACCOUNT_REGION;
    }

    @Override
    public Requirement identifyLabelsRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyVlanIdRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyDataCenterIdRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyGatewayCidrRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyGatewaySharedSecretRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyGatewayBgpAsnRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyGatewayVlanNameRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyGatewayVpnNameRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public boolean supportsAutoConnect() throws CloudException, InternalException {
        return false;
    }
}

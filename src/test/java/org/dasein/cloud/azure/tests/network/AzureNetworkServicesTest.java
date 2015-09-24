/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */

package org.dasein.cloud.azure.tests.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.network.AzureNetworkServices;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Jeffrey Yan on 9/24/2015.
 *
 * @author Jeffrey Yan
 * @since 2015.09.1
 */
public class AzureNetworkServicesTest extends AzureTestsBase {
    private AzureNetworkServices azureNetworkServices;
    @Before
    public void setUp() throws CloudException, InternalException {
        super.setUp();
        azureNetworkServices = new AzureNetworkServices(azureMock);
    }

    @Test
    public void getFirewallSupportShouldReturnNull() {
        assertNull("azureNetworkServices.getFirewallSupport() should return null", azureNetworkServices.getFirewallSupport());
    }

    @Test
    public void getNetworkFirewallSupportShouldReturnNull() {
        assertNull("azureNetworkServices.getNetworkFirewallSupport() should return null", azureNetworkServices.getNetworkFirewallSupport());
    }

    @Test
    public void getDnsSupportShouldReturnNull() {
        assertNull("azureNetworkServices.getDnsSupport() should return null", azureNetworkServices.getDnsSupport());
    }

    @Test
    public void getIpAddressSupportShouldNotReturnNull() {
        assertNotNull("azureNetworkServices.getIpAddressSupport() should not return null",
                azureNetworkServices.getIpAddressSupport());
    }

    @Test
    public void getLoadBalancerSupportShouldNotReturnNull() {
        assertNotNull("azureNetworkServices.getLoadBalancerSupport() should not return null",
                azureNetworkServices.getLoadBalancerSupport());
    }

    @Test
    public void getVlanSupportShouldNotReturnNull() {
        assertNotNull("azureNetworkServices.getVlanSupport() should not return null",
                azureNetworkServices.getVlanSupport());
    }

    @Test
    public void getVpnSupportShouldNotReturnNull() {
        assertNotNull("azureNetworkServices.getVpnSupport() should not return null", azureNetworkServices.getVpnSupport());
    }

    @Test
    public void hasDnsSupportShouldReturnFalse() {
        assertFalse("azureNetworkServices.hasDnsSupport() should return false", azureNetworkServices.hasDnsSupport());
    }

    @Test
    public void hasFirewallSupportShouldReturnFalse() {
        assertFalse("azureNetworkServices.hasFirewallSupport() should return false",
                azureNetworkServices.hasFirewallSupport());
    }

    @Test
    public void hasIpAddressSupportShouldReturnFalse() {
        assertFalse("azureNetworkServices.hasIpAddressSupport() should return false",
                azureNetworkServices.hasIpAddressSupport());
    }

    @Test
    public void hasLoadBalancerSupportShouldReturnTrue() {
        assertTrue("azureNetworkServices.hasLoadBalancerSupport() should return false",
                azureNetworkServices.hasLoadBalancerSupport());
    }

    @Test
    public void hasNetworkFirewallSupportShouldReturnFalse() {
        assertFalse("azureNetworkServices.hasNetworkFirewallSupport() should return false",
                azureNetworkServices.hasNetworkFirewallSupport());
    }

    @Test
    public void hasVpnSupportShouldReturnFalse() {
        assertFalse("azureNetworkServices.hasDnsSupport() should return false", azureNetworkServices.hasVpnSupport());
    }

    @Test
    public void hasVlanSupportShouldReturnTrue() {
        assertTrue("azureNetworkServices.hasVlanSupport() should return false", azureNetworkServices.hasVlanSupport());
    }
}

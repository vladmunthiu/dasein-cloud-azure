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
import org.dasein.cloud.azure.network.AzureIpAddressSupport;
import org.dasein.cloud.azure.network.AzureLoadBalancerSupport;
import org.dasein.cloud.azure.network.AzureNetworkServices;
import org.dasein.cloud.azure.network.AzureVPNSupport;
import org.dasein.cloud.azure.network.AzureVlanSupport;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.network.VpnSupport;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
    public void getIpAddressSupportShouldReturnCorrectResult() {
        IpAddressSupport ipAddressSupport = azureNetworkServices.getIpAddressSupport();
        assertNotNull("azureNetworkServices.getIpAddressSupport() should not return null", ipAddressSupport);
        assertEquals("azureNetworkServices.getIpAddressSupport() should return correct class",
                AzureIpAddressSupport.class, ipAddressSupport.getClass());
    }

    @Test
    public void getLoadBalancerSupportShouldReturnCorrectResult() {
        LoadBalancerSupport loadBalancerSupport = azureNetworkServices.getLoadBalancerSupport();
        assertNotNull("azureNetworkServices.getLoadBalancerSupport() should not return null", loadBalancerSupport);
        assertEquals("azureNetworkServices.getLoadBalancerSupport() should return correct class",
                AzureLoadBalancerSupport.class, loadBalancerSupport.getClass());
    }

    @Test
    public void getVlanSupportShouldReturnCorrectResult() {
        VLANSupport vlanSupport = azureNetworkServices.getVlanSupport();
        assertNotNull("azureNetworkServices.getVlanSupport() should not return null", vlanSupport);
        assertEquals("azureNetworkServices.getVlanSupport() should return correct class", AzureVlanSupport.class,
                vlanSupport.getClass());
    }

    @Test
    public void getVpnSupportShouldReturnCorrectResult() {
        VpnSupport vpnSupport = azureNetworkServices.getVpnSupport();
        assertNotNull("azureNetworkServices.getVpnSupport() should not return null", vpnSupport);
        assertEquals("azureNetworkServices.getVpnSupport() should return correct class",
                AzureVPNSupport.class, vpnSupport.getClass());
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
        assertTrue("azureNetworkServices.hasLoadBalancerSupport() should return true",
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
        assertTrue("azureNetworkServices.hasVlanSupport() should return true", azureNetworkServices.hasVlanSupport());
    }
}

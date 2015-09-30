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

package org.dasein.cloud.azure.tests.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.compute.AzureAffinityGroupSupport;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.compute.disk.AzureDisk;
import org.dasein.cloud.azure.compute.image.AzureOSImage;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VolumeSupport;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Vlad_Munthiu on 6/6/2014.
 */
public class AzureComputeServicesTest extends AzureTestsBase {
    private AzureComputeServices azureComputeServices;

    @Before
    public void setUp() throws CloudException, InternalException {
        super.setUp();
        azureComputeServices = new AzureComputeServices(azureMock);
    }

    @Test
    public void getAffinityGroupSupportShouldReturnCorrectResult() {
        AffinityGroupSupport affinityGroupSupport = azureComputeServices.getAffinityGroupSupport();
        assertNotNull("azureComputeServices.getAffinityGroupSupport() should not return null", affinityGroupSupport);
        assertEquals("azureComputeServices.getAffinityGroupSupport() should return correct class",
                AzureAffinityGroupSupport.class, affinityGroupSupport.getClass());
    }

    @Test
    public void getImageSupportSupportShouldReturnCorrectResult() {
        MachineImageSupport imageSupport = azureComputeServices.getImageSupport();
        assertNotNull("azureComputeServices.getImageSupport() should not return null", imageSupport);
        assertEquals("azureComputeServices.getImageSupport() should return correct class",
                AzureOSImage.class, imageSupport.getClass());
    }

    @Test
    public void getVirtualMachineSupportShouldReturnCorrectResult() {
        VirtualMachineSupport virtualMachineSupport = azureComputeServices.getVirtualMachineSupport();
        assertNotNull("azureComputeServices.getVirtualMachineSupport() should not return null", virtualMachineSupport);
        assertEquals("azureComputeServices.getVirtualMachineSupport() should return correct class",
                AzureVM.class, virtualMachineSupport.getClass());
    }

    @Test
    public void getVolumeSupportShouldReturnCorrectResult() {
        VolumeSupport volumeSupport = azureComputeServices.getVolumeSupport();
        assertNotNull("azureComputeServices.getVolumeSupport() should not return null", volumeSupport);
        assertEquals("azureComputeServices.getVolumeSupport() should return correct class",
                AzureDisk.class, volumeSupport.getClass());
    }

    @Test
    public void hasAffinityGroupSupportShouldReturnTrue() {
        assertTrue("azureComputeServices.hasAffinityGroupSupport() should return true",
                azureComputeServices.hasAffinityGroupSupport());
    }
}

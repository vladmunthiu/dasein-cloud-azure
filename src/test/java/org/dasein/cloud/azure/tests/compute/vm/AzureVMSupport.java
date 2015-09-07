package org.dasein.cloud.azure.tests.compute.vm;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.VirtualMachine;

/**
 * Created by vmunthiu on 9/7/2015.
 */
public class AzureVMSupport extends AzureVM {
    private VirtualMachine virtualMachine;

    public AzureVMSupport(Azure provider) {
        super(provider);
    }

    public AzureVMSupport(Azure provider, VirtualMachine virtualMachine){
        super(provider);
        this.virtualMachine = virtualMachine;
    }

    public VirtualMachine getVirtualMachine(String vmId) {
        return virtualMachine;
    }

}

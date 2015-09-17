package org.dasein.cloud.azure.tests.compute.vm;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

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
        this.setVirtualMachine(virtualMachine);
    }

    public VirtualMachine getVirtualMachine(String vmId) {
        return getVirtualMachine();
    }

    @Override
    public Iterable<VirtualMachineProduct> listProducts(@Nonnull String machineImageId, @Nonnull VirtualMachineProductFilterOptions options) {
        final ArrayList<VirtualMachineProduct> products = new ArrayList<VirtualMachineProduct>();
        CollectionUtils.forAllDo(Arrays.asList("ExtraSmall", "Small", "Medium", "Large", "ExtraLarge"), new Closure() {
            @Override
            public void execute(Object input) {
                VirtualMachineProduct virtualMachineProduct = new VirtualMachineProduct();
                virtualMachineProduct.setProviderProductId((String)input);
                products.add(virtualMachineProduct);
            }
        });
        return products;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public void setVirtualMachine(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }
}

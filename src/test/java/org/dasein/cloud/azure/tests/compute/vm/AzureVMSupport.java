package org.dasein.cloud.azure.tests.compute.vm;

import mockit.MockUp;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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

    @Override
    public Iterable<VirtualMachineProduct> listProducts(VirtualMachineProductFilterOptions options, Architecture architecture) {
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

}

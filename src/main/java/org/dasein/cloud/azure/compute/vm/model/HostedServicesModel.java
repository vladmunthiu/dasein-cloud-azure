package org.dasein.cloud.azure.compute.vm.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by vmunthiu on 10/2/2015.
 */
@XmlRootElement(name="HostedServices", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class HostedServicesModel {
    @XmlElement(name="HostedService", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<HostedServiceModel> hostedServiceModelList;

    public List<HostedServiceModel> getHostedServiceModelList() {
        return hostedServiceModelList;
    }

    public void setHostedServiceModelList(List<HostedServiceModel> hostedServiceModelList) {
        this.hostedServiceModelList = hostedServiceModelList;
    }
}

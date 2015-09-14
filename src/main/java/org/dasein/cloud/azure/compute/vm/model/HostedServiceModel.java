package org.dasein.cloud.azure.compute.vm.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by vmunthiu on 9/10/2015.
 */
@XmlRootElement(name="HostedService")
@XmlAccessorType(XmlAccessType.FIELD)
public class HostedServiceModel {
    @XmlElement(name="Url")
    private String url;
    @XmlElement(name="ServiceName")
    private String serviceName;
    @XmlElement(name="HostedServiceProperties")
    private HostedServicePropertiesModel hostedServicePropertiesModel;

    @XmlRootElement(name="HostedServiceProperties")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HostedServicePropertiesModel {
        @XmlElement(name="Description")
        private String description;
    }
}

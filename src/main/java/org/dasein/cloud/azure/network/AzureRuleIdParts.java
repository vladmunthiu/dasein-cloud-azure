package org.dasein.cloud.azure.network;

import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.Azure;

import javax.annotation.Nonnull;

/**
 * Created by vmunthiu on 7/29/2015.
 */
public class AzureRuleIdParts {
    private String serverId;
    private String protocol;
    private String localPort;

    public static AzureRuleIdParts fromString(@Nonnull String ruleId) throws InternalException {
        if(ruleId == null)
            throw new InternalException("Parameter ruleId cannot be null");

        String[] ruleParts = ruleId.split("_");
        if(ruleParts.length != 3)
            throw new InternalException("Invalid ruleId parameter");

        return new AzureRuleIdParts(ruleParts[0], ruleParts[1], ruleParts[2]);
    }

    public AzureRuleIdParts(String serverId, String protocol, String localPort) {
        this.serverId = serverId;
        this.protocol = protocol.toLowerCase();
        this.localPort = localPort;
    }

    public String toProviderId() {
        return String.format("%s_%s_%s", this.serverId, this.protocol, this.localPort);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        AzureRuleIdParts toCompareWith = (AzureRuleIdParts) obj;
        return this.serverId.equals(toCompareWith.serverId)
                && this.protocol.equals(toCompareWith.protocol)
                && this.localPort.equals(toCompareWith.localPort);
    }

    public String getServerId() {
        return serverId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getLocalPort() {
        return localPort;
    }
}

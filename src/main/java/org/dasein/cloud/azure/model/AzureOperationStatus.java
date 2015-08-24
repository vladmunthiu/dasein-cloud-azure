package org.dasein.cloud.azure.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by vmunthiu on 8/17/2015.
 */
@XmlRootElement(name="Operation", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class AzureOperationStatus {
    @XmlElement(name="Id", namespace ="http://schemas.microsoft.com/windowsazure")
    private String id;
    @XmlElement(name="Status", namespace ="http://schemas.microsoft.com/windowsazure")
    private String status;
    @XmlElement(name="HttpStatusCode", namespace ="http://schemas.microsoft.com/windowsazure")
    private String httpStatusCode;
    @XmlElement(name="Error", namespace ="http://schemas.microsoft.com/windowsazure")
    private AzureOperationError error;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(String httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public AzureOperationError getError() {
        return error;
    }

    public void setError(AzureOperationError error) {
        this.error = error;
    }

    @XmlRootElement(name="Error", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AzureOperationError {
        @XmlElement(name="Code", namespace ="http://schemas.microsoft.com/windowsazure")
        private String code;
        @XmlElement(name="Message", namespace ="http://schemas.microsoft.com/windowsazure")
        private String message;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

/**
 * Copyright (C) 2013-2015 Dell, Inc
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

package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by vmunthiu on 1/14/2015.
 */
@XmlRootElement(name="ServiceResources", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecoverableDatabasesModel {
    @XmlElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<RecoverableDatabaseModel> recoverableDatabaseModels;

    public List<RecoverableDatabaseModel> getRecoverableDatabaseModels() {
        return recoverableDatabaseModels;
    }

    public void setRecoverableDatabaseModels(List<RecoverableDatabaseModel> recoverableDatabaseModels) {
        this.recoverableDatabaseModels = recoverableDatabaseModels;
    }
}

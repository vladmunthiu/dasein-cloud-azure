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

package org.dasein.cloud.azure.tests;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.AzureLocation;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.Region;
import org.junit.Before;

import java.util.Arrays;

/**
 * Created by Jeffrey Yan on 9/16/2015.
 *
 * @author Jeffrey Yan
 * @since 2015.09.1
 */
public class AzureTestsBaseWithLocation extends AzureTestsBase {
    @Mocked
    protected AzureLocation azureLocationMock;

    protected final String REGION_NAME = "test_region_name";

    @Before
    public void setUp() throws CloudException, InternalException {
        super.setUp();

        final Region region = new Region(REGION, REGION_NAME, true, true);
        final DataCenter dataCenter = new DataCenter(REGION, REGION_NAME, REGION, true, true);
        new NonStrictExpectations() {{
            azureMock.getDataCenterServices(); result = azureLocationMock;
            azureLocationMock.getRegion(REGION); result= region;
            azureLocationMock.listDataCenters(REGION); result = Arrays.asList(dataCenter);
            azureLocationMock.getDataCenter(REGION); result = dataCenter;
        }};
    }
}

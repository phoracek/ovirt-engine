/*
Copyright (c) 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.ClusterFeature;
import org.ovirt.engine.api.resource.ClusterEnabledFeatureResource;
import org.ovirt.engine.api.restapi.types.ClusterFeaturesMapper;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.ManagementNetworkOnClusterOperationParameters;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.SupportedAdditionalClusterFeature;
import org.ovirt.engine.core.compat.Guid;

public class BackendClusterEnabledFeatureResource extends AbstractBackendSubResource<ClusterFeature, SupportedAdditionalClusterFeature> implements ClusterEnabledFeatureResource {
    private Guid clusterId;

    public BackendClusterEnabledFeatureResource(Guid clusterId, String id) {
        super(id, ClusterFeature.class, SupportedAdditionalClusterFeature.class);
        this.clusterId = clusterId;
    }

    @Override
    public ClusterFeature get() {
        SupportedAdditionalClusterFeature feature =
                BackendClusterFeatureHelper.getEnabledFeature(this, clusterId, guid);
        if (feature != null) {
            return addLinks(ClusterFeaturesMapper.map(feature.getFeature(), null));
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @Override
    public Response remove() {
        Cluster cluster = BackendClusterFeatureHelper.getClusterWithFeatureDisabled(this, clusterId, guid);
        ManagementNetworkOnClusterOperationParameters param =
                new ManagementNetworkOnClusterOperationParameters(cluster);
        return performAction(ActionType.UpdateCluster, param);
    }

}

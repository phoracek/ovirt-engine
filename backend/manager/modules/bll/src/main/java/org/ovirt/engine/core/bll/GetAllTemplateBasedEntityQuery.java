package org.ovirt.engine.core.bll;

import java.util.List;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.queries.QueryParametersBase;
import org.ovirt.engine.core.dao.VmTemplateDao;

public abstract class GetAllTemplateBasedEntityQuery<P extends QueryParametersBase> extends QueriesCommandBase<P> {
    @Inject
    private VmTemplateDao vmTemplateDao;

    @Inject
    private VmTemplateHandler vmTemplateHandler;

    private final VmEntityType entityType;

    public GetAllTemplateBasedEntityQuery(P parameters, EngineContext engineContext, VmEntityType entityType) {
        super(parameters, engineContext);
        this.entityType = entityType;
    }

    @Override
    protected void executeQueryCommand() {
        List<VmTemplate> retval = vmTemplateDao.getAll(getUserID(), getParameters().isFiltered(), entityType);
        for (VmTemplate template : retval) {
            vmTemplateHandler.updateDisksFromDb(template);
        }
        getQueryReturnValue().setReturnValue(retval);
    }
}

package org.ovirt.engine.api.restapi.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.api.restapi.resource.BackendHostNicsResourceTest.PARENT_GUID;
import static org.ovirt.engine.api.restapi.resource.BackendHostNicsResourceTest.getEntitySpecific;
import static org.ovirt.engine.api.restapi.resource.BackendHostNicsResourceTest.setUpInterfaces;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.ovirt.engine.api.model.HostNic;
import org.ovirt.engine.api.model.Statistic;
import org.ovirt.engine.api.restapi.util.RxTxCalculator;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkStatistics;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendHostNicResourceTest
        extends AbstractBackendSubResourceTest<HostNic, VdsNetworkInterface, BackendHostNicResource> {

    private static final int NIC_IDX = 1;
    private static final Guid NIC_ID = GUIDS[NIC_IDX];

    private static final int SPEED = 50;
    private static final double RECEIVE_RATE = 10;
    private static final double TRANSMIT_RATE = 20;
    private static final double RECEIVE_DROP_RATE = 30;
    private static final double TRANSMIT_DROP_RATE = 40;
    private static final long RECEIVED_BYTES = 50;
    private static final long TRANSMITTED_BYTES = 60;

    private final BackendHostNicsResourceTest hostNicsResource;

    public BackendHostNicResourceTest() {
        super(new BackendHostNicResource(NIC_ID.toString(),
                                         new BackendHostNicsResource(PARENT_GUID.toString())));
        hostNicsResource = new BackendHostNicsResourceTest();
        MockitoAnnotations.initMocks(hostNicsResource);
        hostNicsResource.setUp();
    }

    @Override
    protected void init() {
        super.init();
        initResource(resource.getParent());
    }

    @Override
    protected VdsNetworkInterface getEntity(int index) {
        return getEntitySpecific(index);
    }

    @Test
    public void testBadGuid() throws Exception {
        try {
            new BackendHostNicResource("foo", null);
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testGetNotFound() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(QueryType.GetVdsInterfacesByVdsId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { PARENT_GUID },
                                     new ArrayList<VdsNetworkInterface>());
        try {
            resource.get();
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testGet() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setGetVdsQueryExpectations(1);
        setGetNetworksQueryExpectations(1);
        setUpEntityQueryExpectations();

        hostNicsResource.verifyModelSpecific(resource.get(), NIC_IDX);
    }

    @Test
    public void testGetIncludeStatistics() throws Exception {
        try {
            accepts.add("application/xml; detail=statistics");
            setUriInfo(setUpBasicUriExpectations());
            setGetVdsQueryExpectations(1);
            setGetNetworksQueryExpectations(1);
            setUpEntityQueryExpectations();

            HostNic nic = resource.get();
            assertTrue(nic.isSetStatistics());
            hostNicsResource.verifyModelSpecific(nic, NIC_IDX);
        } finally {
            accepts.clear();
        }
    }

    @Test
    public void testStatisticalQuery() throws Exception {
        VdsNetworkInterface entity = setUpStatisticalExpectations();

        @SuppressWarnings("unchecked")
        BackendStatisticsResource<HostNic, VdsNetworkInterface> statisticsResource =
            (BackendStatisticsResource<HostNic, VdsNetworkInterface>)resource.getStatisticsResource();
        assertNotNull(statisticsResource);

        verifyQuery(statisticsResource.getQuery(), entity);
    }

    protected VdsNetworkInterface setUpStatisticalExpectations() throws Exception {
        VdsNetworkStatistics stats = mock(VdsNetworkStatistics.class);
        VdsNetworkInterface entity = mock(VdsNetworkInterface.class);
        when(entity.getSpeed()).thenReturn(SPEED);
        when(entity.getStatistics()).thenReturn(stats);
        when(entity.getId()).thenReturn(NIC_ID);
        when(stats.getReceiveRate()).thenReturn(RECEIVE_RATE);
        when(stats.getTransmitRate()).thenReturn(TRANSMIT_RATE);
        when(stats.getReceiveDropRate()).thenReturn(RECEIVE_DROP_RATE);
        when(stats.getTransmitDropRate()).thenReturn(TRANSMIT_DROP_RATE);
        when(stats.getReceivedBytes()).thenReturn(RECEIVED_BYTES);
        when(stats.getTransmittedBytes()).thenReturn(TRANSMITTED_BYTES);
        List<VdsNetworkInterface> ifaces = new ArrayList<>();
        ifaces.add(entity);
        setUpEntityQueryExpectations(QueryType.GetVdsInterfacesByVdsId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { PARENT_GUID },
                                     ifaces);
        return entity;
    }

    protected void verifyQuery(AbstractStatisticalQuery<HostNic, VdsNetworkInterface> query, VdsNetworkInterface entity) throws Exception {
        assertEquals(HostNic.class, query.getParentType());
        assertSame(entity, query.resolve(NIC_ID));
        List<Statistic> statistics = query.getStatistics(entity);
        verifyStatistics(statistics,
                new String[] { "data.current.rx", "data.current.tx", "data.current.rx.bps",
                        "data.current.tx.bps", "errors.total.rx", "errors.total.tx",
                        "data.total.rx", "data.total.tx" },
                new BigDecimal[] { asDec(RxTxCalculator.percent2bytes(SPEED, RECEIVE_RATE)),
                        asDec(RxTxCalculator.percent2bytes(SPEED, TRANSMIT_RATE)),
                        asDec(RxTxCalculator.percent2bits(SPEED, RECEIVE_RATE)),
                        asDec(RxTxCalculator.percent2bits(SPEED, TRANSMIT_RATE)),
                        asDec(RECEIVE_DROP_RATE), asDec(TRANSMIT_DROP_RATE),
                        asDec(RECEIVED_BYTES), asDec(TRANSMITTED_BYTES) });
        Statistic adopted = query.adopt(new Statistic());
        assertTrue(adopted.isSetHostNic());
        assertEquals(NIC_ID.toString(), adopted.getHostNic().getId());
        assertTrue(adopted.getHostNic().isSetHost());
        assertEquals(GUIDS[0].toString(), adopted.getHostNic().getHost().getId());
    }

    protected void setUpEntityQueryExpectations() throws Exception {
        setUpEntityQueryExpectations(QueryType.GetVdsInterfacesByVdsId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { PARENT_GUID },
                                     setUpInterfaces());
    }

    protected void setGetVdsQueryExpectations(int times) throws Exception {
        while (times-- > 0) {
            VDS vds = new VDS();
            vds.setClusterId(GUIDS[0]);
            setUpEntityQueryExpectations(QueryType.GetVdsByVdsId,
                    IdQueryParameters.class,
                    new String[] { "Id" },
                    new Object[] { PARENT_GUID },
                    vds);
        }
    }

    protected void setGetNetworksQueryExpectations(int times) throws Exception {
        while (times-- > 0) {
            ArrayList<org.ovirt.engine.core.common.businessentities.network.Network> networks = new ArrayList<>();
            org.ovirt.engine.core.common.businessentities.network.Network network = new org.ovirt.engine.core.common.businessentities.network.Network();
            network.setId(GUIDS[0]);
            network.setName("orcus");
            networks.add(network);
            setUpEntityQueryExpectations(QueryType.GetAllNetworksByClusterId,
                    IdQueryParameters.class,
                    new String[] { "Id" },
                    new Object[] { GUIDS[0] },
                    networks);
        }
    }
}

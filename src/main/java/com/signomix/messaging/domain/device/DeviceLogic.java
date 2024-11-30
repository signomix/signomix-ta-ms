package com.signomix.messaging.domain.device;

import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;
import com.signomix.common.tsdb.IotDatabaseDao;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.logging.Logger;


@ApplicationScoped
public class DeviceLogic {

    @Inject
    Logger LOG;

    @Inject
    @DataSource("oltp")
    AgroalDataSource dataSource;

    IotDatabaseIface dao;


    void onStart(@Observes StartupEvent ev) {
        dao = new IotDatabaseDao();
        dao.setDatasource(dataSource);
    }

    public Device getDevice(String eui){
        try {
            LOG.debug(eui);
            return dao.getDevice(eui, false);
        } catch (IotDatabaseException e) {
            LOG.error("getDevice error",e);
            return null;
        }
    }



}

package org.project.openbaton.common.vnfm_sdk;

import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.catalogue.nfvo.VnfmManagerEndpoint;
import org.project.openbaton.common.vnfm_sdk.interfaces.VNFLifecycleManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by lto on 08/07/15.
 */
public abstract class AbstractVnfm implements VNFLifecycleManagement {
    protected String type;
    protected String endpoint;
    protected Properties properties;
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected VnfmManagerEndpoint vnfmManagerEndpoint;
    protected static final String nfvoQueue = "vnfm-core-actions";

    @PreDestroy
    private void shutdown(){
        this.unregister(vnfmManagerEndpoint);
    }

    @PostConstruct
    private void init(){
        setup();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public abstract CoreMessage instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord);

    @Override
    public abstract void query();

    @Override
    public abstract void scale(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord);

    @Override
    public abstract void checkInstantiationFeasibility();

    @Override
    public abstract void heal();

    @Override
    public abstract void updateSoftware();

    @Override
    public abstract CoreMessage modify(VirtualNetworkFunctionRecord vnfr);

    @Override
    public abstract void upgradeSoftware();

    @Override
    public abstract CoreMessage terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord);

    public abstract CoreMessage handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord);

    protected void loadProperties() {
        Resource resource = new ClassPathResource("conf.properties");
        properties = new Properties();
        try {
            properties.load(resource.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        }
        this.endpoint = (String) properties.get("endpoint");
        this.type = (String) properties.get("type");
    }

    protected void onAction(CoreMessage message) {
        log.trace("VNFM: Received Message: " + message.getAction());
        CoreMessage coreMessage = null;
        switch (message.getAction()){
            case ALLOCATE_RESOURCES:
                break;
            case SCALE:
                this.scale(message.getPayload());
                break;
            case ERROR:
                coreMessage = handleError(message.getPayload());
                break;
            case MODIFY:
                coreMessage = this.modify(message.getPayload());
                break;
            case RELEASE_RESOURCES:
                coreMessage = this.terminate(message.getPayload());
                break;
            case GRANT_OPERATION:
            case INSTANTIATE:
                coreMessage = this.instantiate(message.getPayload());
            case SCALE_UP_FINISHED:
                break;
            case SCALE_DOWN_FINISHED:
                break;
        }


        if (coreMessage != null){
            log.debug("send to NFVO");
            sendToNfvo(coreMessage);
        }
    }

    protected abstract void sendToNfvo(CoreMessage coreMessage);

    protected abstract void unregister(VnfmManagerEndpoint endpoint);

    protected abstract void setup();
}

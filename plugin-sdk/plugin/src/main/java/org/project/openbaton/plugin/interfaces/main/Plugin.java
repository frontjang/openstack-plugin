package org.project.openbaton.plugin.interfaces.main;

import org.project.openbaton.catalogue.nfvo.EndpointType;
import org.project.openbaton.catalogue.nfvo.PluginAnswer;
import org.project.openbaton.catalogue.nfvo.PluginEndpoint;
import org.project.openbaton.catalogue.nfvo.PluginMessage;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.plugin.exceptions.PluginException;
import org.project.openbaton.plugin.interfaces.agents.PluginSender;
import org.project.openbaton.plugin.utils.AgentBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

import javax.jms.MessageListener;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Properties;

/**
 * Created by lto on 13/08/15.
 */
public abstract class Plugin implements MessageListener {

    protected static Logger log = LoggerFactory.getLogger(Plugin.class);

    @Autowired
    private ConfigurableApplicationContext context;

    protected PluginSender pluginSender;

    protected String concurrency;

    protected Object pluginInstance;

    private EndpointType senderType;
    private EndpointType receiverType;

    protected PluginEndpoint endpoint;
    protected String type;

    @Autowired
    private AgentBroker agentBroker;

    protected void loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(pluginInstance.getClass().getResourceAsStream("/plugin.conf.properties"));
        this.senderType = getEndpointType(properties.getProperty("sender-type", "JMS").trim());
        this.receiverType = getEndpointType(properties.getProperty("receiver-type", "JMS").trim());
        this.type = properties.getProperty("type");
//        pluginEndpoint = properties.getProperty("endpoint");
        concurrency = properties.getProperty("concurrency", "1");
        endpoint = new PluginEndpoint();
        endpoint.setEndpoint(properties.getProperty("endpoint"));
        endpoint.setEndpointType(receiverType);
        endpoint.setType(type);
        String classname = pluginInstance.getClass().getSuperclass().getInterfaces()[0].getSimpleName();
        log.debug("classname is: " + classname);
        endpoint.setInterfaceClass(classname);
        log.debug("Loaded properties: " + properties);
    }

    private EndpointType getEndpointType(String trim) {
        log.debug("Endpoint type is: " + trim);
        return EndpointType.valueOf(trim);
//        switch (trim) {
//            case("JMS"):
//                return EndpointType.JMS;
//            case ("REST"):
//                return EndpointType.REST;
//            default:
//                return EndpointType.JMS;
//        }
    }

    protected void setup(){
        setPluginInstance();
        try {
            loadProperties();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        pluginSender = agentBroker.getSender(senderType);
        register();
    }

    public void setPluginInstance(){
        pluginInstance = context.getBean(ClientInterfaces.class);
    }

    protected PluginAnswer onMethodInvoke(PluginMessage pluginMessage) throws PluginException, InvocationTargetException, IllegalAccessException {
        Object result = null;
        if (pluginMessage.getInterfaceClass().getName().equals(pluginInstance.getClass().getSuperclass().getInterfaces()[0].getName())){
            for (Method m : pluginInstance.getClass().getMethods()){
                if (m.getName().equals(pluginMessage.getMethodName())){
                    log.debug("Method name is " + m.getName());
                    log.debug("Method parameter types are: ");
                    for (Type t : m.getParameterTypes()){
                        log.debug("\t*) " + t.toString());
                    }
                    log.debug("Actual Parameters are: " + pluginMessage.getParameters());
                    result =  m.invoke(pluginInstance, pluginMessage.getParameters().toArray());
                }
            }
        }else throw new PluginException("Wrong interface!");

        PluginAnswer pluginAnswer = new PluginAnswer();
        pluginAnswer.setAnswer((Serializable) result);
        return pluginAnswer;
    }

    protected abstract void register();
}

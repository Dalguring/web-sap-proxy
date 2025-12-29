package com.prototype.proxy.config;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Configuration
public class JcoConfig {

    @Value("${sap.jco.destination-name}")
    private String destinationName;

    @Value("${sap.jco.ashost}")
    private String ashost;

    @Value("${sap.jco.sysnr}")
    private String sysnr;

    @Value("${sap.jco.client}")
    private String client;

    @Value("${sap.jco.user}")
    private String user;

    @Value("${sap.jco.passwd}")
    private String passwd;

    @Value("${sap.jco.lang}")
    private String lang;

    @Value("${sap.jco.pool-capacity}")
    private String poolCapacity;

    @Value("${sap.jco.peak-limit}")
    private String peakLimit;

    @Bean
    public JCoDestination jCoDestination() throws JCoException {
        if (!Environment.isDestinationDataProviderRegistered()) {
            InMemoryDestinationDataProvider provider = new InMemoryDestinationDataProvider();

            Properties properties = new Properties();

            properties.setProperty(DestinationDataProvider.JCO_ASHOST, ashost);
            properties.setProperty(DestinationDataProvider.JCO_SYSNR, sysnr);
            properties.setProperty(DestinationDataProvider.JCO_CLIENT, client);
            properties.setProperty(DestinationDataProvider.JCO_USER, user);
            properties.setProperty(DestinationDataProvider.JCO_PASSWD, passwd);
            properties.setProperty(DestinationDataProvider.JCO_LANG, lang);
            properties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, poolCapacity);
            properties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, peakLimit);

            provider.addDestination(destinationName, properties);
            Environment.registerDestinationDataProvider(provider);
            log.info("In-memory JCO destination provider registered");
        }

        JCoDestination destination = JCoDestinationManager.getDestination(destinationName);

        log.info("SAP JCO Destination created: {}", destinationName);
        log.info("Connected to SAP: {}:{}", ashost, sysnr);

        // 연결 테스트
        try {
            destination.ping();
            log.info("SAP connection test successful");
        } catch (JCoException e) {
            log.error("SAP connection test failed", e);
            throw e;
        }

        return destination;
    }

    private static class InMemoryDestinationDataProvider implements DestinationDataProvider {

        private final Map<String, Properties> destinations = new HashMap<>();

        @Override
        public Properties getDestinationProperties(String destinationName) {
            return destinations.get(destinationName);
        }

        @Override
        public boolean supportsEvents() {
            return false;
        }

        @Override
        public void setDestinationDataEventListener(DestinationDataEventListener destinationDataEventListener) {

        }

        public void addDestination(String name, Properties properties) {
            destinations.put(name, properties);
        }
    }
}

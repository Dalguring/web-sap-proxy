package com.prototype.proxy.registry;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.prototype.proxy.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 인터페이스 정의 관리 Registry <br/>애플리케이션 시작 시 YAML 파일들을 로드하여 메모리에 보관
 */

@Slf4j
@Component
public class InterfaceRegistry {

    @Value("${interface.definition-path:classpath:interfaces/}")
    private String definitionPath;
    private final Map<String, InterfaceDefinition> registry = new HashMap<>();
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @PostConstruct
    public void loadDefinitions() {
        log.info("Loading interface definitions from: {}", definitionPath);

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pattern = definitionPath + "*.yml";
            Resource[] resources = resolver.getResources(pattern);

            log.info("Found {} interface definition files", resources.length);

            for (Resource resource : resources) {
                loadDefinition(resource);
            }

            log.info("Loaded {} interface definitions: {}", registry.size(), registry.keySet());
        } catch (IOException e) {
            log.error("Failed to load interface definitions", e);
            throw new RuntimeException("Failed to load interface definitions", e);
        }
    }

    private void loadDefinition(Resource resource) {
        try {
            Map<String, Object> yaml = yamlMapper.readValue(
                resource.getInputStream(),
                Map.class
            );

            Map<String, Object> interfaceData = (Map<String, Object>) yaml.get("interface");

            if (interfaceData == null) {
                log.warn("No 'interface' key found in: {}", resource.getFilename());
                return;
            }

            InterfaceDefinition definition = yamlMapper.convertValue(
                interfaceData,
                InterfaceDefinition.class
            );

            registry.put(definition.getId(), definition);
            log.debug("Loaded interface: {} from {}", definition.getId(), resource.getFilename());
        } catch (IOException e) {
            log.error("Failed to load definition from: {}", resource.getFilename(), e);
        }
    }

    public InterfaceDefinition get(String interfaceId) {
        InterfaceDefinition definition = registry.get(interfaceId.toUpperCase());
        if (definition == null) {
            throw new NotFoundException(interfaceId, "Interface definition not found: " + interfaceId);
        }

        return definition;
    }

    public Map<String, InterfaceDefinition> getAllDefinitions() {
        return new HashMap<>(registry);
    }

    public void reload() {
        log.info("Reloading interface definitions...");
        registry.clear();
        loadDefinitions();
    }

    public boolean exists(String interfaceId) {
        return registry.containsKey(interfaceId.toUpperCase());
    }
}

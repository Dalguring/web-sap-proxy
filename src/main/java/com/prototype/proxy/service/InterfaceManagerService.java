package com.prototype.proxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.prototype.proxy.registry.InterfaceDefinition;
import com.prototype.proxy.registry.InterfaceRegistry;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterfaceManagerService {

    private final InterfaceRegistry registry;

    @Value("${interface.definition-path:src/main/resources/interfaces/}")
    private String definitionPath;
    private final ObjectMapper yamlMapper = new ObjectMapper(
        new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    public void saveInterface(InterfaceDefinition definition) {
        String filename = definition.getId().toUpperCase() + ".yml";
        File file = new File(definitionPath + filename);

        try {
            Map<String, Object> yamlData = Map.of("interface", definition);
            yamlMapper.writeValue(file, yamlData);
            log.info("Interface definition saved: {}", file.getAbsolutePath());

            registry.reload();
        } catch (IOException e) {
            log.error("Failed to save interface definition", e);
            throw new RuntimeException("Failed to save interface file", e);
        }
    }

    public void deleteInterface(String interfaceId) {
        String filename = interfaceId.toUpperCase() + ".yml";
        File file = new File(definitionPath + filename);

        if (file.exists()) {
            if (file.delete()) {
                log.info("Interface definition deleted: {}", filename);
                registry.reload();
            } else {
                throw new RuntimeException("Failed to delete file: " + filename);
            }
        } else {
            throw new RuntimeException("File not found: " + filename);
        }
    }
}

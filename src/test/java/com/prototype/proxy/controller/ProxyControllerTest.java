package com.prototype.proxy.controller;

import com.prototype.proxy.context.RequestContext;
import com.prototype.proxy.exception.NotFoundException;
import com.prototype.proxy.registry.InterfaceDefinition;
import com.prototype.proxy.registry.InterfaceRegistry;
import com.prototype.proxy.service.ProxyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ProxyController.class)
class ProxyControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InterfaceRegistry registry;

    @MockitoBean
    ProxyService proxyService;

    @MockitoBean
    RequestContext requestContext;

    @Test
    @DisplayName("Health Check")
    void health_check() throws Exception {
        given(registry.getAllDefinitions()).willReturn(Map.of(
                "IF1", new InterfaceDefinition(),
                "IF2", new InterfaceDefinition()
        ));

        mockMvc.perform(get("/api/proxy/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Interface Proxy Server"))
                .andExpect(jsonPath("$.loadedInterfaces").value(2))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("존재하는 인터페이스 전체 목록을 반환한다")
    void list_interfaces() throws Exception {
        given(registry.getAllDefinitions()).willReturn(Map.of(
                "WORK_ORDER", new InterfaceDefinition(),
                "STOCK_MOVEMENT", new InterfaceDefinition()
        ));

        mockMvc.perform(get("/api/proxy/interfaces"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.interfaces.WORK_ORDER").exists())
                .andExpect(jsonPath("$.interfaces.STOCK_MOVEMENT").exists());
    }

    @Test
    @DisplayName("존재하는 인터페이스 ID로 조회 시 상세 정보를 반환한다")
    void getInterface_success() throws Exception {
        String interfaceId = "STOCK_MOVEMENT";
        InterfaceDefinition mockDef = new InterfaceDefinition();
        mockDef.setId(interfaceId);
        mockDef.setName("재고이동 전송");
        mockDef.setDescription("WMS → SAP 재고 이동 데이터 전송");
        mockDef.setRfcFunction("IF_MOVE_STOCK");

        given(registry.get(interfaceId)).willReturn(mockDef);

        mockMvc.perform(get("/api/proxy/interfaces/{interfaceId}", interfaceId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value("STOCK_MOVEMENT"))
                .andExpect(jsonPath("$.name").value("재고이동 전송"))
                .andExpect(jsonPath("$.description").value("WMS → SAP 재고 이동 데이터 전송"))
                .andExpect(jsonPath("$.rfcFunction").value("IF_MOVE_STOCK"));
    }

    @Test
    @DisplayName("존재하지 않는 인터페이스 ID로 조회 시 404를 반환한다")
    void getInterface_not_found() throws Exception {
        String invalidId = "INVALID_ID";
        String errorMessage = "Interface definition not found: " + invalidId;

        given(registry.get(invalidId))
                .willThrow(new NotFoundException(invalidId, errorMessage));

        mockMvc.perform(get("/api/proxy/interfaces/{interfaceId}", invalidId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.data.interfaceId").value(invalidId))
                .andExpect(jsonPath("$.data.errorType").value("NOT_FOUND"));
    }
}

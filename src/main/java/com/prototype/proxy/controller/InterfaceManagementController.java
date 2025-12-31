package com.prototype.proxy.controller;

import com.prototype.proxy.registry.InterfaceDefinition;
import com.prototype.proxy.service.InterfaceManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/interfaces")
@RequiredArgsConstructor
@Tag(name = "Admin - 인터페이스 관리", description = "인터페이스 생성, 수정, 삭제")
public class InterfaceManagementController {

    private final InterfaceManagerService managerService;

    @Operation(summary = "인터페이스 저장/수정", description = "YAML 파일을 생성하고 리로드합니다.")
    @PostMapping("/save")
    public ResponseEntity<String> saveInterface(@RequestBody InterfaceDefinition definition) {
        managerService.saveInterface(definition);
        return ResponseEntity.ok("Saved and reloaded successfully");
    }

    @Operation(summary = "인터페이스 삭제", description = "YAML 파일을 삭제하고 리로드합니다.")
    @DeleteMapping("/{interfaceId}")
    public ResponseEntity<String> deleteInterface(@PathVariable String interfaceId) {
        managerService.deleteInterface(interfaceId);
        return ResponseEntity.ok("Deleted and reloaded successfully");
    }
}

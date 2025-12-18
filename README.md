# SAP RFC Proxy Server

Spring Boot 기반 SAP RFC 호출 전용 Proxy 서버입니다.
WEB / Backend 시스템이 SAP RFC(JCO)를 직접 호출하지 않고
HTTP 기반 표준 인터페이스로 SAP와 통신할 수 있도록 중계합니다.

---

## Overview

### 목적

* SAP RFC 호출 로직 중앙화
* SAP 연동 표준화 (요청/응답, 에러 처리)
* WEB ↔ SAP 간 결합도 감소
* 인터페이스 변경 시 코드 수정 최소화

### 주요 특징

* Spring Boot 기반 REST API
* SAP JCO를 이용한 실제 RFC 호출
* YAML 기반 인터페이스 정의 및 매핑
* Stateless Proxy 구조
* 요청/응답 전문 로깅 지원

---

## Architecture

```text
[ WEB / Backend ]
        |
        | HTTP (JSON)
        v
+----------------------+
|  SAP RFC Proxy       |
|  (Spring Boot)       |
+----------------------+
        |
        | JCO (RFC)
        v
[ SAP System ]
```

### Processing Flow

1. HTTP 요청 수신
2. interfaceId 기준 인터페이스 정의 조회
3. 요청 데이터 → SAP RFC 파라미터 매핑
4. SAP RFC 실행 (JCO)
5. 응답 데이터 정규화
6. 결과 반환 및 로그 저장

---

## Core Components

| Component         | Description         |
| ----------------- | ------------------- |
| Controller        | Proxy API 제공, 요청 검증 |
| ProxyService      | 전체 처리 흐름 제어         |
| InterfaceRegistry | YAML 인터페이스 정의 로드    |
| MappingEngine     | 요청/응답 매핑            |
| RfcExecutor       | SAP RFC 실행 전담       |
| JcoConfig         | SAP JCO 설정          |

---

## Interface Definition (YAML)

```yaml
interfaceId: WORK_ORDER_CREATE
sapFunction: Z_RFC_WORK_ORDER_CREATE

import:
  PLANT: plantCode
  ORDER_TYPE: orderType

tables:
  IT_ITEMS:
    - MATERIAL: materialCode
      QUANTITY: quantity
```

장점

* SAP 인터페이스 변경에 유연
* 허용된 RFC만 호출 가능
* 운영 중 무중단 확장 가능

---

## API Example

### Request

```http
POST /api/proxy/execute
Content-Type: application/json
```

```json
{
  "interfaceId": "WORK_ORDER_CREATE",
  "data": {
    "plantCode": "1000",
    "orderType": "PP01",
    "items": [
      {
        "materialCode": "MAT001",
        "quantity": 10
      }
    ]
  },
  "requestId": "REQ-20241217-0001"
}
```

---

### Response

```json
{
  "success": true,
  "data": {
    "export": {
      "ORDER_NO": "4500001234"
    },
    "tables": {},
    "return": [
      {
        "TYPE": "S",
        "MESSAGE": "SUCCESS"
      }
    ]
  },
  "requestId": "REQ-20241217-0001",
  "executionTimeMs": 320
}
```

---

## Error Handling Policy

| Type      | Description            | HTTP Status |
| --------- | ---------------------- | ----------- |
| Technical | JCO Exception, Timeout | 500         |
| Business  | SAP RETURN TYPE = E    | 200         |

---

## Logging & Monitoring

* requestId 기준 요청/응답 추적
* 인터페이스 ID / SAP Function 단위 로깅
* RFC 실행 시간(ms) 기록

---

## Roadmap

* RFC Retry / Timeout 정책
* 인터페이스 버저닝 관리
* 보안 강화 (IP Whitelist, 인증 토큰)
* 대량 호출 성능 최적화
* 모니터링 대시보드 연동

---

## Notes

* SAP RFC 호출 전용 Proxy 서버입니다.
* 인터페이스 정의 변경 시 YAML 수정 필요
* 운영 환경에서는 접근 제어가 필요합니다.

---

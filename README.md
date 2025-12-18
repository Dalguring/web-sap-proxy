SAP RFC Proxy Server

Spring Boot 기반 SAP RFC 호출 전용 Proxy 서버입니다.
기존 WEB/백엔드 시스템이 SAP RFC(JCO)를 직접 호출하지 않고,
HTTP 기반으로 안전하고 일관된 방식으로 SAP와 통신할 수 있도록 중간 계층 역할을 수행합니다.

1. 프로젝트 개요
목적

SAP RFC 호출 로직의 중앙화

SAP 연동 표준화 (요청/응답, 에러 처리, 로깅)

WEB 시스템과 SAP 간 결합도 감소

인터페이스 정의 변경 시 코드 수정 최소화

특징

Spring Boot 기반 REST API

SAP JCO를 이용한 RFC 실제 호출

YAML 기반 인터페이스 정의 및 매핑

무상태(Stateless) Proxy 구조

요청/응답 전문 로깅 지원

2. 전체 아키텍처
[ WEB / Backend ]
        |
        | HTTP (JSON)
        v
[ SAP RFC Proxy Server ]
        |
        | JCO (RFC)
        v
[ SAP System ]

처리 흐름

WEB 시스템에서 HTTP 요청 수신

Interface ID 기준 YAML 정의 조회

요청 데이터 → SAP RFC 파라미터 매핑

SAP RFC 실행 (JCO)

SAP 응답 → 표준 응답 구조로 변환

결과 반환 및 실행 로그 저장

3. 주요 구성 요소
Controller

Proxy API 엔드포인트 제공

요청 유효성 검증 및 requestId 관리

ProxyService

전체 비즈니스 흐름 제어

인터페이스 정의 조회

매핑 엔진 및 RFC 실행 호출

InterfaceRegistry

애플리케이션 시작 시 YAML 인터페이스 정의 로드

interfaceId → InterfaceDefinition 관리

MappingEngine

요청 JSON → SAP Import/Table 파라미터 변환

SAP 응답 → 표준 Map 구조 변환

RfcExecutor

SAP JCO 기반 RFC 실행 전담

Function 호출, 파라미터 설정, 결과 수집

비즈니스 로직 미포함 (순수 실행 책임)

JcoConfig

SAP JCO Destination 설정

연결 정보 및 Ping 체크

4. 인터페이스 정의 (YAML)

SAP RFC 호출 정보는 코드가 아닌 YAML로 관리합니다.

interfaceId: WORK_ORDER_CREATE
sapFunction: Z_RFC_WORK_ORDER_CREATE

import:
  PLANT: plantCode
  ORDER_TYPE: orderType

tables:
  IT_ITEMS:
    - MATERIAL: materialCode
      QUANTITY: quantity

장점

SAP 인터페이스 변경 시 코드 수정 불필요

허용된 RFC만 노출 가능

운영 중 무중단 변경 가능

5. API 예시
요청
POST /api/proxy/execute
Content-Type: application/json

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

응답
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

6. 에러 처리 정책
에러 구분
유형	설명
기술 오류	SAP 연결 실패, Timeout, JCO Exception
업무 오류	SAP RETURN 테이블의 TYPE = E

기술 오류: HTTP 500

업무 오류: HTTP 200 + success=false

7. 설정 정보
application.yml
sap:
  jco:
    ashost: xxx.xxx.xxx.xxx
    sysnr: "00"
    client: "100"
    user: ${SAP_USER}
    passwd: ${SAP_PASSWORD}
    lang: KO


민감 정보는 환경 변수 또는 별도 설정 파일로 관리합니다.

8. 로그 및 모니터링

requestId 기준 요청/응답 전문 로깅

RFC 실행 시간(ms) 기록

인터페이스 ID / SAP Function 단위 추적 가능

9. 개발 및 실행
./gradlew bootRun

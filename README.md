# Interface Proxy Server

Spring Boot 4.0 및 Java 21 기반의 **SAP RFC 호출 전용 Proxy 서버**입니다.<br/>
웹 시스템이 SAP JCo 라이브러리를 직접 설치하지 않고도 표준 HTTP REST API를 통해 SAP RFC를 호출하고 결과를 받을 수 있도록 중계합니다.

---

## 1. 개요

### 목적
* **SAP 연동 표준화**: HTTP 기반의 표준 인터페이스(JSON)를 제공하여 SAP 시스템과의 통신 방식을 통일합니다.
* **로직 중앙화**: SAP RFC 호출 및 매핑 로직을 Proxy 서버에서 통합 관리합니다.
* **유연성 확보**: GUI를 통해 인터페이스를 정의하고 자동 생성 된 YAML 설정을 통해 코드 수정 없이 새로운 인터페이스를 정의하고 수정할 수 있습니다.
* **결합도 감소**: 호출 시스템에서 SAP 종속적인 라이브러리(JCo) 설치 및 구성을 배제할 수 있습니다.

### 주요 특징
* **YAML 기반 매핑**: SAP 파라미터와 HTTP 요청/응답 필드 간의 매핑을 YAML 파일로 관리합니다.
* **Full-Stack 구조**: React 기반의 프런트엔드 관리 도구를 포함하여 인터페이스 및 로그를 관리합니다.
* **실시간 로깅 및 모니터링**: 모든 RFC 요청/응답 전문과 실행 시간을 기록합니다.
* **Swagger(OpenAPI) 지원**: API 명세 자동화를 통해 연동 편의성을 제공합니다.

---

## 2. 기술 스택 (Tech Stack)

### Backend
* **Language**: Java 21
* **Framework**: Spring Boot 4.0.0
* **Library**: SAP JCo 3.x (`lib/sapjco3.jar`), Spring Data JPA, Lombok
* **Database**: H2 (로컬), PostgreSQL (개발, 운영)
* **API Doc**: Springdoc-OpenAPI 3.0.0

### Frontend
* **Library**: React 19, TypeScript
* **Build Tool**: Vite 7
* **Styling**: Tailwind CSS

---

## 3. 시스템 아키텍처 (Architecture)

```text
[ WEB / Backend ] <--- HTTP (JSON) ---> [ Interface Proxy Server ] <--- JCO (RFC) ---> [ SAP System ]
```

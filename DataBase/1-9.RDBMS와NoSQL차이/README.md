# RDBMS와 NoSQL의 차이점은 무엇일까요

RDBMS와 NoSQL(Not Only SQL)은 데이터를 어떻게 모델링하고 얼마나 강한 일관성을 보장할지에서 서로 다른 선택을 한 저장소다.

- RDBMS: 정해진 스키마의 테이블을 관계(Relation)로 묶고 ACID로 정합성 보장
- NoSQL: 키-값·문서·컬럼·그래프 등 다양한 모델을 쓰고, 대신 일관성을 일부 양보

## 핵심 차이 요약

|    축    |           RDBMS           |                NoSQL                |
|:-------:|:-------------------------:|:-----------------------------------:|
| 데이터 모델  |       테이블(행·열) + 관계       | Key-Value, Document, Column, Graph  |
|   스키마   |       사전 정의 (Rigid)       |   유연 (Flexible, 문서마다 구조 다를 수 있음)    |
| 트랜잭션 모델 |  ACID (강한 일관성, 다중 행·테이블)  |       BASE (최종 일관성, 제한적 트랜잭션)       |
|  질의 언어  |          SQL 표준           |   벤더별 독자 API/쿼리 언어 (ex. MQL, CQL)   |
|   조인    |       관계 모델의 핵심 연산        |  제한적 또는 미지원, 대신 임베딩(Embedding) 권장   |
| 주요 구현체  | MySQL, PostgreSQL, Oracle | MongoDB, Redis, Cassandra, DynamoDB |

## 데이터 모델의 차이

### RDBMS - 정규화된 관계 모델

중복 없이 여러 테이블에 쪼개 저장하고, 필요할 때 조인으로 다시 묶어 조회한다.

```sql
-- 사용자, 주문, 주문 항목이 3개 테이블로 정규화
CREATE TABLE users
(
    id   BIGINT PRIMARY KEY,
    name VARCHAR(50)
);
CREATE TABLE orders
(
    id      BIGINT PRIMARY KEY,
    user_id BIGINT REFERENCES users (id),
    total   DECIMAL
);
CREATE TABLE order_items
(
    order_id   BIGINT REFERENCES orders (id),
    product_id BIGINT,
    quantity   INT
);

-- 조회 시 조인 필요
SELECT u.name, o.total, oi.quantity
FROM users u
         JOIN orders o ON u.id = o.user_id
         JOIN order_items oi ON o.id = oi.order_id
WHERE u.id = 1;
```

### NoSQL - 임베딩 모델 (Document DB 예시)

관련된 데이터를 하나의 문서에 묶어 담아, 한 번의 조회로 필요한 정보를 전부 가져온다.

```json
{
  "_id": 1,
  "name": "Alice",
  "orders": [
    {
      "id": 1001,
      "total": 50000,
      "items": [
        {
          "product_id": 201,
          "quantity": 2
        },
        {
          "product_id": 305,
          "quantity": 1
        }
      ]
    }
  ]
}
```

- 조인 불필요, 단일 문서 접근으로 완결
- 반복되는 정보(제품명 등)의 중복 저장으로 정합성 유지 비용 발생
- 읽기에 최적화되지만 쓰기 시 중복 갱신 부담

## 일관성 모델 - ACID vs BASE

RDBMS와 NoSQL의 가장 근본적인 차이는 데이터 무결성과 가용성 사이에서 어떤 트레이드오프를 선택했는지에 있다.

|   ACID (RDBMS)    |         BASE (NoSQL)          |
|:-----------------:|:-----------------------------:|
|  Atomicity (원자성)  |   Basically Available (가용성)   |
| Consistency (일관성) |      Soft State (유연 상태)       |
|  Isolation (격리성)  | Eventual Consistency (최종 일관성) |
| Durability (지속성)  |                               |

### ACID

ACID는 강한 정합성을 보장하지만, 서버가 여러 대로 분산되면 그 비용이 커진다.

- Atomicity: 여러 행·테이블을 바꾸는 작업은 모두 성공하거나 모두 실패
- Consistency: 제약 조건(FK, UNIQUE, CHECK)이 언제나 유지
- Isolation: 동시에 실행되는 트랜잭션끼리 서로 간섭하지 않음 (격리 수준으로 조절)
- Durability: 한 번 커밋된 데이터는 장애가 나도 사라지지 않음
- 비용: 노드가 여러 대로 나뉘면 2PC(Two-Phase Commit) 같은 동기화 과정에서 지연과 가용성을 희생

### BASE

BASE는 분산 환경에서 ACID를 일부 포기한 대신, 가용성과 확장성을 우선시하는 모델이다.

- Basically Available: 장애가 나도 시스템은 일단 어떤 형태로든 응답 (부분적 실패 허용)
- Soft State: 외부 입력이 없어도 복제본 동기화 과정에서 상태가 시간에 따라 바뀔 수 있음
- Eventual Consistency: 시간이 지나면 결국 모든 복제본이 같은 값으로 수렴
- 비용: 방금 읽은 값이 최신이 아닐 수 있어, 필요하면 애플리케이션 수준에서 보정

## NoSQL의 종류

NoSQL은 하나의 기술이 아니라, 서로 다른 데이터 모델을 쓰는 여러 계열을 묶어 부르는 이름이다.

|         종류          |           특징           |         대표         |       용도        |
|:-------------------:|:----------------------:|:------------------:|:---------------:|
|   Key-Value Store   |   단순 키로 값 조회, 매우 빠름    |  Redis, DynamoDB   | 캐시, 세션, 실시간 랭킹  |
|   Document Store    | JSON/BSON 문서, 중첩 구조 지원 | MongoDB, Couchbase |  콘텐츠 관리, 카탈로그   |
| Column-Family Store |  컬럼 단위 저장, 대용량 쓰기 최적화  |  Cassandra, HBase  | 시계열 데이터, 로그 분석  |
|     Graph Store     |   노드·엣지 기반, 관계 탐색 특화   |  Neo4j, ArangoDB   | 소셜 네트워크, 추천 시스템 |

## 선택 기준

RDBMS와 NoSQL은 서로 다른 트레이드오프를 가진 도구이므로, 애플리케이션의 요구사항에 따라 적절히 선택해야 한다.

- RDBMS가 적합한 경우
    - 강한 정합성이 필수 (금융, 결제, 재고)
    - 스키마가 안정적이고 명확한 관계가 존재
    - 복잡한 조인·집계 쿼리가 빈번
- NoSQL이 적합한 경우
    - 쓰기 처리량이 매우 높음 (로그, 이벤트 스트림)
    - 데이터 구조가 가변적이거나 문서 단위 접근이 자연스러움

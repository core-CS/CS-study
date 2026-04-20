# 인덱스에 어떤 컬럼을 사용해야 할까요?

> 인덱스를 잘못 설계하면 없는 것보다 느려질 수 있습니다.
> 어떤 컬럼에 인덱스를 걸어야 하는지, 판단 기준을 정리합니다.

---

## 핵심 한 줄 요약

> **"자주 조회되고, 값이 다양하고, 변경이 적은 컬럼"** 에 인덱스를 건다.

```mermaid
flowchart TD
    Q["이 컬럼에 인덱스를 걸까?"]

    Q --> A{"WHERE / JOIN / ORDER BY에자주 등장하는가?"}
    A -->|"No"| SKIP["인덱스 불필요(조회에 쓰이지 않음)"]
    A -->|"Yes"| B{"Cardinality가높은가?"}

    B -->|"No (성별, 상태값 등)"| C{"전체 데이터 대비\n조회 비율이 낮은가?"}
    C -->|"No (50% 이상 반환)"| SKIP2["Full Scan이 더 빠름\n인덱스 불필요"]
    C -->|"Yes (특정 값만 조회)"| PARTIAL["부분 인덱스 / 복합 인덱스\n고려"]

    B -->|"Yes (이메일, 주문번호 등)"| D{"데이터 변경이\n잦은 컬럼인가?"}
    D -->|"Yes (수시로 UPDATE)"| WARN["인덱스 유지 비용 주의\n쓰기 성능 저하 가능"]
    D -->|"No"| OK["인덱스 적합!"]
```

---

## 1. 카디널리티 (Cardinality) — 값이 얼마나 다양한가

### 개념

컬럼이 가진 **고유한 값의 수**입니다.
카디널리티가 높을수록 인덱스로 좁힐 수 있는 범위가 넓어져 **탐색 효율이 좋습니다**.

### 일상 예시

> 도서관에서 책을 찾는다고 상상해봅시다.
>
> - **"언어별 분류"** 로 찾기 → 한국어/영어/일어 3가지뿐 → 여전히 수천 권을 뒤져야 함 (카디널리티 낮음)
> - **"ISBN 번호"** 로 찾기 → 모든 책마다 고유 번호 → 바로 한 권 특정 가능 (카디널리티 높음)

```mermaid
graph LR
    subgraph Low["카디널리티 낮음 (인덱스 효과 낮음)"]
        direction TB
        G1["gender = '남'<br/>→ 500만 행 반환"]
        G2["status = 'active'<br/>→ 80만 행 반환"]
    end

    subgraph High["카디널리티 높음 (인덱스 효과 높음)"]
        direction TB
        H1["email = 'a@b.com'<br/>→ 1행 반환"]
        H2["order_no = 'ORD-20240101-001'<br/>→ 1행 반환"]
    end
```

### 코드 예시

```sql
-- 카디널리티 확인 쿼리
SELECT
    'gender'    AS col, COUNT(DISTINCT gender)    AS cardinality, COUNT(*) AS total FROM users
UNION ALL
SELECT
    'city'      AS col, COUNT(DISTINCT city)      AS cardinality, COUNT(*) AS total FROM users
UNION ALL
SELECT
    'email'     AS col, COUNT(DISTINCT email)     AS cardinality, COUNT(*) AS total FROM users;

/*
결과 예시 (users 100만 행):
col       | cardinality | total
----------+-------------+---------
gender    |           2 | 1000000   ← 인덱스 효과 낮음
city      |         250 | 1000000   ← 보통
email     |     1000000 | 1000000   ← 인덱스 효과 높음
*/
```

---

## 2. 선택도 (Selectivity) — 인덱스가 얼마나 걸러내는가

### 개념

```
선택도 = 고유값 수 / 전체 행 수   (0 ~ 1)
```

- 선택도가 **1에 가까울수록** 인덱스가 효과적입니다.
- 선택도가 **낮을수록** (0에 가까울수록) Full Scan이 더 빠를 수 있습니다.
- 일반적으로 **5% 이하**인 컬럼에 인덱스를 권장합니다.

### 일상 예시

> 택배 회사에서 배송지를 찾는다고 상상해봅시다.
>
> - **"도 단위(경기도)"** 로 찾기 → 절반 이상의 주소가 해당 → 사실상 전부 뒤짐 (선택도 낮음)
> - **"상세 주소 + 동호수"** 로 찾기 → 딱 한 곳만 해당 → 바로 특정 가능 (선택도 높음)

```mermaid
graph TB
    subgraph T["전체 주문 100만 건"]
        A["선택도 50%<br/>WHERE status = 'paid'<br/>→ 50만 건 반환<br/>Full Scan이 유리"]
        B["선택도 0.0001%<br/>WHERE order_no = 'ORD-001'<br/>→ 1건 반환<br/>Index Scan이 유리"]
    end
```

### 코드 예시

```sql
-- 선택도 계산
SELECT
    column_name,
    cardinality,
    table_rows,
    ROUND(cardinality / table_rows * 100, 2) AS selectivity_pct
FROM information_schema.STATISTICS
JOIN information_schema.TABLES USING (table_schema, table_name)
WHERE table_name = 'orders'
  AND table_schema = 'mydb';

/*
column_name | cardinality | table_rows | selectivity_pct
------------+-------------+------------+----------------
status      |           5 |    1000000 |           0.00   ← 인덱스 비효율
user_id     |      800000 |    1000000 |          80.00   ← 적당
order_no    |     1000000 |    1000000 |         100.00   ← 인덱스 최적
*/
```

---

## 3. 조회 패턴 — 실제로 어떻게 쓰이는가

인덱스는 **WHERE, JOIN ON, ORDER BY, GROUP BY** 절에서 효과를 발휘합니다.
실제 쿼리 패턴을 먼저 파악하고, 자주 등장하는 컬럼에 인덱스를 겁니다.

### 일상 예시

> 회사 내부 전화번호부를 생각해봅시다.
>
> - 사람들은 주로 **"이름"** 으로 검색 → 이름에 인덱스
> - 가끔 **"부서"** 로 검색 → 부서에 인덱스 고려
> - **"입사일"** 로는 거의 안 검색 → 인덱스 불필요

```mermaid
flowchart LR
    subgraph Queries["자주 실행되는 쿼리"]
        Q1["WHERE user_id = ?"]
        Q2["WHERE status = 'active'\nAND created_at > ?"]
        Q3["ORDER BY created_at DESC"]
    end

    subgraph Indexes["권장 인덱스"]
        I1["INDEX (user_id)"]
        I2["INDEX (status, created_at)"]
        I3["INDEX (created_at)"]
    end

    Q1 --> I1
    Q2 --> I2
    Q3 --> I3
```

### 코드 예시

```sql
-- 1. WHERE 조건에 자주 쓰이는 컬럼
-- 주문 조회: user_id로 자주 필터링
SELECT * FROM orders WHERE user_id = 1001;
CREATE INDEX idx_orders_user_id ON orders(user_id);  -- ✅

-- 2. JOIN ON 에 사용되는 컬럼
-- orders.user_id ↔ users.id 조인
SELECT o.*, u.name
FROM orders o
JOIN users u ON o.user_id = u.id;   -- 양쪽 컬럼에 인덱스가 있어야 효율적
CREATE INDEX idx_orders_user_id ON orders(user_id);  -- ✅

-- 3. ORDER BY + LIMIT (페이지네이션)
-- 최신 주문 목록 조회
SELECT * FROM orders ORDER BY created_at DESC LIMIT 20;
CREATE INDEX idx_orders_created_at ON orders(created_at);  -- ✅

-- 4. GROUP BY
-- 사용자별 주문 수 집계
SELECT user_id, COUNT(*) FROM orders GROUP BY user_id;
CREATE INDEX idx_orders_user_id ON orders(user_id);  -- ✅ (위와 동일 인덱스 재활용)
```

---

## 4. 복합 인덱스 컬럼 순서 — 어떤 컬럼을 앞에 놓을까

### 규칙

1. **등치 조건(`=`)** 컬럼을 앞에
2. **범위 조건(`>`, `<`, `BETWEEN`)** 컬럼을 뒤에
3. 카디널리티가 높은 컬럼을 앞에

### 일상 예시

> 도서관 책 분류를 생각해봅시다.
>
> - **(장르 → 작가명 → 제목)** 순으로 분류하면 장르 안에서 작가명으로 좁히고, 다시 제목으로 좁힐 수 있습니다.
> - 만약 **(제목 → 장르)** 순이면 제목 없이 장르만으로는 찾을 수 없습니다.
>
> 복합 인덱스도 **왼쪽 컬럼부터 순서대로** 사용해야 효과가 있습니다. (**Leftmost Prefix 규칙**)

```mermaid
graph LR
    subgraph Index["복합 인덱스: (status, created_at)"]
        direction LR
        C1["status 기준 정렬<br/>(active / inactive)"]
        C2["같은 status 내에서<br/>created_at 기준 정렬"]
        C1 --> C2
    end

    subgraph UseOK["인덱스 사용 O"]
        U1["WHERE status = 'active'"]
        U2["WHERE status = 'active'\nAND created_at > '2024-01-01'"]
    end

    subgraph UseNG["인덱스 사용 X"]
        U3["WHERE created_at > '2024-01-01'\n(앞 컬럼 status 없음)"]
    end

    Index --> UseOK
    Index -.->|"미적용"| UseNG
```

### 코드 예시

```sql
-- 잘못된 인덱스 순서 (범위 조건이 앞에)
CREATE INDEX idx_bad ON orders(created_at, status);
-- created_at 범위를 먼저 걸면 status 인덱스가 사실상 무력화됨

-- 올바른 인덱스 순서 (등치 조건 앞, 범위 조건 뒤)
CREATE INDEX idx_good ON orders(status, created_at);

-- 이 쿼리에서 idx_good은 효과적으로 동작
SELECT * FROM orders
WHERE status = 'paid'           -- status로 먼저 좁히고
  AND created_at > '2024-01-01' -- 그 안에서 created_at으로 범위 탐색
ORDER BY created_at DESC;

-- EXPLAIN으로 확인
EXPLAIN SELECT * FROM orders
WHERE status = 'paid' AND created_at > '2024-01-01';
-- type: range, key: idx_good  → 인덱스 정상 사용
```

---

## 5. 인덱스를 피해야 하는 컬럼

모든 컬럼에 인덱스를 거는 것은 오히려 **쓰기 성능을 저하**시킵니다.

### 인덱스를 걸지 말아야 할 경우

| 상황 | 이유 | 예시 |
|------|------|------|
| 카디널리티가 매우 낮은 컬럼 | 결과를 거의 좁히지 못함 | `gender`, `is_deleted` |
| 자주 UPDATE되는 컬럼 | 변경마다 인덱스 재정렬 비용 발생 | `last_login_at`, `view_count` |
| 조회에 거의 사용되지 않는 컬럼 | 공간 낭비 + 쓰기 성능 저하 | `memo`, `remark` |
| 데이터가 매우 적은 테이블 | Full Scan이 더 빠름 | 코드 테이블(수십 행) |

### 일상 예시

> 쇼핑몰 상품 테이블에서 `view_count`(조회수) 컬럼에 인덱스를 건다고 가정합니다.
>
> - 상품 조회 페이지를 열 때마다 `view_count`가 1씩 증가합니다.
> - 인덱스가 있으면 **매 조회마다 인덱스도 재정렬** → 인기 상품일수록 더 심각한 쓰기 병목 발생합니다.
> - `view_count`는 인덱스 없이, 필요하면 별도 캐시 서버(Redis)에서 관리하는 것이 낫습니다.

```sql
-- ❌ 잘못된 설계: 자주 변경되는 컬럼에 인덱스
CREATE INDEX idx_view_count ON products(view_count);
-- 상품 조회 시마다 INSERT INTO → 인덱스 재정렬 → 쓰기 병목

-- ✅ 올바른 설계: 조회 필터에 사용되는 컬럼에만 인덱스
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price    ON products(price);
-- view_count는 Redis 등 별도 캐시에서 관리
```

```mermaid
flowchart LR
    subgraph Bad["인덱스 걸면 안 되는 컬럼"]
        B1["view_count<br/>(매 조회마다 UPDATE)"]
        B2["is_deleted<br/>(값: true/false만 존재)"]
        B3["memo<br/>(조회에 거의 안 쓰임)"]
    end

    subgraph Good["인덱스를 걸어야 하는 컬럼"]
        G1["user_id<br/>(JOIN, WHERE 자주 사용)"]
        G2["email<br/>(고유값, 로그인 조회)"]
        G3["created_at<br/>(범위 조회, 정렬)"]
    end

    Bad -->|"쓰기 성능 저하 / 효과 없음"| X["인덱스 X"]
    Good -->|"조회 성능 향상"| Y["인덱스 O"]
```

---

## 정리: 인덱스 컬럼 선택 체크리스트

```mermaid
flowchart TD
    S["컬럼 검토 시작"]

    S --> C1{"WHERE / JOIN / ORDER BY에\n자주 등장하는가?"}
    C1 -->|"No"| R1["❌ 인덱스 불필요"]

    C1 -->|"Yes"| C2{"카디널리티 / 선택도가\n충분한가? (5% 이하)"}
    C2 -->|"No"| C3{"복합 인덱스나\n부분 인덱스로 해결 가능?"}
    C3 -->|"No"| R2["❌ Full Scan이 유리\n인덱스 불필요"]
    C3 -->|"Yes"| R3["⚠️ 복합 인덱스 설계 검토"]

    C2 -->|"Yes"| C4{"자주 UPDATE되는\n컬럼인가?"}
    C4 -->|"Yes"| R4["⚠️ 쓰기 비용 고려 후 결정"]
    C4 -->|"No"| R5["✅ 인덱스 적합!"]
```

| 기준 | 인덱스 적합 | 인덱스 부적합 |
|------|-----------|-------------|
| 카디널리티 | 높음 (이메일, 주문번호) | 낮음 (성별, 상태값) |
| 선택도 | 5% 이하 | 10% 초과 |
| 조회 빈도 | WHERE/JOIN에 자주 등장 | 거의 사용 안 됨 |
| 변경 빈도 | 낮음 (정적 데이터) | 높음 (수시 UPDATE) |
| 데이터 규모 | 대용량 테이블 | 수십~수백 행 소규모 |

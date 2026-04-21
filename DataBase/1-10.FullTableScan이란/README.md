# Full Table Scan이란

Full Table Scan(전체 테이블 스캔)은 인덱스를 사용하지 않고 테이블의 모든 데이터 페이지를 순차적으로 읽어 조건에 맞는 행을 찾는 접근 방식이다.

- 항상 피해야 할 대상은 아니며, 특정 상황에서는 가장 효율적인 접근 경로
- 실행 계획(EXPLAIN)에서 `type: ALL`(MySQL) 또는 `Seq Scan`(PostgreSQL)으로 표시

옵티마이저가 인덱스 탐색보다 전체 스캔이 더 저렴하다고 판단할 때 선택된다.

## Full Table Scan의 동작

테이블의 데이터 페이지를 처음부터 끝까지 순차적으로 읽으며 각 행에 WHERE 조건을 평가한다.

- 데이터 페이지를 메모리로 적재
- 순차 I/O라 단위 비용은 랜덤 I/O보다 낮음

## Full Table Scan이 발생하는 조건

옵티마이저는 비용 기반(Cost-Based)으로 실행 계획을 결정하며, 다음 상황에서 Full Table Scan이 선택된다.

### 1. 인덱스가 없음

조회 컬럼에 인덱스가 없으면 다른 선택지가 없다.

```sql
-- age 컬럼에 인덱스 없음
SELECT *
FROM users
WHERE age = 30;
-- → Full Table Scan 강제
```

### 2. 인덱스가 있어도 더 비쌈

조건에 맞는 행이 전체의 상당 비율이면, 인덱스 탐색 후 실제 행 접근(Random I/O)이 누적되어 Full Scan보다 느려진다.

- 일반적 임계점: 전체 행의 약 20~25% (DBMS와 상황에 따라 다름)
- 랜덤 I/O는 순차 I/O보다 단위 비용이 크기 때문에, 많은 행을 접근할 때 역전

```sql
-- 100만 행 테이블에서 80%가 status = 'ACTIVE'라면
SELECT *
FROM users
WHERE status = 'ACTIVE';
-- 인덱스가 있어도 Full Table Scan이 더 효율적
```

### 3. 테이블이 매우 작음

데이터가 한두 개 페이지에 담기는 작은 테이블은 인덱스 접근 비용이 오히려 크다.

- B-Tree 탐색 비용 vs 페이지 1~2개 순차 읽기
- 작은 테이블은 Full Scan이 기본 선택

### 4. 함수·연산으로 인덱스 무효화

인덱스 컬럼을 가공하면 인덱스를 사용할 수 없게 된다.

```sql
-- 인덱스 무효화 사례
SELECT *
FROM users
WHERE YEAR(created_at) = 2024;
-- created_at에 인덱스가 있어도 함수 적용으로 인덱스 미사용

-- 해결: 범위 조건으로 재작성
SELECT *
FROM users
WHERE created_at >= '2024-01-01'
  AND created_at < '2025-01-01';
```

- LIKE 선두 와일드카드(`'%kim'`)도 동일하게 인덱스 무효화
- 인덱스 컬럼 타입 변환

## 인덱스 스캔과의 비교

Full Table Scan과 Index Scan은 각자 유리한 상황이 다르다.

|    구분    | Full Table Scan |   Index Range Scan   |
|:--------:|:---------------:|:--------------------:|
|  접근 단위   |     페이지 순차      | 인덱스 B-Tree 탐색 + 행 접근 |
|   I/O    |     순차 I/O      |     랜덤 I/O 발생 가능     |
| 조회 비율 유리 |  많은 행 (20% 이상)  |    적은 행 (선별성 높음)     |
|    정렬    |    정렬 보장 없음     |    인덱스 순서대로 반환 가능    |
| 버퍼 풀 점유  |   많은 페이지 캐싱됨    |      소수 페이지만 접근      |

### Covering Index로 Full Scan 방지

자주 조회되는 컬럼을 인덱스에 모두 포함시키면, 인덱스만으로 쿼리를 완결할 수 있어 Full Table Scan을 방지할 수 있다.

```sql
-- 복합 인덱스로 필요한 모든 컬럼 포함
CREATE INDEX idx_users_covering ON users (status, created_at, name);

-- 쿼리가 인덱스만으로 완결
SELECT name
FROM users
WHERE status = 'ACTIVE'
  AND created_at > '2024-01-01';
-- → Covering Index Scan, 테이블 접근 생략
```

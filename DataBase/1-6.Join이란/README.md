# Join은 무엇이며 어떤 종류들이 있을까요

조인(Join)은 두 개 이상의 테이블을 특정 조건으로 연결해 하나의 결과 집합으로 반환하는 관계 연산으로, 정규화로 분리된 테이블을 질의 시점에 다시 합치는 수단이다.

## 조인의 종류

결과 집합에 포함시킬 행의 기준에 따라 구분된다.

|       종류        |                  설명                  |     결과 집합      |
|:---------------:|:------------------------------------:|:--------------:|
|   INNER JOIN    |       양쪽 테이블에서 조인 조건을 만족하는 행만        |      교집합       |
|    LEFT JOIN    | 왼쪽 테이블의 모든 행 + 오른쪽 매칭, 미매칭은 NULL로 채움 |     왼쪽 전체      |
|   RIGHT JOIN    | 오른쪽 테이블의 모든 행 + 왼쪽 매칭, 미매칭은 NULL로 채움 |     오른쪽 전체     |
| FULL OUTER JOIN |      양쪽의 모든 행, 매칭되지 않는 쪽은 NULL로      |      합집합       |
|   CROSS JOIN    |      조인 조건 없이 양쪽의 모든 조합 (카티션 곱)      | M행 × N행 = M×N행 |
|    SELF JOIN    |       동일 테이블을 별칭으로 두 번 참조해 조인        |  동일 테이블 내 관계   |

```sql
-- 예시 테이블
-- employees: (1, Alice, 10), (2, Bob, 20), (3, Carol, NULL)
-- departments: (10, 개발팀), (30, 디자인팀)

-- INNER JOIN: 두 조건 모두 만족하는 Alice만 반환
SELECT e.name, d.name
FROM employees e
         INNER JOIN departments d ON e.dept_id = d.id;
-- 결과: (Alice, 개발팀)

-- LEFT JOIN: 직원 전체 + 부서 매칭, Bob/Carol은 부서 NULL
SELECT e.name, d.name
FROM employees e
         LEFT JOIN departments d ON e.dept_id = d.id;
-- 결과: (Alice, 개발팀), (Bob, NULL), (Carol, NULL)
```

## 조인 순서와 드라이빙 테이블

여러 테이블을 조인할 때 어떤 테이블을 먼저 읽는지에 따라 실행 비용이 크게 달라진다.

- 드라이빙 테이블(Driving Table): 조인의 시작점
- 드리븐 테이블(Driven Table): 드라이빙 테이블의 각 행을 기준으로 탐색되는 테이블

FROM/JOIN에 쓴 순서는 드라이빙 테이블 선택에 영향을 주지 않으며, 옵티마이저가 통계 정보를 바탕으로 비용이 가장 낮은 순서를 결정한다.

- 필터 조건 적용 후 결과 행 수가 가장 적은 쪽이 드라이빙으로 선택됨
- 드리븐 쪽에 조인 키 인덱스가 있으면 탐색 비용이 낮아 드리븐 후보가 됨
- 통계가 부정확하면 잘못된 순서를 선택할 수 있으며, MySQL의 `STRAIGHT_JOIN`, Oracle의 `LEADING` 힌트로 강제 지정 가능

```sql
-- 옵티마이저가 잘못된 순서를 선택한 경우, 힌트로 강제
SELECT STRAIGHT_JOIN *
FROM small_table s
         JOIN large_table l ON s.id = l.s_id;
-- small_table을 먼저 읽도록 강제
```

## ON과 WHERE의 차이

OUTER JOIN에서는 같은 조건을 ON에 두느냐 WHERE에 두느냐에 따라 결과가 달라지는데, 이는 논리적 처리 순서(`FROM` → `ON` → `JOIN` → `WHERE`) 차이에서 나온다.

다음 데이터로 "부서가 없는 직원도 포함하되, 개발팀만 조회하고 싶다"는 요구를 두 가지 방식으로 비교한다.

- employees: (1, Alice, 10), (2, Bob, 20), (3, Carol, NULL)
- departments: (10, 개발팀), (30, 디자인팀)

### 방식 1 - ON에 조건 추가

```sql
SELECT e.name, d.name
FROM employees e
         LEFT JOIN departments d ON e.dept_id = d.id AND d.name = '개발팀';
```

각 직원 행에 대해 `dept_id = id AND name = '개발팀'` 조건으로 매칭한다.

- 매칭 실패해도 LEFT JOIN 의미상 왼쪽 행은 유지되고 오른쪽 컬럼만 NULL로 채워짐

| e.name | d.name |
|:------:|:------:|
| Alice  |  개발팀   |
|  Bob   |  NULL  |
| Carol  |  NULL  |

### 방식 2 - WHERE로 필터

```sql
SELECT e.name, d.name
FROM employees e
         LEFT JOIN departments d ON e.dept_id = d.id
WHERE d.name = '개발팀';
```

LEFT JOIN이 먼저 수행되어 `(Alice, 개발팀), (Bob, NULL), (Carol, NULL)`을 만든 뒤, 그 결과에 `d.name = '개발팀'` 필터를 적용한다.

- NULL 값과의 비교는 UNKNOWN이라 Bob, Carol은 결과에서 제외됨

| e.name | d.name |
|:------:|:------:|
| Alice  |  개발팀   |

즉, WHERE에 드리븐 테이블의 컬럼 조건을 두면 LEFT JOIN으로 확보한 NULL 행이 다시 걸러져 실질적으로 INNER JOIN과 같은 결과가 된다.

- ON: 조인 단계의 매칭 조건이므로 LEFT JOIN의 보존 의미가 유지됨
- WHERE: 조인 이후의 후처리 필터라 NULL 행이 제외됨

### 성능 관점

결과가 같은 쿼리라면 ON과 WHERE의 성능은 거의 차이가 없다.

- INNER JOIN: ON/WHERE가 의미상 동일해 옵티마이저가 Predicate Pushdown으로 같은 실행 계획을 생성
- OUTER JOIN: 결과 자체가 달라지므로 성능이 아닌 의도 기준으로 선택
- 조인 키에 인덱스가 있으면 ON의 등가 조건은 인덱스로 매칭되고, 추가 필터는 ON/WHERE 어디에 두든 비슷하게 처리됨

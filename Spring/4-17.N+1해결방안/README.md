## N+1 해결 방안에 대해 설명해주세요. 

### 1. Fetch Join
JPQL을 사용하여 DB에서 데이터를 가져오게 하는 방법

```java
@Query("select m from Member m join fetch m.team")
List<Member> findAllWithTeam();
```

```java
select m
from Member m
join fetch m.team
```

Member 목록을 조회할 때 각 Member가 속한 Team도 한 번에 조회

-> ```member.getTeam().getName()```을 호출해도 Team 조회 쿼리가 추가로 발생 X

### 2. ```@EntityGraph```
Repository 메서드에 연관관계를 함께 조회하도록 지정하는 방식
```java
@EntityGraph(attributePaths = {"team"})
@Query("select m from Member m")
List<Member> findAllWithTeam();
```
Member를 조회할 때 team도 함께 로딩

fetch join과 비슷하게 동작해서, 이후에 ```member.getTeam().getName();```을 호출해도 Team 조회 쿼리가 추가로 나가지 않음!

### 3. ```@BatchSize```
지연 로딩은 유지하되, Team을 하나씩 조회하지 않고 여러 개를 묶어서 조회하는 방식
```java
@ManyToOne(fetch = FetchType.LAZY)
@BatchSize(size = 100)
private Team team;
```
Member 목록 조회 후 Team에 접근하면

```sql
select *
from team
where id in (?, ?, ?, ...);
```
처럼 Team을 ```IN```절로 한 번에 묶어서 조회
즉,

Member 조회 1번 + Team batch 조회 1번 또는 몇 번

으로 줄어든다!

[Batch size 전역적으로 조절 방법]
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```
default_batch_fetch_size 설정을 통해 프로젝트 전체에 기본 batch fetch 전략을 적용

### 4. DTO로 Projection
엔티티를 조회하지 않고, 필요한 데이터만 직접 조회

```java
select new com.example.MemberTeamDto(
        m.id,
        m.name,
        t.name
        )
from Member m
join m.team t
```

```java
queryFactory
    .select(new QMemberDto(member.id, member.name, team.name))
    .from(member)
    .join(member.team, team)
    .fetch();

```
Member와 Team 엔티티 전체를 로딩하지 않고, 화면에 필요한 값만 한 번에 가져올 수 있음!

그러나
- 엔티티가 아니므로 영속성 컨텍스트 미적용 (변경 감지 X)
- 쿼리 재사용이 어려울 수 있음
- 객체 그래프 탐색이 어려움 -> 엔티티 관계를 따라가며 탐색하는 것이 아니라, 조회 시점에 필요한 데이터를 명시적으로 획득

## Fetch Join 과 Limit 를 같이 사용하면 어떤 문제가 발생하나요?
> Fetch Join과 Limit를 함께 사용하면, 특히 @OneToMany 같은 컬렉션 fetch join의 경우 정상적인 DB 페이징이 어려워질 수 있습니다.
> 
> 이유는 fetch join을 하면 SQL 결과가 부모 엔티티 기준이 아니라 부모-자식 조인 row 기준으로 증가하기 때문입니다. 그래서 DB에서 limit을 적용하면 원하는 엔티티 개수가 아니라 조인 row 기준으로 잘릴 수 있습니다.
> 
> Hibernate는 이런 불완전한 컬렉션 로딩 문제를 방지하기 위해 DB에서 바로 페이징하지 않고, 전체 데이터를 조회한 뒤 메모리에서 페이징합니다.
>
> 문제는 데이터가 많아지면 메모리 사용량 증가와 성능 저하가 발생할 수 있다는 점입니다.

실무에서는 보통 부모 ID만 먼저 페이징 조회한 뒤 해당 ID로 fetch join을 수행하거나, @BatchSize와 default_batch_fetch_size를 사용해 N+1 문제를 완화하는 방식으로 해결합니다.

### Fetch Join과 Limit 문제점
oneToMany 혹은 manyToMany 관계에서 Fetch Join과 Limit을 같이 사용할 경우

> HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory

이런 경고 메시지를 확인할 수 있는데...

이는 컬렉션 fetch join을 한 상태에서 페이징을 걸었기 때문에, DB에서 페이징하지 못하고 Hibernate가 Application Memory에 쿼리 수행 결과를 적재한 후 페이징 하겠다는 의미!

=> **"모든 데이터"**를 전부 가져와 메모리에서 걸러낸다는 것

```java
select t
from Team t
join fetch t.members
```
팀의 멤버를 fetch join을 통해 가져올때 pageable이나 setMaxResults를 쓰면 Hibernate는

> Team 10개를 가져와야 하는데, SQL 결과는 Team-Member 조인 때문에 row가 뻥튀기 된다. DB에서 limit을 10을 걸면 Team 10개가 아니라 조인 row 10개로 잘릴 수 있다.

라고 생각하게 됩니다...! (SQL의 LIMIT은 엔티티 개수가 아니라 **"결과 row 개수"**에 적용되기 때문에)

그래서 안전하게 전체 데이터를 가져온 뒤 메모리에서 페이징!

=> 이때 성능 문제를 유발할 수 있음

Team이 10개만 필요한데 실제로 Team + Member 전체를 다 가져올 수 있음

#### 데이터가 많다면...?
메모리 사용량 증가, 조회 속도 저하, 장애 위험 발생

### 해결 방법
**1. 부모 ID 페이징 조회 후 fetch join**
앞서 든 팀과 멤버의 관계가 존재한다고 했을때

```java
select t.id
from Team t
order by t.id
```

이렇게 먼저 부모 ID가 되는 Team을 기준으로 페이징 해주고

```java
select distinct t
from Team t
join fetch t.members
where t.id in :teamIds
```

방금 가져온 목록으로 다시 팀원을 조회하는 방법으로

단점은 쿼리가 2번 나간다는 것... 그러나 전체 데이터를 메모리에 올리는 것보다 훨씬 안전

**2. @BatchSize**
이 방식은 fetch join 대신 지연 로딩을 유지하고, 컬렉션을 조회할 때 여러 개를 묶어서 가져오는 방식
```java
@OneToMany(mappedBy = "team")
@BatchSize(size = 100)
private List<Member> members = new ArrayList<>();
```
이 상태에서 team을 조회하면 Team마다 쿼리가 하나씩 나가는 것이 아니라, 여러 Team의 members를 한번에 조회

예를 들어 Team을 10개를 조회했고 각 Team의 members에 접근함녀
```sql
select *
from member
where team_id in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

이렇게 쿼리가 나가게 됩니당..!

이 방식의 단점 역시 쿼리가 1번으로 끝나지 않고 연관관계에 접근해야 쿼리가 나가게 됩니다.

**3. ```hibernate.default_batch_fetch_size```**
이는 ```@BatchSize```를 엔티티마다 붙이지 않고 전역으로 설정하는 방법입니다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```
지연 로딩되는 연관관계를 Hibernate가 적절히 묶어서 조회


**4. DTO Projection**
필요한 데이터만 직접 조회하는 방식

```java
select new com.example.TeamDto(
    t.id,
    t.name,
    count(m)
)
from Team t
left join t.members m
group by t.id, t.name
order by t.id
```

Team 기준으로 group by를 해서 결과가 Team당 1 row로 나오기 때문에 limit 10을 걸면 Team 10개가 정상적으로 조회


| 방법                                       | 언제 쓰면 좋은가                             |
| ---------------------------------------- | ------------------------------------- |
| **Fetch Join**                           | 연관 데이터가 많지 않고, 페이징이 없을 때              |
| **ID 먼저 페이징 후 Fetch Join**               | 부모 엔티티 기준 페이징이 필요하고, 컬렉션도 함께 보여줘야 할 때 |
| **`@BatchSize`**                         | 특정 연관관계의 N+1을 줄이고 싶을 때                |
| **`hibernate.default_batch_fetch_size`** | 프로젝트 전반에서 지연 로딩 N+1을 기본적으로 완화하고 싶을 때  |
| **DTO Projection**            | 조회 전용 API나 목록 화면에서 필요한 필드만 가져오면 될 때            |

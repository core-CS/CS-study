## N+1 발생 원인에 대해 설명해 주세요.
>N+1 문제는 최초 조회 쿼리 1번 이후, 조회된 엔티티의 개수만큼 연관 엔티티 조회 쿼리가 추가로 발생하는 문제입니다.
>
>예를 들어 Member가 Team을 지연 로딩으로 참조할 때, Member 목록을 조회하면 먼저 member 조회 쿼리 1번이 나갑니다. 이후 반복문에서 member.getTeam().getName()처럼 Team에 접근하면, 각 Member마다 Team을 조회하는 쿼리가 추가로 발생합니다.
>
>즉, Member가 N명이라면 Member 조회 1번, Team 조회 N번으로 총 N+1번의 쿼리가 발생합니다.
>
>근본적인 원인은 객체는 연관 객체를 참조로 바로 접근할 수 있지만, RDB는 연관 데이터를 조회하려면 별도의 SQL이 필요하기 때문입니다. 이 패러다임 차이를 ORM이 처리하는 과정에서 N+1 문제가 발생합니다.

### N+1 문제란?
N+1 문제는 ORM 기술에서 특정 객체를 대상으로 수행한 쿼리가 해당 객체가 가지고 있는 연관관계 또한 조회하게 되면서 N번의 추가적인 쿼리가 발생하는 문제

#### 예시
Team과 Member가 있고, Member가 Team을 지연 로딩으로 참조 

```java
@ManyToOne(fetch = FetchType.LAZY)
private Team team;
```

이 상태에서 Member 목록을 조회하면
```sql
select * from member;
```
해당 쿼리가 1번 나가게 됨

```java
for (Member member : members) {
    member.getTeam().getName();
}
```
이후 코드에서 Member의 Team에 접근하게 되면

```sql
select * from team where id = 1;
select * from team where id = 2;
select * from team where id = 3;
...
```

Member마다 Team을 조회하는 쿼리가 추가로 발생

이때 Member 조회가 1번 + Team 조회가 N번이라

N+1 번의 쿼리가 발생하게 됨!

### 원인
N+1문제가 발생하는 근본적인 원인은 관계형 데이터베이스와 객체지향 언어간의 패러다임 차이로 발생

객체는 연관관계를 통해 레퍼런스를 가지고 있으면 언제든 메모리 내에서 Random Access를 통해 연관 객체에 접근할 수 있지만 RDB의 경우 Select 쿼리를 통해서만 조회할 수 있기 때문
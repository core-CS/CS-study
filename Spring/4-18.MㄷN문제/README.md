## M:N 의 문제점은 무엇인가요?
>M:N의 가장 큰 문제는 관계 자체가 하나의 중요한 도메인인데, 이를 명시적으로 표현하지 못해 데이터 정합성, 확장성, 성능 문제가 생긴다는 점입니다.
> 
> DB 관점에서는 다대다 관계를 그대로 두면 데이터 중복이 발생하기 쉽고, 삽입·삭제·갱신 이상으로 인해 정합성을 유지하기 어려워집니다.
> 
> JPA 관점에서는 @ManyToMany를 사용하면 내부적으로 조인 테이블이 만들어지지만, 이 조인 테이블을 엔티티로 직접 관리하기 어렵습니다. 그래서 수강 등록일, 점수, 상태 같은 추가 정보를 저장하기 어렵고, 관계의 의미도 코드에 명확히 드러나지 않습니다.
>
> 또한 연관관계를 조회할 때 숨겨진 조인 테이블을 거치며 조인이 연쇄적으로 발생할 수 있어 예상보다 복잡한 쿼리와 성능 저하가 생길 수 있습니다.
>
> 그래서 저는 @ManyToMany를 직접 쓰기보다 중간 엔티티를 명시적으로 만들어 1:N, N:1 관계로 풀어내는 방식을 선호합니다.

### M:N
데이터베이스나 시스템 설계에서 한 항목이 여러 항목과 연결될 수 있고, 반대로 그 여러 항목들도 하나의 항목과 연결될 수 있는 구조
![다대다](https://blog.kakaocdn.net/dna/xAq93/btsFqM1EsNZ/AAAAAAAAAAAAAAAAAAAAAL2a6YkLphhLirgRSisVKxJ64O80rxbwspHwcoP_dl9h/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=e9NgoAqgPYB6i72BfpYBMkEjLfs%3D)

### M:N의 문제
 ![다대다 테이블](https://blog.kakaocdn.net/dna/bcWmWY/btrwgEFDUSG/AAAAAAAAAAAAAAAAAAAAAHJDB0dzSavnAcvxGQFyTitdcOCHzpoQTG7krpVqa4tA/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=qVhTKoRY9NExBwrlAcqeJGmrQRc%3D)

#### [DB 관점에서]
1. 데이터 중복: 동일한 데이터가 여러 곳에 반복 저장
2. 삽입 이상: 특정 데이터를 추가하기 위해 불필요한 데이터도 함께 추가 필요
3. 삭제 이상: 특정 데이터 삭제 시 필요한 다른 데이터까지 함께 삭제
4. 갱신 이상: 데이터 변경 시 일관성 유지 어려움

#### [JPA 관점에서]

스프링에서 다대다 관계를 표현하기 위해 `@ManyToMany`를 사용할 수 있는데

여기서 문제 발생

**_1. 조인 테이블 활용 불가_**

JPA는 내부적으로 조인테이블 생성 -> **자바 코드 상 직접 관리 X**

개발자들은 조인 테이블에 추가적인 정보를 저장하거나, 이를 직접 조회하는 상황 발생!

ex) 학생의 수업 등록일, 점수 등등

**_2. 성능 저하_**

다대다 관계에서 조인이 연쇄적으로 발생

숨겨진 조인 테이블 때문에 예상치 못한 쿼리가 나가기도 함

만약, `@ManyToMany`를 썼다면...

```java
@Query("""
    select distinct s
    from Student s
    join fetch s.courses c
    join fetch c.tags t
""")
List<Student> findStudentsWithCoursesAndTags();
```

학생 목록을 조회하면서 강의와 태그까지 한 번에 가져오는 기능을 만들었다고 칩시다

이때 발생하는 쿼리는

```sql
select distinct
    s.*,
    c.*,
    t.*
from student s
join student_course sc
    on s.id = sc.student_id
join course c
    on sc.course_id = c.id
join course_tag ct
    on c.id = ct.course_id
join tag t
    on ct.tag_id = t.id;
```
연쇄적 조인 발생...

만일 중간 테이블이 있었다면...?

```sql
select
    sc.*,
    s.*,
    c.*
from student_course sc
join student s
    on sc.student_id = s.id
join course c
    on sc.course_id = c.id
where s.id in (...);
```

**_3. 객체 지향적이지 않음_**

다대다 관계는 RDBMS의 개념!

객체 지향 설계에서 엔티티는 책임과 역할이 명확해야함

그러나, 다대다 관계는 중간 테이블(조인 테이블)을 코드 상에서 명시하지 않으므로, 객체 간의 관계를 명확히 표현할 수 없음!
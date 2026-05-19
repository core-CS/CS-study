## 그렇다면 다대다일 경우, 해결 방법은 무엇인가요?

> 다대다 관계는 가능하면 @ManyToMany를 직접 사용하지 않고, 중간 테이블을 엔티티로 승격해서 일대다, 다대일 관계로 풀어내는 방식으로 해결합니다.
> 이렇게 하면 단순 연결 정보뿐 아니라 참여일, 역할 같은 추가 컬럼을 둘 수 있고, 연관관계 자체를 조회하거나 수정, 삭제하기도 훨씬 명확해집니다.
> 
> 다만 정말 단순한 연결이고 추가 컬럼이나 복잡한 조회·삭제 로직이 없다면 @ManyToMany를 사용할 수도 있습니다. 
> 이 경우에는 List보다는 Set을 사용하고, @JoinTable로 조인 테이블명과 컬럼명을 명시하며, 연관관계 편의 메서드로 양쪽 객체 상태를 동기화하는 방식으로 관리하겠습니다.

### 해결방법
1. 중간 테이블
```java
@Entity
public class MemberStudy {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;

    private LocalDateTime joinedAt;
    private String role;
}
```

이렇게 중간테이블을 만들어

```
Member 1 : N MemberStudy N : 1 Study
```
이런식으로 관계를 변경해줌

### 만약 `@ManyToMany`가 필요하다면...?

```
1. 중간 테이블에 추가 컬럼이 절대 필요 없을 것
2. 관계 자체를 엔티티로 다룰 필요가 없을 것
3. 조회/삭제/수정 로직이 단순할 것
4. 성능 튜닝 요구가 크지 않을 것
5. 데이터 규모가 작거나 관리성보다 구현 속도가 중요한 경우
```

1. `List`보다 `Set` 사용하기

`@ManyToMany`에서는 대부분의 경우 `List`보다 `Set`을 사용하는 것이 좋음!

특히 연관관계 데이터를 삭제할 때 `List`를 사용하면 Hibernate가 조인 테이블의 데이터를 비효율적으로 처리 가능

반면 `Set`은 중복을 허용하지 않고, 특정 요소의 추가/삭제를 더 명확하게 처리 가능

```java
@ManyToMany
private Set<Book> books = new HashSet<>();
```

2. 연관관계 양측 동기화 유지하기

한쪽 컬렉션만 수정하면 객체 상태가 서로 맞지 않을 수 있어서..!

addBook(), removeBook() 같은 연관관계 편의 메서드를 만들어 양쪽 객체의 상태를 함께 변경하는 것이 좋음!

```java
public void addBook(Book book) {
    this.books.add(book);
    book.getAuthors().add(this);
}

public void removeBook(Book book) {
    this.books.remove(book);
    book.getAuthors().remove(this);
}
```

3. 조인 테이블 명시적으로 설정하기

@JoinTable을 사용해 테이블명과 컬럼명을 명시적으로 설정
```java
@ManyToMany
@JoinTable(
    name = "author_book",
    joinColumns = @JoinColumn(name = "author_id"),
    inverseJoinColumns = @JoinColumn(name = "book_id")
)
private Set<Book> books = new HashSet<>();
```


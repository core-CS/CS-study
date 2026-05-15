## Dirty Checking 에 대해 설명해주세요.

***

### Dirty Checking

---

JDBC 를 직접 쓸 때는 개발자가 무엇이 변경되었는지 **직접 추적**을 해서 `UPDATE` 쿼리를 작성해야 했다.

Dirty Checking 은 이 부분을 **JPA 가 전담하는 역할**이다.

즉, **엔티티를 수정**하면 알아서 `UPDATE` 가 된다.

### 동작 방식

---

### 1️⃣ 스냅샷 저장

`em.find()` 또는 JPQL 조회 시 Hibernate 가 엔티티를 1차 캐시에 넣으면서 동시에 그 시점의 필드값 배열 복사본을 보관한다.

### 2️⃣ Flush 시점을 비교한다.

`flush()`  가 수행되면 지금 관리하고 있는 엔티티에 대해서 현재 상태와 `Object.equals()` 를 통해서 변경점을 확인한다.

### 3️⃣ UPDATE SQL 을 만든다.

변경된 필드가 있으면 쿼리문을 만들고 Flush 실행 시 해당 필드만 포함된 UPDATE SQL 이 생성된다.

### Dirty Checking 이 동작하지 않는 상황

---

1️⃣ `@Transactional` 없이 변경

2️⃣ 준영속 상태에서 변경

```java
@Transactional
public void detached(Long id) {
	User user = userRepository.findById(id).get();
	em.detach(user); // 영속성 컨텍스트에서 분리되어
	user.setName("change");  // 스냅샷 추적 대상에서 제외된다.
}
```

- 위의 상황에서 flush 를 수행해도 이 변경이 DB 에 안간다.

3️⃣ 새 트랜잭션에서 준영속 상태의 엔티티를 건드리기

4️⃣ 컨렉션을 바꾼다.

```java
@Transactional
public void replaceOrders(Long userId, List<Order> newOrders) {
	User user = em.find(User.class, userId);
	user.setOrders(newOrders); // 컬렉션을 아예 새걸로 교체한다.
	
}
```

- Hibernate 는 원래 PersistentBag 의 변경을 추적하는데 새 List 로 교체하면 추적이 끊긴다.

### Dirty Checking 최적화?

---

1️⃣ N+1 을 일으킬 수 있는 Dirty Checking 이라면 메모리가 넘치기 때문에

- 대신 대량 조회 + 단건 수정은 JPQL 벌크로 업데이트하기

2️⃣ 읽기에서 UPDATE 쿼리가 발생한다면

- readOnly = true 로 설정하기
- 그리고 DTO 에서 수정하고 Entity 자체를 건드리지 말 것

그 외에도 알면 좋을 것이 `@DynamicUpdate` , `JPQL Bulk insert` 등이 최적화에 도움이 될 것 같습니다.
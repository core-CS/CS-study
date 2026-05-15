## 영속성 컨텍스트의 내용을 데이터베이스에 반영하려면 어떻게 해야하나요?

***


**[이전 질문과 이어집니다.]**

영속성 컨텍스트는 엔티티를 **영구 저장**하는 환경으로 **애플리케이션과 DB 사이의 객체를 보관하는 가상의 데이터베이스** 같은 역할을 합니다.

### `Flush`

---

일반적으로 **트랜잭션을 Commit 하는 순간** 영속성 컨텍스트에 새로 저장된 엔티티를 DB에 반영할 때 이 명령어가 쓰인다.

- 즉, 이를 통해서 **메모리에 있는 엔티티**의 상태를 DB 와 맞추는 과정이 진행되며,
- 영속성 컨텍스트를 비우는 과정은 아니다.
- Flush ≠ Commit. Flush 는 SQL → DB, Commit 은 트랜잭션을 확정하는 행위이기에 Flush 이후 rollback 하면 DB 에 아무것도 남지 않습니다.

### 발생하는 타이밍

---

### 1. `em.flush()`

당연하게도 직접 호출 시 영속성 컨텍스트의 변경된 엔티티들이 DB 에 들어간다.

그러나 직접 호출하는 일은 없을 것이다. (스프링이 직접 관리해주기 때문)

### 2. 트랜잭션 Commit 시

위에서 말했듯 가장 일반적인 케이스로 우리가 트랜잭션이 커밋되면 이가 자동으로 호출된다.

### 3. JPQL 쿼리 실행 시

우리가 쿼리를 날리기 전에 이전 변경 사항이 반영 되어야 정확한 조회가 가능하기에 이 전에 `flush` 를 해주어야 한다.

- **왜 쿼리가 실행되면 자동으로 호출될까?**
    
    ```java
    em.persist(memberA);
    em.persist(memberB);
    em.persist(memberC);
    
    // 영속화를 시키고 중간에 JPQL 쿼리를 수행한다.
    // 아래 쿼리에서 flush 가 발생합니다.
    query = em.createQuery("select m from Member m", Member.class);
    List<Member> members = query.getResult();
    ```
    

- 그러면 성능적으로 부하가 있나?
- 그리고 트랜잭션을 계속 안하고 데이터를 넣는다면?

### 영속성 컨텍스트 → DB 반영 과정

---

**1️⃣ Dirty Checking**

- `flush` 가 호출되면, JPA가 엔티티의 스냅샷과 현재 상태를 비교한다.
- 만약 변경점이 있다면 수정 쿼리를 생성해 `쓰기 지연 SQL 저장소` 에 쌓는다.

**2️⃣ 쓰기 지연 SQL 저장소로 보낸다.**

- 저장소에 쌓여 있는 `INSERT`, `UPDATE`, `DELETE` 쿼리를 모아서 DB 로 보낼 준비를 한다. (위 순서로 쿼리문이 진행되며 이유는 외래키 제약 충돌 방지)

**3️⃣ DB 를 동기화한다.**

- 실제 JDBC 쿼리를 수행한다.
- 하지만 DB 에 쿼리가 전송되었다고 해서 최종적으로 저장된 상태는 아니며, **트랜잭션이 최종적으로 commit 되어야** 동기화가 된 상태인 것이다.

### 영속성 컨텍스트의 관점에서 적용하면..

---

영속성 컨텍스트의 “쓰기 지연” 덕분에 매번 쿼리를 날리지 않고 한 번에 모아서 보내는 `Batch Insert/Update` 가 가능하다.

그래서 `batch_size` 를 조정해 한 번에 보낼 쿼리 수를 조절할 수 있다.

또한 동일 트랜잭션 내에서 DB 가 반영되기 전이라도 1차 캐시를 통해서 수정된 데이터를 즉시 조회할 수 있다.

조회만 하는 서비스에서는 `@Transactional(readOnly = true)` 를 통해 플러시를 생략해 성능 최적화를 꿰할 수 있다.

또한 너무 많은 엔티티를 한 번에 수정하면 DB Lock 이 길어질 수 있기에 변경 범위를 최소한으로 가져가는 것이 좋다.

### 안티 패턴

---

1️⃣ 트랜잭션 없이 Persist → flush 시에는 예외나 무시가 발생합니다.

```java
// 트랜잭션 없이 persist -> flush 시 예외나 무시
public void saveWOTransaction(User user) {
	entityManager.persist(user);
}
```

- 1차 캐시에는 들어가겠지만, 트랜잭션이 없어 DB 에 반영이 안됩니다.

2️⃣ FlushModeType.COMMIT 으로 설정함.

```java
@Transactional
public void antiPattern2() {
	User user = em.find(User.class, 1L);
	user.setName("change");
	
	
	List<User> result = em.createQuery("SELECT u FROM User u WHERE u.name = "change")
					.getResultList();
}
```

- 위 모드로 실행 시 JPQL 실행 전에 Flush 가 안되기 때문에 빈 리스트가 반환됩니다.
- 그래서 [FlushMode.AUTO](http://FlushMode.AUTO) (기본값)으로 변경하지 말고 수행하는 것을 추천

3️⃣ 대량의 INSERT 문에서 flush/clear 없이 수행

```java
@Transactional
public void bulkInsert(List<UserDto> dto) {
	for (UserDto d : dto) {
		em.persist(new User(d));
	}
}
```

- 1차 캐시의 공간이 점차 부족해져 OOM 이 나타날 수 있습니다.
- 그래서 이 때는 Batch + flush 조합으로 사용하는 것을 권장합니다.
    - 주기적으로 1차 캐시를 비워주기
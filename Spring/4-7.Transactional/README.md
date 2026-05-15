## 스프링의 @Transactional 에 대해서 알려주세요.

***

### Transaction이란?

---

데이터 수정 중 예외가 발생한다면?

우리는 당연히 수정 전으로 돌아가야 한다. (Rollback)

이 때 사용되는 것이 `Transaction` 이다.

### JDBC 트랜잭션 과정

---

```java
Connection con = dataSource.getConnection();

try{
	con.setAutoCommit(false);
	// business logic
	con.commit();
} catch (Exception e) {
	con.rollback();
} finally {
	con.close();
}
```

- 위를 보면 우리는 비즈니스 로직을 수행하기 위해서 인프라를 위한 행동이 붙어야 했습니다.
- `@Transactional` 을 통해서 DB 에 연결하고 닫기까지의 과정을 AOP 프록시로 분리해 우리가 **트랜잭션을 몰라도 되게** 해주었습니다.

### Spring 에서 제공하는 Transaction

---

1️⃣ 개발식 트랜잭션

- 트랜잭션 매니저에게 가져오는 방법
- 그러나 휴먼에러가 발생할 수 있기에 사용하지 않습니다.

**2️⃣ 선언적 트랜잭션**

- 바로 어노테이션을 사용하는 방법으로
- 스프링의 AOP 가 무엇인지 알 수 있는 어노테이션입니다.
    - **왜?**
        
        비즈니스 로직에 위처럼 `commit` , `rollback` 같은 트랜잭션 제어 코드가 섞이면 코드를 알아보기 어려워진다.
        
        그렇기에 Spring에서 AOP 를 도입해 트랜잭션이라는 횡단 관심사를 비즈니스 로직에서 완전히 분리했다.
        

### `@Transactional` 이란?

---

트랜잭션은 개별 메서드나 클래스에 붙을 수 있다.

따라서 Class 레벨의 어노테이션은 선언된 클래스 + 서브 클래스 내부의 모든 메서드에 적용이 된다. (단, Lookup 은 불가능하다.)

### 동작 과정

---

> 복잡해서 조금 간략히 설명하겠습니다.
> 

1️⃣ 프록시 객체를 만들고 DI 를 한다.

`@Transactional` 이 붙은 클래스를 감지해서 이를 실제 객체 대신에 이를 감싼 프록시 객체를 생성해서 빈으로 등록한다.

- Client 가 DI 받는 객체는 실제가 아닌 프록시이다 !

**2️⃣ Interceptor**

메서드가 호출되면 프록시의 메서드가 먼저 호출된다.

내부의 인터셉터가 이 동작을 가로챈다.

**3️⃣ 트랜잭션 시작 및 스레드 바인딩**

인터셉터가 트랜잭션을 시작한다.

DB Connection 에서 보관 장소에 보관을 하고, 내부적으로는 로컬쓰레드를  사용한다. = 특정 스레드에 종속됨

**3️⃣ 비즈니스 로직 수행**

프록시가 실제 타겟 객체의 비즈니스 로직을 호출한다.

로직 내부의 모든 쿼리는 방금 전의 로컬쓰레드에 묶여있던 동일 커넥션을 가져와 사용한다. ⇒ 하나의 트랜잭션으로 묶이게 된다.

**4️⃣ 트랜잭션 종료 (Commit or Rollback)**

비즈니스 로직이 예외가 없다면 커밋을 한다.

- 만약 커스텀 룰이 없다면 기본적으로 RuntimeException 과 Error 상황에서는 롤백되기에 Check Exception 에서는 그냥 커밋되므로 주의하자 !

![image.png](attachment:dbfb4630-f096-45ad-96b3-93e007ef1c78:image.png)

### 안티 패턴

---

```java
// 같은 클래스 내부 메서드 호출 (Self-Invocation)
@Service
public class OrderService {

    public void processOrder(Order order) {
        saveOrder(order);         // 프록시를 거치지 않고 직접 호출!
        // @Transactional 완전 무시됨
    }

    @Transactional
    public void saveOrder(Order order) {
        orderRepository.save(order);
        // 이 트랜잭션은 절대 시작되지 않음
        // 이유: processOrder는 프록시로 호출되지만
        //        내부에서 this.saveOrder()는 실제 객체의 메서드 직접 호출
    }
}
```

해결방법 - 클래스 분리

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderPersistenceService orderPersistenceService;

    public void processOrder(Order order) {
        orderPersistenceService.saveOrder(order); // 외부 Bean 호출 → 프록시 경유
    }
}

@Service
public class OrderPersistenceService {
    @Transactional
    public void saveOrder(Order order) {
        orderRepository.save(order); // 정상 동작
    }
}
```

### 참고 자료

---

우아한 형제들 2023

- 대규모 트랜잭션을 처리하는 배민 주문 시스템 규모에 따른 진화

https://www.youtube.com/watch?v=704qQs6KoUk

### 꼬리 질문

- 같은 트랜잭션 내에서 내부 메서드가 `RuntimeException`을 던지고 외부에서 `catch`로 잡았는데 `UnexpectedRollbackException`이 발생했습니다. 왜 그런가요?
    
    `REQUIRED` 전파에서 내부 메서드가 `RuntimeException`을 던지면 `TransactionInterceptor`가 해당 트랜잭션에 `rollback-only` 플래그를 마킹합니다. 외부에서 예외를 catch해도 이 마킹은 지워지지 않습니다. 이후 외부 메서드가 정상 종료되어 `commit`을 시도하면, `AbstractPlatformTransactionManager`가 `rollback-only` 플래그를 확인하고 `commit` 대신 `rollback` 후 `UnexpectedRollbackException`을 던집니다. 해결책은 내부 메서드를 `REQUIRES_NEW`로 분리해서 독립된 트랜잭션으로 만들거나, 예외를 삼키지 않고 명시적으로 처리하는 것입니다.
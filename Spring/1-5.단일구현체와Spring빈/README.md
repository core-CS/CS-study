# 구체 클래스가 하나임에도 Spring Bean을 사용하는 이유

> - 빈은 다형성과 무관한 혜택이 더 많음
> - 싱글톤 생명주기, AOP 프록시, 의존성 그래프 일관성, 테스트·환경별 교체, 외부 구성 주입

## 빈 등록의 장점

|     이유      |                       핵심 효과                        |
|:-----------:|:--------------------------------------------------:|
| 싱글톤 생명주기 관리 |                인스턴스 초기화·종료 콜백 자동 처리                |
| AOP 프록시 적용  | `@Transactional`·`@Async`·`@Cacheable` 등 선언적 부가 기능 |
| 의존성 그래프 일관성 |             다른 빈을 받으려면 자신도 빈이어야 자연스러움              |
| 테스트·환경별 교체  |            Mock 주입, `@Profile`로 환경별 분기             |
| 외부 구성 자동 주입 |  `@Value`, `@ConfigurationProperties`로 설정값 자동 반영   |

### 1. 객체 생명주기 관리

Spring 빈의 기본 스코프는 싱글톤이므로, 빈으로 등록하면 컨테이너가 인스턴스 수를 자동으로 제어해준다.

```java

@Service
class OrderService { ...
}

// 컨테이너 내에서 단 하나의 인스턴스만 존재
@Autowired
OrderService a;
@Autowired
OrderService b;
// a == b → true
```

- 직접 `new`로 만들면 호출 시마다 인스턴스가 생기거나, 어딘가에 직접 캐싱 로직을 둬야 함
- 컨테이너에 맡기면 인스턴스 수 제어, 초기화/종료 콜백(`@PostConstruct`/`@PreDestroy`)이 자동 처리

### 2. AOP 프록시 적용

`@Transactional`, `@Async`, `@Cacheable`, `@Retryable` 같은 어노테이션은 모두 빈에 적용된 프록시를 통해 동작한다.

```java

@Service
class OrderService {

    @Transactional
    void place(Order o) { ...}
}
```

- `new OrderService()`로 직접 만든 인스턴스에서는 `@Transactional`이 동작하지 않음
- 빈으로 등록되어야 컨테이너가 프록시를 씌우고, 메서드 호출을 가로채 트랜잭션 경계 생성

### 3. 의존성 그래프의 일관성

대상 클래스가 다른 빈을 사용한다면, 그 자신도 빈으로 등록하여 의존성 그래프에 포함시켜 일관된 방식으로 관리 할 수 있다.

```java

@Service
class OrderService {

    OrderService(PaymentClient client, OrderRepository repo) { ...}
}
```

- `PaymentClient`, `OrderRepository`가 빈이므로 `OrderService`를 빈으로 두지 않으면 호출자가 매번 두 의존성을 직접 받아 넘겨야 함
- 모든 협력 컴포넌트가 빈으로 등록되어, 컨테이너가 의존성 그래프를 한 번에 조립

### 4. 테스트와 환경별 교체의 여지

테스트에서 Mock 구현체를 주입하거나, 운영과 테스트 환경에서 다른 구현체를 주입하는 등, 교체 가능성이 있다면 빈으로 등록해두는 것이 좋다.

- 단위 테스트에서 Mock으로 교체
- 통합 테스트에서 Fake/Stub으로 교체 (예: 결제 게이트웨이를 인메모리 가짜 구현으로)
- 운영/스테이징 환경별로 다른 구현체 (`@Profile`)

```java

@Service
@Profile("prod")
class RealPaymentClient implements PaymentClient { ...
}

@Service
@Profile("test")
class FakePaymentClient implements PaymentClient { ...
}
```

### 5. 외부 구성의 자동 주입

빈은 `@ConfigurationProperties`, `Environment`, `@Value` 등을 통해 외부 설정을 자동으로 주입받는다.

```java

@Service
class OrderService {

    private final int maxRetry;

    OrderService(@Value("${order.max-retry:3}") int maxRetry) {
        this.maxRetry = maxRetry;
    }
}
```

- `new`로 만들면 설정값을 어딘가에서 직접 읽어와야 하고, 환경별 분리도 직접 관리해야 함
- 빈으로 등록하면 환경 변수·yml·시스템 프로퍼티·암호화된 설정 등이 자동 반영

## 빈으로 등록하지 않는 경우

모든 클래스를 빈으로 만들 필요는 없으며, 보통 다음과 같은 객체는 보통 빈으로 두지 않는다.

|         성격          |         예시         |           이유            |
|:-------------------:|:------------------:|:-----------------------:|
| 값 객체 (Value Object) | `Money`, `Address` | 생성 시 값이 결정되고 매번 새로 만들어짐 |
|         엔티티         | JPA `@Entity` 클래스  |   영속성 컨텍스트가 생명주기를 관리    |
|     DTO / 요청 객체     |   `OrderRequest`   |  요청·응답마다 새로 생성, 상태 없음   |
|        정적 유틸        |   `StringUtils`    | 상태도 의존성도 없음, 정적 메서드로 충분 |

빈은 공유되며 협력하는 객체에 적합한 단위이고, 데이터 자체인 객체는 빈으로 두지 않는다.

### 정적 유틸의 빈 등록

정적 헬퍼는 보통 빈으로 둘 이유가 없는데, 상태도 없고 의존성도 없는 함수 모음이기 때문이다.

```java
class TimeUtil {

    static String today() {
        return LocalDate.now().toString();
    }
}
```

하지만 유틸에서 외부 의존성을 갖게 되는 경우, 빈으로 등록해 DI를 활용하는 것이 좋다.

```java

@Component
class TimeProvider {

    private final Clock clock;

    TimeProvider(Clock clock) {
        this.clock = clock;
    }

    String today() {
        return LocalDate.now(clock).toString();
    }
}
```

- 운영 환경: `Clock.systemDefaultZone()` 빈을 등록해 실제 시간을 그대로 사용
- 테스트: `Clock.fixed(...)`를 주입해 시간을 고정시켜 검증 가능

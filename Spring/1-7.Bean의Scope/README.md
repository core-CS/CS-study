# Bean의 Scope

> - Scope: 같은 BeanDefinition을 두고 컨테이너가 인스턴스를 언제 새로 만들고 누구와 공유할지 결정하는 기준
> - 기본은 singleton, 그 외 prototype·request·session·application·websocket
> - 실무에서는 사실상 singleton + (가끔) 웹 스코프 정도, prototype은 거의 안 씀
> - 면접 빈출: 싱글톤에 짧은 스코프(prototype/request)를 주입하면 인스턴스가 박제되는 함정 → 스코프 프록시 또는 `ObjectProvider`로 해결

## 스코프 종류

|     스코프     |          생존 범위           | 동시 공유 |
|:-----------:|:------------------------:|:-----:|
|  singleton  |    컨테이너 기동 ~ 종료 (기본값)    |   O   |
|  prototype  |   조회 시점 생성, 클라이언트가 관리    |   X   |
|   request   |      HTTP 요청 하나의 수명      |   X   |
|   session   |        HTTP 세션 수명        |   X   |
| application | ServletContext 수명 (앱 단일) |   O   |
|  websocket  |     WebSocket 세션 수명      |   X   |

웹 스코프 4종은 `spring-web`이 클래스패스에 있어야 동작.

## 실무에서 언제 쓰이나

|     스코프     |                    실무 사용 빈도                     |
|:-----------:|:-----------------------------------------------:|
|  singleton  |                   사실상 거의 모든 빈                   |
|  prototype  |           거의 안 씀 — `new`로 충분한 경우 대부분            |
|   request   | `HttpServletRequest` 등 주입 시 내부적으로 동작, 직접 정의는 드뭄 |
|   session   |              거의 안 씀 (DB·Redis로 대체)              |
| application |                     거의 안 씀                      |
|  websocket  |                 WebSocket 사용 시                  |

- 로깅·인증 컨텍스트는 request scope가 아니라 `ThreadLocal`이 표준
    - `MDC`, `RequestContextHolder`, `SecurityContextHolder` 모두 ThreadLocal 기반으로 사용
- 라이브러리에서 활발히 쓰이는 건 보통 커스텀 스코프
    - Spring Batch의 `@StepScope`·`@JobScope`(job parameter 늦은 바인딩), Spring Cloud의 `@RefreshScope`(설정 핫리로드)

## singleton의 무상태(stateless) 설계

```java

@Service
class StatefulService {

    private int total;  // 위험: 모든 호출이 공유

    void add(int v) {
        total += v;
    }
}
```

- 모든 스레드가 한 인스턴스를 공유 → 변경 가능한 필드는 즉시 동시성 문제
- 상태는 메서드 지역 변수·파라미터·`ThreadLocal`에 두거나, 필드라면 `final` + 불변 또는 자체 동시성 보장(`AtomicLong`, `ConcurrentHashMap` 등)

## prototype의 주의점

- 컨테이너가 생성·DI·초기화까지만 책임지고 이후엔 참조를 놓음
- 결과적으로 `@PreDestroy` 등 소멸 콜백이 호출되지 않음 → 자원 해제는 클라이언트가 직접 책임져야 함

## 스코프 미스매치

긴 스코프(singleton) 빈에 짧은 스코프(prototype·request) 빈을 그대로 주입하면, 짧은 스코프의 의도가 무너지는 문제가 발생한다.

### 발생 원인

스프링은 의존성 주입을 빈을 만드는 시점에 한 번만 수행하는데, singleton 빈은 컨테이너 기동 시 한 번 만들어지므로, 그때 받은 prototype 인스턴스가 필드에 그대로 박제된다.

```java

@Component
class Singleton {

    @Autowired
    private Prototype prototype;  // 기동 시 P1 받음 → 이후 변경 없음

    void use() {
        prototype.doSomething();  // 첫 호출: P1, 두 번째: P1, 세 번째: P1 ...
    }
}
```

사용자는 호출마다 P1·P2·P3가 나오길 기대했지만, 실제로는 컨테이너 기동 시 받은 P1이 영원히 사용된다.

### 해결 1 - 스코프 프록시 (선언만 변경)

빈 정의에 `proxyMode`를 지정하면, 싱글톤이 받는 건 진짜 빈이 아니라 호출마다 컨테이너에 위임하는 프록시 객체가 된다.

```java

@Component
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
class Prototype { ...
}
```

```java

@Component
class Singleton {

    @Autowired
    private Prototype prototype;  // 실제로는 프록시 객체가 주입됨

    void use() {
        prototype.doSomething();  // 프록시가 호출 시점에 컨테이너 lookup → 새 인스턴스에 위임
    }
}
```

- 호출 측 코드 변경 없음 — 평범한 주입처럼 보이지만 내부적으론 매 메서드 호출마다 컨테이너에서 새로 조회
- 웹 스코프(request·session)에서는 거의 표준 패턴

|       옵션       |   프록시 방식   |    조건    |
|:--------------:|:----------:|:--------:|
| `TARGET_CLASS` | CGLIB (상속) |  구체 클래스  |
|  `INTERFACES`  | JDK 동적 프록시 | 인터페이스 보유 |

### 해결 2 - `ObjectProvider` (호출부에서 명시적 조회)

프록시 대신 빈 공급자를 주입받아, 호출 시점에 명시적으로 `getObject()`를 호출해 새 인스턴스를 가져온다.

```java

@Component
class Singleton {

    private final ObjectProvider<Prototype> provider;

    Singleton(ObjectProvider<Prototype> provider) {
        this.provider = provider;
    }

    void use() {
        Prototype p = provider.getObject();  // 호출 시점마다 새 인스턴스 조회
        p.doSomething();
    }
}
```

- `provider.getObject()`는 사실상 컨테이너의 `getBean()` 호출과 같음
- "이 시점에 새 인스턴스를 가져온다"는 의도가 코드에 그대로 드러남
- 단점: 호출부에 스프링 타입(`ObjectProvider`)이 노출됨

# @Bean·@Configuration으로 빈을 등록하는 상황

> - 빈 등록 수단은 컴포넌트 스캔(`@Component`)과 자바 설정(`@Configuration` + `@Bean`) 두 가지를 보통 사용
> - `@Bean` 활용 사례: 외부 라이브러리 객체, 같은 클래스로 인스턴스 둘 이상, 생성 로직이 필요한 경우
> - `@Configuration`의 핵심은 CGLIB 프록시로 감싸서 `@Bean` 메서드 직접 호출 시에도 싱글톤 보장 (`@Component` + `@Bean` 조합은 깨짐)

## 컴포넌트 스캔 vs @Bean

|   구분    |       컴포넌트 스캔        |    `@Configuration` + `@Bean`    |
|:-------:|:--------------------:|:--------------------------------:|
|  주 용도   |   직접 작성한 비즈니스 컴포넌트   | 외부 라이브러리, 인프라 객체, 같은 클래스 다중 인스턴스 |
|  등록 위치  | 클래스 자체에 `@Component` |           설정 클래스의 메서드            |
|  본문 로직  |  클래스 정의에 종속 (분기 불가)  |       메서드 본문에서 빌더·검증·분기 가능       |
| 가독성/명시성 |          간결          |          생성 과정이 코드로 드러남          |

직접 작성한 비즈니스 컴포넌트는 컴포넌트 스캔, 직접 작성하지 않은 객체나 생성 로직이 필요한 객체는 `@Bean`이 일반적인 선택이다.

## @Bean 활용 사례

### 1. 외부 라이브러리 객체

소스 코드 수정이 불가능한 외부 클래스(`HikariDataSource`, `RedisClient` 등)는 `@Component`를 붙일 수 없으므로 `@Bean`을 통해 등록해야 한다.

```java

@Configuration
class InfraConfig {

    @Bean(destroyMethod = "close")
    HikariDataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://...");
        return new HikariDataSource(config);
    }
}
```

많은 라이브러리들이 Spring Boot 자동 구성(`spring-boot-autoconfigure`)이 클래스패스 존재 여부와 프로퍼티를 보고 이미 빈으로 등록해주는 경우가 많다.

### 2. 같은 클래스로 여러 인스턴스가 필요한 경우

동일 클래스로 서로 다른 설정의 인스턴스를 여러 개 만들어야 하는 경우가 대표적이다.

```java

@Bean
@Qualifier("paymentApi")
RestTemplate paymentRestTemplate(RestTemplateBuilder builder) {
    return builder
            .rootUri("https://payment.example.com")
            .additionalInterceptors(new ApiKeyInterceptor(paymentKey))
            .setConnectTimeout(Duration.ofSeconds(3))
            .build();
}

@Bean
@Qualifier("shippingApi")
RestTemplate shippingRestTemplate(RestTemplateBuilder builder) {
    return builder
            .rootUri("https://shipping.example.com")
            .additionalInterceptors(new HmacInterceptor(shippingSecret))
            .setConnectTimeout(Duration.ofSeconds(10))
            .build();
}
```

### 3. 생성 과정에 로직이 필요한 경우

빌더 호출, 검증, 여러 빈의 조립 등 생성 자체가 한 줄로 끝나지 않을 때 메서드 본문에 로직을 담을 수 있는 `@Bean`이 유용하다.

```java

@Bean
DiscountPolicyRegistry discountPolicyRegistry(List<DiscountPolicy> policies) {
    DiscountPolicyRegistry registry = new DiscountPolicyRegistry();
    policies.forEach(p -> registry.register(p.code(), p));
    if (registry.isEmpty()) {
        throw new IllegalStateException("등록된 DiscountPolicy가 없습니다");
    }
    return registry;
}
```

`@Component`에서도 조합·검증 로직을 `@PostConstruct`로 뺴서 해결할 수 있으나, `@Bean` 메서드는 컴파일 타임에 의존성·검증 실패를 명확히 드러낼 수 있다는 장점이 있다.

## @Configuration의 CGLIB 프록시와 싱글톤 보장

`@Configuration`는 `@Component`를 포함하므로 컴포넌트 스캔 대상이 되고, 컨테이너가 `@Configuration` 클래스를 CGLIB 프록시로 감싸서 관리한다.

```java

@Configuration
class AppConfig {

    @Bean
    Repository repository() {
        return new Repository();
    }

    @Bean
    Service service() {
        return new Service(repository());  // 직접 호출처럼 보이지만 프록시가 가로채 컨테이너 캐시 반환
    }
}
```

- 프록시가 `repository()` 호출을 가로채 이미 등록된 빈이 있으면 그것을 반환
- 결과적으로 `@Bean` 메서드를 코드상 N번 호출해도 인스턴스는 하나만 생성

## destroyMethod 자동 추론

`@Bean(destroyMethod)`를 지정하지 않으면 스프링이 `close()`·`shutdown()` 같은 일반적인 종료 메서드명을 자동으로 찾아 호출한다.

```java

@Bean
SomeClient client() {  // SomeClient에 close()가 있다면 컨테이너 종료 시 자동 호출됨
    return new SomeClient();
}
```

원치 않는 메서드가 의도치 않게 호출되는 사고가 날 수 있으므로, 명시적으로 비활성화하려면 `@Bean(destroyMethod = "")`로 빈 문자열을 지정해야 한다.

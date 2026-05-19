## Q. ArgumentResolver란 무엇인가요?

ArgumentResolver는 Spring MVC에서 Controller 메서드의 파라미터를 분석해 적절한 객체를 생성하고 바인딩해주는 컴포넌트입니다.

이를 통해 Controller는 HttpServletRequest를 직접 파싱하지 않고 비즈니스 로직에 집중할 수 있습니다.

</br>

### ArgumentResolver란??

Spring MVC에서 Controller 메서드의 파라미터를 자동으로 생성하고 바인딩해주는 기능입니다.

예를 들어 아래와 같은 Controller가 있다고 하면

```java
@GetMapping("/users")
publicResponseEntity<?>getUser(@Login Useruser
) {
    ...
}
```

Spring은 Controller를 호출하기 전에 @Login이 붙은 User 객체를 어떻게 생성할까?를 해결해야 합니다.

이때 사용하는 것이 `HandlerMethodArgumentResolver` 입니다.

</br>

**등장 배경**

```java
@GetMapping("/users")
public ResponseEntity<?> getUser(
        @RequestParam Long id,
        @RequestBody UserRequest request,
        HttpServletRequest servletRequest,
        @Login User user
) {
}
```

Spring은 해당 컨트롤러를 호출하기 전에 id, request 객체, 로그인 유저  등을 찾아서 넣어줘야 합니다.

하지만 “파라미터마다 생성 방식이 다르다는” 문제가 있습니다.

그래서 “파라미터 생성 책임을 Resolver 들에게 위임하자”라는 사고로 이어졌습니다.

</br>

**동작 방식**

```
HTTP 요청
 ↓
Filter
 ↓
DispatcherServlet
 ↓
Interceptor
 ↓
HandlerAdapter
 ↓
ArgumentResolver
 ↓
Controller 호출
```

ArgumentResolver는 HandlerAdapter 내부에서 동작합니다.

Controller를 호출하기 전에 각 파라미터를 처리할 수 있는 ArgumentResolver를 찾습니다.

```java
for (HandlerMethodArgumentResolver resolver : resolvers) {

    // 현재 파라미터를 처리할 수 있는 Resolver인지 확인
    if (resolver.supportsParameter(parameter)) {

        // 파라미터 생성 및 바인딩
        Object argument =
                resolver.resolveArgument(...);

        arguments.add(argument);

        break;
    }
}
```

</br>

예를 들어

| 파라미터 | 사용되는 Resolver |
| --- | --- |
| `@RequestParam Long id` | `RequestParamMethodArgumentResolver` |
| `@RequestBody UserRequest` | `RequestResponseBodyMethodProcessor` |
| `@Login User user` | `LoginUserArgumentResolver` (사용자 정의) |

→ Spring은 여러 Resolver 중 현재 파라미터를 처리할 수 있는 Resolver를 선택하여 값을 생성한 뒤 Controller 메서드를 호출합니다.

</br>

> ArgumentResolver를 사용하면 Controller 내부에서 HttpServletRequest를 직접 꺼내 데이터를 파싱하는 코드를 줄일 수 있습니다.
> 즉, 요청 데이터 생성, 바인딩 책임을 Spring MVC가 처리하고, Controller는 비즈니스 로직에 집중할 수 있도록 도와줍니다.

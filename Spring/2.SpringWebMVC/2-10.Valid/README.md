## Q. @Valid 어노테이션과 동작 위치를 설명해주세요.

`@Valid` 어노테이션은 객체의 유효성을 검증하는 역할이며, 주로 Controller에서 요청 DTO를 검증할 때 사용됩니다.

요청이 들어오면 HandlerMethodArgumentResolver가 요청 데이터를 객체로 변환한 뒤,

`@Valid`가 붙어 있으면 Controller 호출 직전에 Bean Validation이 수행됩니다.

검증이 성공하면 Controller가 호출되고, 실패하면 예외가 발생합니다.

</br>

---

@Valid 어노테이션은 객체의 유효성 검증을 수행하라는 의미이며, 주로 DTO 객체에 정의된 Validation 규칙을 검사할 때 사용합니다.

예를 들어 아래와 같은 객체가 있을 때

```java
public class UserRequest {

    @NotBlank
		private String name;

    @Min(1)
		private int age;
}
```

Controller에서 다음과 같이 사용할 수 있습니다.

```java
@PostMapping("/users")
public void save(
        @Valid @RequestBody UserRequest request
) {
}
```

요청이 들어오면 Spring은

1. Request Body를 읽어 UserRequest 객체 생성
2. @Valid 발견
3. 객체의 Validation 수행
4. 검증 성공 시 Controller 호출
5. 검증 실패 시 예외 발생

순서로 동작하고, 

- name 필드에 대해 `@NotBlank`
- age 필드에 대해 `@Min(1)`

검증을 수행하게 됩니다.

예를 들어 아래와 같은 요청이 들어오면:

```json
{
  "name":"",
  "age":-1
}
```

Validation 조건을 만족하지 못하므로 검증 실패가 발생합니다.

</br>

**동작 흐름**

```
HTTP 요청
 ↓
DispatcherServlet
 ↓
HandlerAdapter
 ↓
ArgumentResolver
 ↓
객체 생성
(@RequestBody / @ModelAttribute)
 ↓
@Valid Validation 수행
 ↓
검증 성공 → Controller 호출
검증 실패 → 예외 발생
```

즉 @Valid 는

- 객체 생성 이후
- Controller 호출 이전

시점에 동작합니다.

</br>

**📌 Valid 를 붙일 수 있는 대표 위치**

| 위치 | 의미 |
| --- | --- |
| @RequestBody | 요청 Body 객체 검증 |
| @ModelAttribute | Form/Object 바인딩 검증 |
| 컬렉션 요소 | List 내부 객체 검증 |
| 중첩 객체 필드 | 객체 내부 객체 검증 |
| Method Parameter | 메서드 파라미터 검증 |
| Bean 필드 | 실제 Validation 규칙 정의 |

</br>

<details>
<summary>1. @Request + @Valid</summary>

```java
@PostMapping("/users")
public void save(
        @Valid @RequestBody UserRequest request
) {
}
```
    
**동작 방식**

```
JSON → 객체 생성
    ↓
Validation 수행
    ↓
검증 실패 시 MethodArgumentNotValidException 발생
```

</details>

<details>
<summary>2. @ModelAttribute + @Valid</summary>

```java
@PostMapping("/users")
public void save(
        @Valid @ModelAttribute UserRequest request
) {
}
```

**동작 방식**

```
요청 파라미터 → 객체 바인딩
    ↓
Validation 수행
```

</details>

<details>
<summary>3. 중첩 객체 검증</summary>

```java
public class OrderRequest {

    @Valid
        private UserRequest user;
}
```

**동작 방식**

```
OrderRequest 검증
    ↓
내부 UserRequest까지 재귀적으로 Validation 수행
```

→ 내부 객체도 함께 검증 (Valid가 없다면 내부 객체 검증 안됨)

</details>

<details>
<summary>4. 컬렉션 내부 객체 검증</summary>
    
```java
public class OrderRequest {

        private List<@Valid UserRequest>users;
}
```

**동작 방식**

```
List 순회
    ↓
각 UserRequest 객체 Validation 수행
```

→  List 내부의 객체들을 하나씩 검증합니다.

</details>

<details>
<summary>5. Method Parameter 검증</summary>
    
```java
@Validated
@RestController
public class UserController {

    @GetMapping("/users")
        public void get(
            @Min(1) Long id
    ) {
    }
}
```

**동작 방식**

```
메서드 호출 전
    ↓
파라미터 Validation 수행
```

- 단순 파라미터 검증에 사용
- 클래스에 `@Validated` 필요

</details>

</br>
</br>

### 🚨 주의할 점

1~4번까지의 공통점은 모두 객체(DTO) 내부 필드 검증이라는 점이다. 또한 이 방식은 기본적으로 Controller 계층에서 요청 데이터를 검증하는 용도로 사용된다.

즉, `@Valid`는 Controller로 들어오는 요청 객체를 기준으로 동작하기 때문에 Controller 진입 전에 ArgumentResolver 단계에서 검증이 수행된다.

</br>

그럼 서비스 계층이나 다른 곳에서 들어오는 파라미터도 검증하고 싶으면 어떻게 할까?

이 문제를 해결하기 위해 Spring은 `@Validated`를 제공한다.

`@Validated`는 단순히 DTO 검증만을 위한 것이 아니라

Spring AOP 기반으로 메서드 호출 자체를 가로채서 검증을 수행하는 기능이다.

```java
@Service
@Validated   // 1️⃣ 서비스에 @Validated 추가
public class UserService {

		// 2️⃣ 검증 진행할 파라미터에 @Valid 추가
    public void addUser(@Valid UserRequest request) {
    }
}
```
## **Q. @RequestParam, @RequestBody , @ModelAndAttribute 의 차이**

세 어노테이션의 가장 큰 차이는 요청 데이터를 어디서 읽어오고 어떤 방식으로 바인딩하느냐입니다.

`@RequestParam` 은 Query Parameter나 Form Data 값을 단일 변수에 바인딩할 때 사용합니다.

`@ModelAttribute` 는 여러 요청 파라미터를 객체로 묶어서 바인딩할 때 사용합니다.

반면 `@RequestBody` 는 HTTP Body의 JSON 데이터를 객체로 변환할 때 사용합니다.

</br>

---

@RequestParam, @ModelAttribute, @RequestBody 의 공통점은

HTTP 요청 데이터를 Controller 파라미터에 바인딩하기 위한 어노테이션이라는 점입니다.

다만 어떤 위치의 데이터를 어떤 방식으로 바인딩하느냐에서 차이가 있습니다.

</br>

### 1️⃣ RequestParam

HTTP 요청의 Query Parameter 또는 Form Data 값을 개별 변수에 바인딩할 때 사용합니다.

`RequestParamMethodArgumentResolver` 를 사용합니다.

주로 단순 타입(String, int, Long 등)을 받을 때 사용합니다.

```java
@GetMapping("/users")
publicResponseEntity<?>getUser(
        @RequestParam Long id,
        @RequestParam String name
) {
}
```

</br>

**동작 흐름**

```
HTTP 요청
 ↓
DispatcherServlet
 ↓
HandlerAdapter
 ↓
RequestParamMethodArgumentResolver 선택
 ↓
Query Parameter 조회
 ↓
타입 변환(String → Long)
 ↓
Controller 호출
```

</br>

**예시**

/users?id=1&name=kim 요청이 들어오면 Spring은

@RequestParam Long id를 처리하기 위해 RequestParamMethodArgumentResolver 를 선택하고

```
request.getParameter("id")
```

를 통해 Query Parameter 값을 꺼낸 뒤,

```
"1" → Long 타입 변환
```

을 수행하고 Controller 파라미터에 바인딩합니다.

</br>

**특징**

- Query Parameter 기반
- 단일 값 처리에 적합
- 주로 GET 요청에서 사용
- 내부적으로 RequestParamMethodArgumentResolver 가 처리

</br>
</br>

### 2️⃣ RequestBody

HTTP Request Body의 데이터를 읽어 객체로 변환할 때 사용합니다.

RequestResponseBodyMethodProcessor를 사용합니다.

주로 JSON 기반 API에서 사용합니다.

```java
@PostMapping("/users")
public void save(
        @RequestBody UserRequest request
) {
}
```

Spring은 Request Body를 읽은 뒤 JSON → Java 객체로 변환합니다.

이 과정에서 HttpMessageConverter 와 Jackson 라이브러리를 사용합니다.

</br>

**동작흐름**

```
1. ArgumentResolver 선택
   ↓
RequestResponseBodyMethodProcessor

2. Request Body 읽기
   ↓

3. HttpMessageConverter 호출
   ↓

4. JSON → UserRequest 변환
   ↓

5. Controller 호출
```

→ RequestResponseBodyMethodProcessor 에서 HttpMessageConverter를 사용하는 구조

</br>

**예시**

@RequestBody UserRequest request를 처리하기 위해 RequestResponseBodyMethodProcessor를 선택해서 JSON 값을 읽고

```json
{
  "name": "kim",
  "age": 20
}
```

HttpMessageConverter(`MappingJackson2HttpMessageConverter`)를 통해 자바 객체로 변환합니다.

```java
UserRequest(
    name = "kim",
    age = 20
)
```

</br>

**특징**

- HTTP Body 기반
- JSON/XML 데이터 처리
- 주로 POST, PUT, PATCH 요청에서 사용
- Content-Type 기반으로 동작
- 내부적으로 RequestResponseBodyMethodProcessor 가 처리
- HttpMessageConverter 사용

</br>
</br>

### 3️⃣ ModelAndAttribute

여러 요청 파라미터를 객체로 묶어서 바인딩할 때 사용합니다.

```java
public class UserRequest {

	private String name;
	private int age;
	
	// getter, setter
}
```

```java
@PostMapping("/users")
publicvoidsave(
        @ModelAttribute UserRequest request
) {
}
```

</br>

**동작 흐름**

```
1. ArgumentResolver 선택
   ↓
ModelAttributeMethodProcessor

2. 객체 생성
   ↓
new UserRequest()

3. 요청 파라미터 조회
   ↓
name=kim
age=20

4. DataBinder를 사용한 필드 바인딩
   ↓
setter 호출

5. Controller 호출
```

</br>

**예시**

name=kim&age=20 요청이 들어오면

```java
UserRequest request = new UserRequest();

request.setName("kim");
request.setAge(20);
```

1. 기본 생성자로 객체 생성

2. 요청 파라미터 조회

3. setter를 통해 값 주입

순서로 동작합니다.

이 과정에서 `WebDataBinder` 가 사용되며, 문자열 값을 적절한 타입으로 변환한 뒤 객체 필드에 바인딩합니다.

</br>

**특징**

- Parameter → 객체 바인딩
- HTML Form 요청 처리에 많이 사용
- 기본 생성자 + Setter 필요
- 내부적으로 Data Binding 사용
- 내부적으로 ModelAttributeMethodProcessor 가 처리
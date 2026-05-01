## 제네릭(Generic)을 사용하는 이유와 장단점은 무엇이며, 왜 Object 타입을 직접 사용하는 것보다 권장되나요?

> 제니릭은 클래스나 메소드에서 사용할 타입을 컴파일 시점에 지정하는 기술입니다.
>
> 과거 object 타입을 직접 사용했을 대는 수동으로 형변환이 필요했고, 잘못된 타입이 들어왔을 경우 ClassCastException 이 발생할 수 있습니다.
>
> 제네릭을 사용하면 컴파일러가 타입 안정성을 사전에 체크해 런타임 에러를 방지해주고 불필요한 형변환 코드를 줄여주기에 더 권장됩니다.


### Generic 이란?

- 타입을 파라미터화 하는 방법

클래스나 메소드를 정의할 때 사용할 내부 타입을 확정 짓지 않고, 실제 객체를 생성하거나 메소드를 호출할 때 외부에서 타입을 지정한다.

런타임이 아닌 컴파일 시점에 타입 안정성을 보장한다.

컴파일러가 타입을 자동으로 결정해 줍니다.

### 왜 Object 타입보다 제네릭을 추천할까?

Java 5이전에는 모든 객체의 최상위 부모가 Object 타입이었습니다.

이에 따라 범용적인 컬렉션이 가능합니다.

하지만 다음의 문제점이 있습니다.

1️⃣ 강제 형 변환의 불편함

Object로 꺼낸 데이터를 사용 시 실제 타입으로 다운 캐스팅이 필요합니다.

적어도 한 번의 형 변환이 필요하기에 가독성이 떨어집니다.

2️⃣ 컴파일 에러의 위험

컴파일러는 object 에 무엇이 들어있는지 모르기에, 잘못된 타입으로 형 변환을 시도 시 ClassCastException 이 발생합니다.

런타임 시에는 바로 프로그램이 죽어버리기에 제네릭을 써야 합니다.

🤔 그렇다면 어떤 방식으로 동작될까?

> 타입 소거

1. 컴파일러가 제네릭 타입을 확인해 타입 안정성을 검사한다.

2. <T> 와 같은 타입 파라미터를 comparable 이 있으면 그 타입으로, 없으면 Object 로 변경됩니다.

3. 데이터를 꺼낼 때 적절한 타입으로 자동 형변환 코드가 삽입됩니다.

### 언제 쓸까?

API 공통 응답 형식을 만들 때 

```Java
public class ApiResponse<T> {
    private String status;
    private T data;
    private String message;

    public ApiResponse(String status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("200", data, "Success");
    }
}

// 나중에 사용할 때...
public ApiResponse<UserDto> getUser() {
    UserDto user = userRepository.findById(1L);
    return ApiResponse.success(user); // compiler : T 를 UserDto
}
```

**[PECS (Producer-Extends, Consumer-Super)]**


데이터를 읽어올 때는 `<? extends T>` 를 사용하고

-> 리스트에는 T 또는 T의 자식이 있다.

-> 리스트에는 무엇인지는 모르겠다만 T의 자식이라는 것은 확실합니다. 따라서 안전하게 T 타입을 꺼낼 수 있다. 

-> 만약 여기서 쓰게 된다면, 어떤 자식 타입인지는 모르겠지만, 잘못된 타입을 넣으면 타입 안정성이 깨지기에 add 가 금지된다.

데이터를 삽입/소비할 때는 `<? super T>` 를 사용하자.

-> 리스트에는 T 또는 T의 조상들이 있다.

-> 리스트에는 최소한 T 타입을 수용할 수 있는 공간이 보장되기에 T 나 T의 자식을 안전하게 넣을 수 있다.

-> 만약 여기서 읽게 될 때, 꺼내는 타입이 T 일수도 Object일 수도 있다. 정확한 타입을 알 수 없으니 오직 Object 타입으로만 받을 수 있어 실용성이 떨어진다.

### 장단점

🅾️ **장점**

- 버그를 사전에 방지할 수 있다. (타입 체크)

- 로직은 같으나 타입만 다른 코드 (예: List, Map 등)의 중복을 없앨 수 있다.

- 코드만 보고도 어떤 데이터가 오가는지 알 수 있다.

❎ **단점 및 한계**

- 기본 타입은 사용할 수 없어서 박싱/언박싱 비용이 발생한다.

- 타입 소거 때문에 `instanceof` 연산자나 `new T()` 같은 인스턴스 생성이 불가능하다.
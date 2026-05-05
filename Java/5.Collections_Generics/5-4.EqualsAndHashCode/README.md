## equals()와 hashCode()를 왜 함께 재정의해야 하는지, 구현 시 어떤 점을 유의해야 하는지 설명해주세요.

> 자바의 해시 기반 컬렉션이 객체를 식별할 때 equals()는 true 인 객체는 반드시 동일한 hashCode() 를 가져야 합니다.
>
> 만약 equals() 만 정의하고 hashCode()는 재정의하지 않는다면, 논리적으로 같은 객체라도 해시값이 달라
>
> 서로 다른 버킷에 저장되어 다른 객체로 인식하게 됩니다.
>

자료 구조 안에서 데이터를 찾아내기 위한 `equals()` 와 `hashCode()`

### 1. Default Implementation

***

이 `equals()` 와 `hashCode()` 는 `Object` 클래스에 정의되어 있습니다.

```java
public boolean equals(Object obj) {
    return (this == obj);
}
```

`hashCode()` 는 객체 메모리 주소를 기반으로 생성된 정수값을 반환하는 메서드입니다.

### 2. 그러면 왜 __다시__ 정의해야 할까?

***

자바 API 에는 이렇게 써 있습니다.

> 두 객체 `equals()` 에 의해 같다면, 두 객체의 `hashCode()` 도 반드시 같아야 한다.

즉, 우리는 **`equals()`를 재정의하려면 `hashCode()`도 재정의**해야 합니다.


1️⃣ get(key) 호출 시 키의 `hashCode()` 를 보고 어느 버킷에 들어있는지 찾습니다.

2️⃣ 해당 버킷에 도달하면, 그 안의 노드들을 순회하면서 `equals()` 를 호출해 실제 객체와 **같은지** 확인합니다.

여기서 확일할 수 있는 것은 `equals()` 만 재정의하고 `hashCode()` 를 재정의하지 않았다면,

- 결국 **다른 해시 코드**를 가지고 되어 서로 다른 데이터라고 인식하게 됩니다.

### 예시

***

```java
public class User {
    private final Long id;
    private final String email;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Object.hash(id, email);
    }

}

```

### 재정의 vs 그대로 쓰기

***

- 재정의를 해서 쓸 경우

비즈니스 로직상의 동등함을 구현할 수 있다.

그리고 해시 기반 컬렉션의 성능을 그대로 이용할 수 있다. (O(1))

- 재정의를 하지 않고 사용할 경우

객체 생성 속도가 조금 더 빠를 수 있고 복잡하지 않다.

그러나 DTO나 Key로 사용할 객체의 경우 데이터 찾기 실패가 일어날 수 있다.
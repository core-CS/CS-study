## Auto Boxing 과 UnBoxing 이란 무엇이며, 사용 시 주의해야 할 점은 무엇인가요?

> Auto Boxing 은 자바 컴파일러가 기본 타입에 대응하는 래퍼 클래스로 자동 변환하는 기능, 반대가 Unboxing 입니다.
>
> 사용 시 주의점으로 첫 번째는 성능 그리고 두번째는 예기치 못한 nullpointerexception 입니다.
>
> 과도한 박싱은 불필요한 객체 생성으로 GC 부하를 그리고 `null` 상의 Wrapper 객체를 언박싱 시 에러가 납니다.

자바에는 산술 연산에 최적화되어 있는 기본 타입
- int, long 등

객체로서 기능을 갖춘 Wrapper class
- Integer, Long 등

이 있습니다.

🤔 그러면 왜 둘이 있을까?

-> 결론만 말하자면 `null` 때문이다 !

1. 기본 타입

🅾️ 스택 메모리를 사용해 접근 속도가 빠릅니다. 따라서 오버헤드가 없습니다.

❎ 그러나 제네릭에서 사용이 불가하고 `null` 이 불가합니다.

🤔 제네릭이란?

- 클래스나 메서드에서 사용할 내부 데이터 타입을 컴파일 시점에 미리 지정하지 않고, 사용자가 객체를 생성 시 타입을 결정합니다.


2. Wrapper class

🅾️ `null` 표현이 가능하고 제네릭(`List<T>`) 가 사용이 가능합니다.

- 가능한 이유는 List<String> 을 내부적으로 List<Object> 이기에 가능합니다.

❎ 힙 메모리를 점유해 간접 참조로 성능이 저하될 수 있습니다.

<br><br>

🤔 그럼 언제 써야 될까?
- DTO 나 엔티티에서 **"값이 없다."**를 표현해야 할때만 Wrapper class 사용

- 순수 계산이나 지역 변수의 경우는 반드시 기본 타입을 사용하자.


## Auto Boxing 이란?

> 원시 타입의 값을 해당하는 Wrapper 클래스의 객체로 바꾸는 과정입니다.

자바 컴파일러는 원시 타입이 아래 두 가지에 해당될 때 AutoBoxing을 적용합니다.

1. Passed as parameter to a method that expects an object of the corresponing wrapper class

> 원시 타입이 Wrapper 클래스의 타입의 파라미터를 받는 메서드를 통과할 때

2. Assigned to a variable of the corresponding wrapper class

> 원시 타입이 Wrapper 클래스의 변수로 할당될 때

조금 더 쉽게 말하면, 기본 타입을 대응하는 Wrapper 클래스 객체로 **자동 변환**하는 과정을 Auto Boxing 이라 합니다.

```Java
public class Text {
    private int text;

    public Integer getText() {
        return text;
    }
}
```

- 위에서 컴파일 오류가 발생하지 않습니다.

- 이유는 자바 컴파일러가 **자동으로** Integer 값으로 변환해주기 때문입니다.

위를 수행 시 아래의 과정이 자동으로 됩니다.

```Java
public class Text {
    private int text;

    public Integer getText() {
        return Integer.valueOf(text);
    }
}
```

JS2E 5.0 부터 도입되어서 `new Integer(i)`나 `int.intValue()` 를 호출하지 않아도 자동으로 코드를 삽입해줍니다.

## UnBoxing 이란?

> Wrapper 클래스 타입을 원시 타입으로 변환하는 과정입니다.

자바 컴파일러는 원시 타입이 아래 두 가지 경우에 해당될 때 unBoxing 을 적용합니다.

1. Wrapper 클래스 타입이 원시 타입의 파라미터를 받는 메서드를 통과할 때

2. Wrapper 클래스 타입이 원시 타입의 변수로 할당될 때

다음의 예가 UnBoxing 에 해당합니다.

```Java
public static void main(String[] args) {
    Integer first = new Integer(10);
    // unboxing
    int second = i;

    // auto boxing
    Character firstChar = 'a';
    // unboxing
    char secondChar = firstChar;
}
```


## 그렇다면 주의점 ? 🤔

### 1. 불필요한 객체 생성으로 인한 GC 부하

Wrapper class 를 이용해서 연산을 수행 시 매 단계마다 새로운 객체가 생성될 수 있습니다.

[안티 패턴 예시]
```Java
Long sum = 0L;
for (long i = 0l i < 10000000; i++) {
    sum += i; // 100만 번의 오토 박싱과 객체가 생성됨
}
```

대신 이렇게 써야 합니다.
```Java
long sum = 0L;
for (long i = 0; i < 1000000; i++) {
    sum += i;
}
```

⁇ 그렇다면 우리가 entity에는 Long 그리고 dto에는 long 이라고 적는 것보다 이 둘을 맞추는 것이 좋을까?

- 둘을 가급적이면 맞추는 것이 더 좋다. 

- 둘 다 Long 으로 쓰면, 이 값이 비어있을 수 있다는 `Optional` 의 의미를 전달할 수 있습니다.

⁇ 그렇다면 Entity 에 Long vs long

- 우리는 `null` 이 필요한 상황이 분명 있을 텐데 (이는 미리 고민해보고 선택해야 함) 이 때 long으로 하면 억지로 0을 넣어야 하는 상황이 발생할수도??


### 2. Null Safety

Wrapper 클래스는 `null` 을 가질 수 있지만,

기본 타입은 가질 수 없습니다.

따라서 `null` 인 Wrapper 클래스를 언박싱 시 `NPE` 가 발생됩니다.

```Java
Integer count = null;
int actualCount = count; // NullPointerException
```

또한 Wrapper 클래스는 "객체" 입니다.

`==` 연산자 비교 시 실제 값이 아닌 **주소값**을 비교하기 때문에,

반드시 `.equals()` 를 사용해야 합니다.

(단, Integer의 경우 -128~127 범위는 캐싱 처리되어서 `==` 가 동작할 때도 있습니다.)


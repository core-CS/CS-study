### Q. Java 8에서 추가된 주요 특징들(Lambda, Stream API, Optional 등)에 대해 설명해주세요.

Java 8은 함수형 프로그래밍을 지원하기 위해 Lambda와 Stream API가 추가되었습니다.

Lambda는 익명 함수를 간결하게 표현하여 함수형 인터페이스 기반으로 동작하며, Stream API는 컬렉션 데이터를 선언형 방식으로 처리하면서 내부 반복과 지연 처리, 파이프라인 구조를 통해 데이터를 가공할 수 있게 합니다.

또한 null-safe 처리를 위한 Optional, 인터페이스 확장을 위한 Default Method, 그리고 불변성과 쓰레드 안정성을 개선한 java.time 패키지가 추가되었습니다.

</br>
</br>

**💡 Lambda Expression**

- 익명 함수를 간단한 문법으로 표현한 것
- → 함수를 값처럼 전달할 수 있게 해줌 (함수형 프로그래밍 기반)
- 기본 구조
    
    ```java
    (파라미터) -> 실행할 코드
    ```
    
- 예제1
    - 람다는 익명 클래스에서 메서드를 오버라이딩하던 방식을 간결하게 표현한 것
    
    ```java
    interface Runnable {
        void run();
    }
    
    // 기본적인 Lambda 사용 (Runnable)
    Runnable r = () -> System.out.println("Hello");  // Runnable.run()을 실행하면 System.out.println을 대신 실행하겠다 의미 (람다 메서드 구현)
    r.run();
    ```
    
- 예제2
    - 첫번째 예시와 다르게 람다식 왼쪽에 타입이 없음 (ex.Runnable) → 의미 불명
    - 이런 경우에는 문맥이 필요함 = forEach
    
    ```java
    // 사실 forEach는 이렇게 구현되어 있음
    // T 타입 데이터를 하나씩 꺼내서, 그걸 처리하는 함수(action)를 받아서 실행할게
    void forEach(Consumer<T> action) {
    		for (T t : list) {
    	    action.accept(t);
    		}
    }
    
    // Consumer : 값 하나 받아서 처리하는 함수
    interface Consumer<T> {
        void accept(T t);
    }
    
    // 함수를 값처럼 전달 (컬렉션 처리)
    List<String> list = Arrays.asList("a", "b", "c");
    list.forEach(s -> System.out.println(s));
    
    // 람다식에게 문맥 제공 (forEach의 변수는 Consumer<T> action 이니깐)
    Consumer<T> action = s -> System.out.println(s);  // 그래서 람다가 accept를 구현한 것
    ```

</br>

<details>
<summary>익명 함수란?</summary>
    
    
    이름이 없는 함수로, 필요할 때 즉석에서 정의하여 사용할 수 있는 함수이다.
    
    예를 들어, 일반적인 메서드는 다음과 같이 이름을 가진다.
    
    ```java
    public void hello() {
        System.out.println("Hello");
    }
    
    // 메서드 명 : hello
    ```
    
    하지만 익명 함수는 다음과 같이 정의된다. (이름X)
    
    ```java
    () -> System.out.println("Hello")
    ```
    
    ---
    
    하지만 Java는 함수만 단독으로 정의할 수 없고, 반드시 클래스 안에 있어야 한다.
    
    그래서 Java 8 이전에는 익명 함수를 대신해 익명 클래스를 사용했다.
    
    ```java
    Runnable r = new Runnable() {
        @Override
        public void run() {
            System.out.println("Hello");
        }
    };
    ```
    
    그래서 Java 8부터는 Lambda Expression이 도입되면서, 익명 함수를 간결하게 표현할 수 있게 되었다.
    
    ```java
    Runnable r = () -> System.out.println("Hello");
    ```
</details>

<details>
<summary>Method Rference란?</summary>

    람다를 더 축약한 형태
    
    ```java
    // 람다
    list.forEach(x -> System.out.println(x));
    
    // 메서드 참조
    list.forEach(System.out::println);
    ```

</details>

</br>
</br> 

**💡 Stream API**

- 컬렉션 데이터를 선언형 방식으로 처리하는 API
- 예시
    
    ```java
    List<String> list = Arrays.asList("a", "b", "c");
    
    list.stream()
        .filter(s -> s.equals("a"))
        .forEach(System.out::println);
    ```
    
- 특징
    - 내부 반복  (for문 대신 라이브러리가 반복 처리)
    - 함수형 스타일 지원 (filter, map, reduce 등)
    - 지연 연산 (최종 연산 전까지 실행 X)
    - 병렬 처리 지원 (`parallelStream()`)
- 처리 흐름 (데이터 → 중간 연산 → 최종 연산)
    - 중간 연산 : filter, map, sorted (지연 연산)
    - 최종 연산 : forEach, collect, count (실행 트리거)

</br>
</br>

**💡Optional**

- null일 수 있는 값을 직접 다루지 않고, 컨테이너로 감싸서 NullPointException을 방지하기 위한 객체이다.
- 기존 방식
    - null 체크 반복, 실수하면 NPE 발생
    
    ```java
    String name = getName();
    
    if (name != null) {
        System.out.println(name);
    }
    ```
    
- Optional 사용
    - 값 있으면 실행, null이면 아무것도 안 함
    
    ```java
    Optional<String> name = Optional.ofNullable(getName());
    name.ifPresent(System.out::println);
    ```
    
<details>
<summary>Optional 남용하면 안되는 이유</summary>
    
    **💡 잘못된 사용 예시**
    
    1. 필드에 사용
        - 객체 상태 복잡
        - 직렬화/ORM에서 문제 발생
        - null-safe 구조가 깨짐
    
    ```java
    class User {
        private Optional<String> name;
    }
    ```
    
    1. 파라미터에 사용
        - 호출하는 쪽에서 불필요하게 감싸야 함
        - API 사용성 나빠짐
        
        ```java
        public void printName(Optional<String> name) { }
        
        // 호출하는 쪽 번거로움
        printName(Optional.of("kim"));
        ```
        
    
    **💡 성능 오버헤드**
    
    - 내부적으로 객체 하나 더 생성됨
    - 값 하나 감싸는 wrapper 객체 → 대량 처리 시 불필요한 메모리 증가
    
    ```java
    Optional<String> name = Optional.of("kim");
    ```
    
    **💡 가독성 저하**
    
    - 단순 null 체크보다 오히려 읽기 어려운 경우 있음, 디버깅 어려움
    
    ```java
    Optional.ofNullable(user)
        .map(User::getAddress)
        .map(Address::getCity)
        .ifPresent(System.out::println);
    ```
    
    💡 **예외 처리와 역할 혼동**
    
    - Optional은 값이 없을 수도 있음
    - 실무에서 에러 상황, 비즈니스 예외까지 optional로 처리하려는 경우 → 부적합!!
    
    **💡 컬렉션/DTO에 과하게 사용**
    
    - 구조 복잡도 증가
    - 대부분 null + Optional 중복 의미
    
    ```java
    List<Optional<String>> list;
    ```
</details>

<details>
<summary>Optional 올바른 사용 기준</summary>
    
    - 메서드 return 값이 null 가능할 때
    - “값이 있을 수도 / 없을 수도 있음”이 의미적으로 중요할 때
    
    ```java
    public Optional<User> findUser(Long id)
    ```
</details>

</br>
</br>

**💡 Default Method (인터페이스 개선)**

- 인터페이스에 기본 구현을 제공하여, 구현 클래스가 선택적으로 오버라이드할 수 있게 하는 기능이다.
- 인터페이스에 구현 메서드 추가 가능
- 기존 인터페이스 수정 시 하위 클래스(구현체) 깨지는 문제 해결

```java
Interface MyInterface {
    default void hello() {
        System.out.println("hello");
    }
}
```

</br>
</br>

**💡 Date/Time API (java.time)**

- 기존 Date, Calendar는 값 변경이 가능해서 thread-safe 하지 않았음
    
    ```java
    // 객체 내부 값 자체를 변경할 수 있음 (state 변경)
    Date date = new Date();
    date.setTime(0L);
    
    // 하나의 객체 상태를 계속 변경하면서 사용
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2025);
    ```
    
    - 만약 해당 date, cal이 멀티 쓰레드에서 공유되는 자원이라면
    - Thread A: date.setTime(1000)
    - Thread B: date.setTime(5000)
    - Thread A: date.getTime() → ????
    - 누가 값을 바꿨는지 알 수 없음
    - 공유 자원인데 값을 바꿀 수 있어서 동시에 값 변경이 가능하고, 동기화가 없으면 일관성이 깨짐 → race condition 발생
- 수정 후
    
    ```java
    LocalDate now = LocalDate.now();
    LocalDateTime time = LocalDateTime.now();
    
    // 값 변경이 불가해서 항상 새 객체를 만들어서 사용해야 함
    LocalDate date = LocalDate.now();
    LocalDate newDate = date.plusDays(1);
    ```
    
    - 불변 객체 (immutable) 보장
        - Thread-safe 하다
        - 객체 상태가 변경되지 않기 때문에 동시에 여러 스레드가 접근해도 값이 절대 바뀌지 않음
    - 명확한 API
    - 시간대 지원 (ZonedDateTime)
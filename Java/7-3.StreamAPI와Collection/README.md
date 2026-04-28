### **Q. Stream API의 특징과 Collection을 직접 다룰 때와의 차이점은 무엇인가요?**

Stream API는 컬렉션 데이터를 처리하기 위한 선언형 API로, 내부 반복과 파이프라인 구조를 기반으로 filter, map 등의 연산을 수행하며, 지연 실행(lazy evaluation)과 원본 데이터 비변경이라는 특징을 가집니다.

Collection을 직접 다룰 때는 for문 같은 외부 반복 방식으로 데이터를 순차적으로 처리해야 하지만, Stream은 연산이 pipeline 형태로 연결되어 필요할 때만 실행되기 때문에 불필요한 연산을 줄일 수 있고, findFirst 같은 경우에는 조건을 만족하면 즉시 종료되는 short-circuit 최적화도 가능합니다.

</br>
</br>

일단 Stream API는 데이터를 처리하는 도구이고, Collection은 데이터를 저장하는 자료구조이다.

우리가 직접 Collection을 처리한다면 아래와 같은 특징을 가진다.

- 즉시 실행
    - 외부 반복이라 실행 시점이 코드 흐름과 동일
    
    ```java
    List<String> list = Arrays.asList("a", "b", "c");
    
    for (String s : list) {
        System.out.println("filter 실행: " + s);
        if (s.equals("a")) {
            System.out.println("출력: " + s);
        }
    }
    ```
    
- 원본 데이터 변경 가능
    
    ```java
    List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
    
    list.remove("a");
    
    System.out.println(list); // [b, c]
    ```
    
- 재사용 가능
    
    ```java
    List<String> list = Arrays.asList("a", "b", "c");
    
    list.forEach(System.out::println);
    list.forEach(System.out::println); // 또 사용 가능
    ```
    

하지만 이를 Stream API로 처리한다면 

- 지연 실행 (Lazy)
    - forEach (최종 연산) 전 까지 실행 안됨
    
    ```java
    List<String> list = Arrays.asList("a", "b", "c");
    
    list.stream()
        .filter(s -> {
            System.out.println("filter 실행: " + s);
            return s.equals("a");
        });
    // 아직 아무것도 실행 안됨
    
    System.out.println("==== 실행 시작 ====");
    
    list.stream()
        .filter(s -> {
            System.out.println("filter 실행: " + s);
            return s.equals("a");
        })
        .forEach(s -> System.out.println("출력: " + s));
    ```
    
- 원본 데이터 유지
    - stream은 원본 데이터를 변경하지 않음 (side-effect 최소화)
    
    ```java
    List<String> list = Arrays.asList("a", "b", "c");
    
    list.stream()
        .filter(s -> !s.equals("a"))
        .forEach(System.out::println);
    
    System.out.println(list); // [a, b, c]
    ```
    
- 재사용 불가
    - stream은 한번 소비되면 끝이다
    
    ```java
    Stream<String> stream = Arrays.asList("a", "b", "c").stream();
    
    stream.forEach(System.out::println);
    
    // 이미 사용된 스트림
    stream.forEach(System.out::println); // IllegalStateException 발생
    ```
    
</br>
</br>

**💡 Stream은 왜 Lazy 처리일까?**

= 필요한 데이터만 최소한으로 처리하기 위해서

```java
list.stream()
    .filter(x -> {
        System.out.println("filter: " + x);
        return x > 10;
    })
    .map(x -> {
        System.out.println("map: " + x);
        return x * 2;
    })
    .findFirst();
```

1. 불필요한 연산 제거
- eager : 모든 요소 filter → 모든 요소 map → 첫번째 값 찾기 (비효율, 끝까지 다 돌아야 함)
- lazy : 1개 요소 꺼냄 → filter → map → 조건 만족하면 종료, 아니면 다음 요소 확인 (조기 종료 가능 = short-circuit 가능)
1. 연산 묶어서 최적화 (fusion)
- eager : filter 전체 저장 → 결과 저장, map 전체 결과 → 결과 저장
    - 중간 컬렉션 생성 X
    - 메모리 낭비 X
- lazy : 요소 1개 → filter → map → 다음 요소
    - 한 번에 처리, 중간 저장 X ⇒ 이거를 operation fusion  (연산 결합)이라고 부른다고 함

</br>
</br>

**💡 병렬 처리**

Stream은 **내부 반복**과 **선언형 파이프라인 구조**를 가지고 있기 때문에, 개발자가 실행 방식을 제어하지 않아도 데이터를 자동으로 분할하여 병렬 처리할 수 있다.

```java
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);

list.parallelStream()
    .filter(x -> {
        System.out.println(Thread.currentThread().getName() + " filter: " + x);
        return x % 2 == 0;
    })
    .map(x -> {
        System.out.println(Thread.currentThread().getName() + " map: " + x);
        return x * 10;
    })
    .forEach(x -> {
        System.out.println(Thread.currentThread().getName() + " result: " + x);
    });
```

- 해당 코드를 실행하면 여러 쓰레드 이름이 섞여서 출력된다 ⇒ 각 요소가 나눠서 처리됨
- filter → map → forEach 흐름이 각 스레드에서 독립적으로 수행됨
    - 동작 과정
    - 데이터 자동 분할 (ex. [1,2] [3,4] [5,6]
    - 각 스레드에서 동일 파이프라인 실행 → filter → map → forEach
    - 결과 합침

</br>
</br>

**🚨 Stream은 for보다 느릴 수 있음**

Stream은 내부적으로 함수 호출, 람다, 객체 생성 등의 추상화 비용이 추가되기 때문에 단순 반복에서는 for문보다 느릴 수 있습니다.

1. 추상화 오버헤드

stream은 내부적으로 ‘ Stream → pipeline → lambda → Consumer → method call ‘이런 형식이다.

함수 호출 비용, 람다 객체 생성, 중간 연산 체인 관리 등의 비용이 생길 수 있음

</br>

2. for문은 JVM 최적화에 유리

for문은 구조가 단순하고 예측 가능하기 때문에 JVM의 JIT 최적화에 유리합니다.

JIT 컴파일러는 자주 실행되는 코드를 네이티브 코드로 변환하면서 인라이닝 등의 최적화를 수행하는데, for문은 메서드 호출이 적고 흐름이 단순해 이러한 최적화가 잘 적용됩니다.

또한 for문은 배열이나 리스트를 순차적으로 접근하기 때문에 CPU 캐시 효율이 좋습니다. CPU는 RAM → L3 → L2 → L1 캐시 구조로 데이터를 가져오는데, 순차 접근 패턴에서는 다음 데이터를 미리 예측해서 캐시에 올릴 수 있어 cache hit rate이 높아집니다.

<details>
<summary>인라이닝 최적화</summary>
    
    메서드 호출을 없애고, 메서드 내부 코드를 호출 위치에 직접 붙여 넣는 최적화이다.
    
    ```java
    int result = add(1, 2);
    
    int add(int a, int b) {
        return a + b;
    }
    ```
    
    보통 실행 과정은 add(1,2) 호출 → 스택 프레임 생성 → 함수 이동 → return (이게 다 함수 호출 비용)인데
    
    인라이닝을 적용하면 JIT가 아래와 같이 함수 호출 자체를 없애고, 코드만 남긴다 (함수 호출 비용 없음)
    
    ```java
    int result = 1 + 2
    ```

</details>
</br>

→ 따라서 단순 반복은 for문이 더 좋고, 복잡한 로직을 처리할 때 stream이 적합하다.
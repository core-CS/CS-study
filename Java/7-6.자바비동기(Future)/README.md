### Q. 자바의 비동기 처리 방식들(Future, CompletableFuture 등)을 비교하여 설명해주세요

자바에서는 Future와 CompletableFuture를 통해 비동기 처리를 할 수 있습니다.
Future는 ExecutorService의 submit을 통해 작업을 다른 스레드에서 실행하고 결과를 Future로 받아올 수 있지만, 결과를 조회할 때 get()을 사용해야 해서 blocking이 발생하고, 후속 처리나 작업 조합이 어렵다는 한계가 있습니다.

이를 개선한 것이 CompletableFuture로, 콜백 기반의 체이닝을 통해 비동기 작업 이후의 로직을 연결할 수 있고, 여러 비동기 작업을 조합하거나 예외 처리까지 지원하여 더 유연한 비동기 흐름을 구성할 수 있습니다.

</br>
</br>

**자바의 비동기 처리 방식**

작업을 별도의 스레드에서 수행하고, 현재 스레드는 결과를 기다리지 않고 다른 작업을 수행한 뒤, 필요 시 결과를 받아 처리하는 방식

</br>

**💡 Future**

- 자바의 기본적인 비동기 모델
- `ExecutorService`와 함께 사용
- Future는 비동기 결과를 표현하지만, 결과를 조합하거나 후속 처리를 연결하는 기능은 없다 (단점)

</br>

```java
// 비동기 작업 수행
Future<String> future = executor.submit(() -> {
    return "result";
});

// 결과 조회는 get()을 통해서
String result = future.get();   // get은 blocking으로 동작
```

- `submit`은 작업을 다른 스레드한테 맡기고, 즉시 Future을 반환
- `get` 을 통해서 필요할 때 결과를 요청
    - get은 결과가 준비될 때까지 현재 스레드를 멈추는 Blocking 방식

</br>

**문제점**

- `get`은 blocking 방식으로 동작하여, 비동기 작업을 수행하더라도 결과를 가져오는 시점에는 현재 스레드가 대기하게 된다.
    - 작업이 아직 완료되지 않은 경우 → 완료될 때까지 대기
    - 이미 완료된 경우 → 즉시 반환
- 콜백 기반 처리를 지원하지 않아, 작업 완료 이후의 로직을 유연하게 연결하거나 체이닝하는 것이 어렵다.

</br>
</br>

**💡 CompletableFuture**

이런 Future의 단점을 보완하기 위해서 Java8에 CompletableFuture이 나왔다.

</br>

1. Non-Blocking 콜백 처리 지원

```java
CompletableFuture.supplyAsync(() -> "result")
    .thenApply(res -> res + " processed")
    .thenAccept(System.out::println);
```

- `thenApply`, `thenAccept` 등으로 콜백 체이닝 가능
    - 콜백 체이닝 : 비동기 작업이 끝난 뒤 실행할 작업을 줄줄이 이어 붙이는 방식
- `get` 없이 흐름 제어 가능 → get()을 지원하긴 하지만 이거 쓰면 Future와 똑같은 Blocking 문제 발생

</br>

2. 작업 조합 가능

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "B");

CompletableFuture<String> result =
    f1.thenCombine(f2, (a, b) -> a + b);
```

- 여러 비동기 작업을 쉽게 조합할 수 있음

</br>

3. 예외 처리 기능

```java
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException();
}).exceptionally(e -> "fallback");
```

</br>
</br>

**Q. @Async 기능과 다른건가??**

@Async와 CompletableFuture 모두 비동기를 지원하지만, 추상화 수준이 다르다.

- @Async : 메서드를 다른 스레드에서 실행해줘 (간단한 비동기 실행)
- CompletableFuture : 비동기 흐름을 제어하는 API(조합, 체이닝)

따라서 이메일 발송, 로그 저장, 알림 처리 등 결과는 필요 없고, 그냥 작업만 던져두면 되는 경우에는 @Async를 사용해 비동기 처리를 하고,

결과를 받아서 조합하거나, 흐름이 있는 경우에는 CompletableFuture로 비동기 처리 흐름을 제어한다.

</br>
</br>

**참고 자료**

⬇️ Future의 내부 구조를 이해하고 싶다면

[자바 비동기 파헤치기 1편: Future의 내부 구조](https://aplbly.tistory.com/28)
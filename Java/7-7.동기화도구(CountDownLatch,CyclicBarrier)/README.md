### Q. 멀티스레드 환경에서 동기화를 돕는 CountDownLatch와 CyclicBarrier의 차이점은 무엇인가요?

멀티스레드 환경에서는 실행 순서와 완료 시점이 보장되지 않기 때문에, 스레드 간 실행 타이밍을 맞추기 위한 동기화 도구가 필요합니다.

CountDownLatch는 여러 작업 스레드가 작업을 완료할 때마다 카운트를 감소시키고, 특정 스레드(보통 메인 스레드)는 카운트가 0이 될 때까지 기다렸다가 이후 로직을 수행하는 구조입니다.

반면 CyclicBarrier는 여러 스레드가 서로를 기다리다가, 모든 스레드가 특정 지점에 도달하면 동시에 다음 단계로 진행하는 구조입니다.

즉, CountDownLatch는 작업 완료를 기다리는 용도이고, CyclicBarrier는 여러 스레드의 실행 시점을 맞추는 동기화 도구라는 점에서 차이가 있습니다.

</br>
</br>

일단 멀티 스레드 환경에서 발생 가능한 문제점은 크게 2가지이다.

- 공유 자원 문제
    - Lock / Semaphore / synchronized
- 실행 타이밍 문제
    - CountDownLatch / CyclicBarrier

</br>

멀티스레드는 기본적으로 스레드 간 실행 순서가 보장되지 않고, 각 작업의 완료 시점을 예측하기 어렵다.

예를 들어 Thread1에서 DB 로딩, Thread2에서 API 호출, Thread3에서 파일 로딩을 동시에 수행하는 경우, 일부 작업만 완료된 상태에서 서비스 로직이 실행되면 데이터 불일치나 초기화 오류가 발생할 수 있다.

따라서 여러 스레드의 실행을 특정 시점까지 기다리거나 동기화할 수 있는 도구가 필요하며,
자바에서는 이러한 상황을 해결하기 위해 CountDownLatch와 CyclicBarrier를 제공한다.

</br>
</br>

**1️⃣ CountDownLatch (카운트 내려가기)**

- 여러 스레드 작업이 완료될 때까지 **하나의 스레드가 기다리는** 동기화 도구
- main thread가 latch.await()에서 블로킹 상태로 대기한다

```java
int taskCount = 3;      // 이렇게 초기 카운트 설정
CountDownLatch latch = new CountDownLatch(taskCount);

// Thread1
executor.submit(() -> {
      try {
          //
      } catch (InterruptedException e) {
          //
      } finally {
          latch.countDown(); // 작업 완료
      }
  });
  
// Thread2
executor.submit(() -> {
      try {
          //
      } catch (InterruptedException e) {
          //
      } finally {
          latch.countDown(); // 작업 완료
      }
  });
  
// Thread3
executor.submit(() -> {
      try {
          //
      } catch (InterruptedException e) {
          //
      } finally {
          latch.countDown(); // 작업 완료
      }
  });
  
  
  System.out.println("메인 스레드 대기 중...");
  latch.await();    // 0이 되면 await 풀려서 서비스 시작 !

  System.out.println("모든 작업 완료 → 서비스 시작");

```

- 작업 스레드가 끝날 때마다 `countDown()` 을 호출하여 1씩 감소시킨다.
- taskCount가 0이 되면 `await()` 자동으로 풀린다.
- 카운트가 0이 되면 재사용할 수 없는 1회성 동기화 도구
- 서비스 초기화 시 여러 외부 리소스(DB, API 등)가 준비될 때까지 기다리는 용도로 사용된다.

</br>

**2️⃣ CyclicBarrier (모여서 같이 출발)**

- **여러 스레드**가 모두 특정 지점에 도착해야 **다음 단계로 진행**하는 동기화 도구

```java
CyclicBarrier barrier = new CyclicBarrier(threadCount, () -> {
    System.out.println("👉 모든 스레드가 1단계 완료! 이제 2단계 시작\n");
});   // 모든 스레드가 도착했을 때, 실행되는 barrier action 설정

ExecutorService executor = Executors.newFixedThreadPool(threadCount);

Runnable task = () -> {
    try {
        String name = Thread.currentThread().getName();

        // 🔹 1단계 작업
        System.out.println(name + " → 1단계 작업 시작");
        Thread.sleep((long)(Math.random() * 2000));
        System.out.println(name + " → 1단계 작업 완료");

        // 🔥 여기서 다른 스레드 기다림
        System.out.println(name + " → 대기 중...");
        barrier.await();

        // 🔹 2단계 작업 (모두 동시에 시작됨)
        System.out.println(name + " → 2단계 작업 시작");

    } catch (Exception e) {
        e.printStackTrace();
    }
};

// 스레드 3개 실행
for (int i = 0; i < threadCount; i++) {
    executor.submit(task);
}

executor.shutdown();
```

- 각 스레드는 `await()`에서 대기하며, 모든 스레드가 도착하면 동시에 다음 단계로 진행된다.
- CyclicBarrier는 반복적으로 재사용이 가능하다.
- 병렬 처리에서 단계별 연산을 맞춰야 할 때 사용된다.

</br>

**차이점**

| 구분 | CountDownLatch | CyclicBarrier |
| --- | --- | --- |
| 핵심 개념 | 작업 완료까지 기다림 | 모두 모이면 함께 진행 |
| 동기화 방식 | 한쪽(보통 main)이 기다림 | 모든 스레드가 서로 기다림 |
| 목적 | 결과 수집 / 완료 대기 | 단계별 동기화 |
| 사용 패턴 | N → 0 (countDown) | N개 스레드 → await |
| 실행 흐름 | 작업 끝 → main 진행 | 작업 끝 → 모두 다음 단계 |
| 재사용 | ❌ 불가능 (1회성) | ✔ 가능 (cyclic) |
| 추가 기능 | 없음 | barrier action 지원 |
| 실패 시 영향 | await 무한 대기 가능 | barrier 깨짐 (BrokenBarrierException) |

</br>

**실무 사용 케이스**

| 상황 | CountDownLatch | CyclicBarrier |
| --- | --- | --- |
| 서비스 초기화 | ✔ 매우 많이 사용 | ❌ 거의 안 씀 |
| 여러 API 병렬 호출 후 응답 | ✔ | ❌ |
| 멀티스레드 테스트 코드 | ✔ | ❌ |
| 병렬 알고리즘 (단계별 처리) | ❌ | ✔ |
| 시뮬레이션 / 게임 턴 처리 | ❌ | ✔ |
| 배치 단계 동기화 | ❌ | ✔ |
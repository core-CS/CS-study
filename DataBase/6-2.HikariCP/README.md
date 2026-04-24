### Q. HikariCP 무엇이며, 동작 원리에 대해 설명

HikariCP는 Java에서 사용하는 커넥션 풀 라이브러리입니다.

커넥션 요청 시 ThreadLocal 캐시와 CAS 기반으로 락 없이 커넥션을 가져오고, 풀이 가득 찬 경우에는 Handoff 구조로 대기 중인 쓰레드에게 커넥션을 전달합니다.

이러한 구조를 통해 HikariCP는 락 경쟁과 컨텍스트 스위칭을 줄여 높은 성능을 제공합니다.

</br>
</br>

**💡 동작 원리 + 최적화 방법**

- 목표 : DB 커넥션을 재사용하면서, 최소한의 락과 비용으로 빠르게 획득/반납
- 핵심 구성요소
    - Connection Pool (커넥션 저장소)
    - ConcurrentBag (커넥션 관리 자료구조)
    - HandoffQueue (대기 쓰레드 관리)

</br>

1. 커넥션 요청 (`getConnection()`)
    - 현재 쓰레드가 커넥션 요청
    - ThreadLocal 캐시에서 먼저 탐색
        - 각 쓰레드가 최근에 사용한 커넥션을 기억해두고, 다시 요청 시 우선적으로 재사용하여 탐색 비용을 줄임 ⇒ 전역 탐색 전에 로컬 캐시 확인
    - 없으면 ConcurrentBag에서 유휴 커넥션 탐색
        - 커넥션 상태 (`NOT_IN_USE`, `IN_USE`, `RESERVED`)
        - CAS(Compare-And-Swap) 기반으로 상태 변경
        ⇒ 락 경쟁 최소화 (락 기반 구조에서는 쓰레드가 BLOCK 상태로 전환되면서 컨덱스트 스위칭이 자주 발생)

1. 유휴 커넥션 없는 경우
    1. Pool 여유로움
        - 새로운 커넥션 생성 후 반환
        - 필요할 때만 생성해서 불필요한 초기 생성 비용 방지 (LAZY)
    2. Max Pool Size 도달
        - 쓰레드는 HandOffQueue에서 대기 → 다른 쓰레드가 커넥션 반환하면 대기 중인 쓰레드에게 전달
        - 단순 큐가 아니라 직접 전달 구조
        - polling 비용 제거  → latency 감소

1. 커넥션 반환 (`close()`)
    - 상태를 NOT_IN_USE로 변경 후 pool에 반환
    - 물리 커넥션 종료 없어서 매우 빠름 → CAS 기반 상태 변경만

</br>

> 일반적은 커넥션 풀은 synchronized 기반이라 contention이 발생하지만, 
HikariCP는 ThreadLocal 캐시 + CAS 기반 ConcurrentBag + HandoffQueue를 활용하여 락 경쟁과 컨텍스트 스위칭을 최소화함으로써 매우 빠른 커넥션 획득 성능을 제공하는 커넥션 풀이다.
> 

(contention : 여러 쓰레드가 동시에 하나의 자원을 쓰려고 부딪히는 상황)

</br></br>

**Q. CAS(Compare-And-Swap) 란??**

**동작 방식**

```
if (value==expected) {
value=newValue;
}
```

위 코드는 단일 쓰레드에서는 문제 없지만,

멀티 쓰레드 환경에서는 다음과 같은 문제가 발생한다.

- 값을 확인한 이후 (value == expected)
- 값을 변경하기 전 사이에
- 다른 쓰레드가 값을 바꿔버릴 수 있음

즉, 검사(check)와 변경(set)이 분리되어 있어 race condition 발생

</br>

**CAS의 핵심**

CAS는 이 과정을 하나의 연산으로 묶는다:

```
CAS(value, expected, newValue)
```

- 현재 값이 expected와 같으면 → 변경 (성공)
- 다르면 → 아무것도 하지 않음 (실패)

이 과정이 CPU 레벨에서 원자적으로 수행됨

</br>

**왜 중요한가?**

- 락(synchronized) 없이도 동시성 제어 가능
- 쓰레드를 block시키지 않음
- 컨텍스트 스위칭 감소

→ 높은 성능과 높은 동시성 처리 가능

</br>

**실제 사용 패턴**

CAS는 보통 이렇게 사용됨:

```
while (true) {
	int current=value;
	int next=current+1;
	
	if (CAS(value,current,next)) {
		break;
	}
}
```

→ 실패하면 다시 시도 (retry)
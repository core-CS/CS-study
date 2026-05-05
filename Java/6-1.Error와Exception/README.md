## 자바에서 Error와 Exception의 근본적인 차이는 무엇인가요?

> 근본적인 차이는 복구 가능한지 불가능한 지 입니다.
>
> Error 는 JVM 레벨에서 발생하는 심각한 문제로, OOM이나 오버플러우처럼 애플리케이션에서 대응하기 어렵고 시스템을 종료 시키는 상황입니다.
>
> Exception 은 우리가 로직으로 제어할 수 있어, try-catch 등으로 미리 제어할 수 있습니다. 또한 이는 컴파일 시점에 체크하는 Checked Exception 과 실행 시점에 발생하는 UncheckedException 으로 나뉩니다.

### Throwable

***

자바의 모든 예외와 에러는 `java.lang.Throwable` 로 부터 나온다.

이 상속을 받는 가장 큰 두가지가 **Error** 와 **Exception** 입니다.

1️⃣ **Error**

시스템 레벨의 (심각한) 문제

이는 JVM 리소스 부족, 스택 오버플로우 등 애플리케이션 코드 수준에서 복구가 불가능한 레벨을 의미한다.

2️⃣ **Exception**

애플리케이션 레벨의 문제

파일이 없거나, 네트워크 연결이 끊기거나, 값이 null 인 경우처럼 개발자가 미리 예측하고 대응(catch) 할 수 있는 상태를 의미한다.

### 단계별 동작

***

**Error**
JVM 이 메모리 할당에 실패 시 `OutofMemoryError` 을 던지는데 이는 프로세스 종료로 이어진다.

**Exception**

코드가 null 참조 시 `NullPointerException` 을 던진다.


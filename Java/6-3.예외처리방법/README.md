## 예외를 처리하는 다양한 방법(try-catch, throws, 직접 정의 등)과 각각의 권장 상황은 무엇인가요?
### try-catch
잠재적으로 발생할 수 있는 에러에 대비하여 실현 상태를 유지시킬 수 있는 방법

- try
  - try 블록 안에 예외가 발생할 가능성이 있는 코드 작성 -> 작성한 코드가 예외 없이 정상적으로 동작하면 catch 블록은 실행되지 않고, 예외처리를 종료하거나 finally 블록 실행
- catch
  - try 블록에서 예외가 발생할 경우 실해오디는 코드 블록
  - 여러 종류의 예외 처리 가능
  - catch 블록이 여러개인 경우, 일치하는 하나의 catch 블록만 실행되고 예외처리를 종료하거나 finally 블록으로 넘어가게 됨
- finally
  - 예외 발생 여부와 상관 없이 항상 실행

```java
    static void printStr(String str) {
    String upperStr = str.toUpperCase();
    System.out.println(upperStr);
    }


    public static void main(String[] args) {
      try {
        printStr("Hello");
        printStr(null);
      } catch (NullPointerException e) {
        System.out.println("예외 발생");
      } finally {
        System.out.println("finally 실행");
      }
}
```

- 주의사항
  - catch 문은 if문과 비슷하게 순차적 검사 -> 상위 catch 블록에서 예외 처리를 하면 하위 존재하는 catch 문 실행 X

- 권장상황
  - 예외가 발생한 위치에서 바로 복구하거나, 사용자에게 적절한 응답을 줄 수 있을 때 사용
  - ex) 파일 읽기 실패 시 기본 파일 사용하거나 외부 API 호출 실패 시 재시도, 예외 로깅한 뒤 비즈니스 예외로 변환하는 경우
  - 단순 catch만 하고 아무 처리도 하지 않는 것은 좋지 않음!
### throws
예외를 호출한 곳으로 다시 예외를 떠넘기는 것

throws를 사용하면 자바의 JVM이 최종적으로 예외의 내용을 콘솔에 출력하여 예외처리 수행

```java
    public static void main(String[] args) {
        try {
            throwException();
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    static void throwException() throws ClassNotFoundException, NullPointerException {
        Class.forName("예외 발생");
    }
```
throws 키워드를 사용하여 해당 예외를 발생한 메서드 안에서 처리하는 것이 아닌, 메서드를 호출한 곳을 다시 떠넘김. 따라서 예외 처리 책임은 throwException 메서드가 아닌 main 메서드가 지게 됨!

- 권장상황
  - 현재 계층에서 예외를 처리할 적절한 방법이 없고, 상위 계층이 더 적절하게 처리할 수 있을 때 사용
  - ex) repository나 service 내부에서 발생한 예외를 controller 또는 전역 예외 처리기에서 일관되게 처리
  - 무분별산 throws exception처러 최상위 예외를 던지면 호출자가 어떤 예외를 처리해야하는지 알기 어려워지므로 피하는 것이 좋음!

* throw
  throws와 유사한 throw 키워드는 의도적으로 예외를 발생시킬 수 있음
  ```java
  public void withdraw(int amount) {
    if (amount <= 0) {
        throw new IllegalArgumentException("출금 금액은 0보다 커야 합니다.");
    }
  }
  ```
  잘못된 인자, 불가능한 상태, 비즈니스 규칙 위반 등을 명확히 알리고 싶을 때 사용
### 직접 예외 정의
비즈니스 의미가 있는 예외를 표현하기 위해 직접 예외 클래스 정의
```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("사용자를 찾을 수 없습니다.");
    }
}

if (user == null) {
        throw new UserNotFoundException();
}
```
- 권장상황
  - 단순한 시스템 예외가 아니라 도메인 규칙 위반이나 비즈니스 실패를 명확히 표현하고 싶을 때 사용
  - ex) 회원 없음, 권한 없음, 잔액 부족 등등
  - 예외만 봐도 어떤 문제가 발생했는지 명확하고 전역 예외 처리에서 사앹 코드와 메시지 매핑이 쉬움

### 예외를 처리하는 다양한 방법(try-catch, throws, 직접 정의 등)과 각각의 권장 상황은 무엇인가요?
> 자바에서 예외를 처리하는 방법은 대표적으로 try-catch, throws, throw, 그리고 사용자 정의 예외가 있습니다.
> 
> try-catch는 예외가 발생한 위치에서 직접 처리하는 방식입니다. 현재 메서드 안에서 예외를 복구할 수 있거나, 대체 로직을 수행할 수 있을 때 사용합니다. 예를 들어 잘못된 입력을 다시 받거나, 파일 읽기에 실패했을 때 기본값을 사용하는 경우입니다.
>
> throws는 현재 메서드에서 예외를 처리하지 않고 호출한 쪽으로 책임을 넘기는 방식입니다. 현재 메서드가 예외를 어떻게 처리할지 판단하기 어렵고, 호출자가 더 적절하게 대응할 수 있을 때 사용합니다.
> 
> throw는 개발자가 의도적으로 예외를 발생시키는 방식입니다. 메서드 인자가 잘못되었거나, 객체의 상태가 올바르지 않거나, 특정 조건에서 더 이상 정상 흐름을 이어갈 수 없을 때 사용합니다.
> 
> 사용자 정의 예외는 기본 예외만으로 의미를 명확히 표현하기 어려울 때 직접 예외 클래스를 만들어 사용하는 방식입니다. 예를 들어 단순히 RuntimeException을 던지는 것보다 BalanceNotEnoughException처럼 구체적인 이름의 예외를 만들면, 어떤 문제가 발생했는지 코드만 봐도 명확하게 알 수 있습니다.
> 
> 정리하면, 복구 가능한 위치에서는 try-catch로 처리하고, 현재 위치에서 처리할 책임이 없으면 throws로 위임하며, 잘못된 상태나 규칙 위반은 throw로 명확히 알립니다. 그리고 예외의 의미를 더 분명히 표현하고 싶을 때 사용자 정의 예외를 사용합니다.
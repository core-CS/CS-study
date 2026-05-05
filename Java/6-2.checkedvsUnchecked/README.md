## Checked Exception과 Unchecked Exception의 차이점을 발생 시점과 처리 의무 관점에서 설명해주세요.

> 가장 큰 차이점은 컴파일러의 체크 여부와 예외 처리의 강제성
>
> Checked 는 컴파일 시점에 체크되어 try-catch 나 throws 로 반드시 처리해야 합니다. 주로 외부 리소스와의 상호작용으로 인해 개발자가 제어 불가한 요인으로 인해 발생합니다.
>
> Uncheked 는 런타임에 발생하며 처리를 강제하지는 않습니다. 주로 null 참조, 인자 전달의 오류 등 로직 에서 발생하는 오류입니다.
>
> 실무 관점에서 Spring의 @Transactional 은 기본적으로 unchecked 만 롤백하기에, Checked 를 사용할 때 반드시 롤백 설정을 확인하거나, Unchecked 로 매핑해 처리하는 전략을 선호합니다.

`Exception` 클래스를 상속받는 예외 그룹

1. checked Exception

- RuntimeException 을 상속받지 않는 예외들

- IOException, SQLException 등

-> 외부 요인으로 인해 발생할 확률이 높아 호출자가 반드시 복구 전략을 세워라

2. Unchecked Exception 

- RuntimeException 을 상속받는 예외들

- NullPointerException, IllegalArgumentException 등

-> 주로 프로그래밍 오류로 코드 수정을 통해 해결해야 하고, 굳이 모든 것을 강제로 예외 처리 할 필요는 없다.

| Checked | Unchecked |
| --- | ----|
| 컴파일 시점에 컴파일러가 | 런타임 시점에 |
|명시적으로 처리 필수 (try-catch 또는 throws) | 명시적으로 처리는 선택이다. |
| 환경적인 요인으로 발생 | 로직으로 인해 발생 | 
| 기본적으로 롤백 불가 | 롤백 가능 |

### Checked Exception 의 문제??

***

코드의 가독성을 해칠 수 있다. 
그래서 가급적으로 Checked 를 Unchecked 로 감싸서 던지는 패턴을 적용한다.

```java
public void processFile(String path) {
    try {
        List<String> lines = Files.readAllLines(Paths.get(path)); // IOException 발생 (Checked)
    } catch (IOException e) {
        // Checked를 Unchecked로 변환하여 상위 레이어로 전파
        throw new RuntimeException("파일 읽기 중 시스템 오류 발생", e);
    }
}
```

`@Transactional` 은 기본적으로 Unchecked Exception 이 발생했을 때만 롤백을 수행한다.

원한다면 명시적으로 `@Transactional(rollbackFor = Exception.class)` 가 필요하다.

```java
@Transactional
public void registerUser(User user) throws Exception {
    userRepository.save(user);
    if (someCondition) {
        throw new Exception("Checked Exception 발생!"); // 롤백 안 됨! 데이터 저장됨.
    }
}

```
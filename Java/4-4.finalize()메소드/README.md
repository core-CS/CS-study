## finalize() 메소드를 수동으로 호출하거나 의존하는 것이 왜 위험한지 설명해주세요.
### finalize()
Object 클래스에 있는 메서드로, gc가 해당 object를 참조하는 레퍼런스가 없을때 호출함

=> 객체가 GC에 의해 제거되기 직전에 호출되는 메서드로 메모리에서 제거되기 전에 정리 작업ㅇ르 수행할 수 있도록 설계된 메서듣

### 위험성
- 언제 실행될지 알 수 없음
  특히 타이밍이 중요한 작업은 절대 finalize로 실행되게 해서는 안됨!
- 인스턴스 반납을 지연시킬 수 있음
  finalize 스레드는 다른 애플리케이션 스레드보다 우선순위가 낮아서 대기열에서 반납되길 기다리다가 그래도 메모리가 터질 수 있음
- 아예 실행이 안될 수 있음
- 심각한 성능 문제가 발생할 수 있음
  finalize()가 있는 객체는 일반 객체보다 GC 처리 과정이 복잡

  -> queue 등록, finalizer thread 실행, 이후 재GC 같은 과정이 필요할 수 있음

- 보안 이슈
  ex) finalize attack -> finalize() 메서드를 악용하여 객체의 수명 주기 동안 민감한 정보를 노출시키거나, 객체의 상태를 변경하거나, 보안에 취약점을 만들어내는 공격 기법

### 대체 방법
- try-with-resource
  자원을 명시적으로 닫는 코드를 작성하지 않고도, 안전하게 자원을 관리할 수 있도록 설계된 구문

  AutoClosable 또는 Closable 인터페이스를 구현한 자원에 한해 사용가능하며, 자원을 자동으로 닫을 수 있도록 close() 메서드가 자동으로 호출됨.

  주로 I/O 관련 클래스에서 이 인터페이스들을 구현

    ```java
    try (BufferedReader br = new BufferedReader(new FileReader("data.txt"))) {
        String line = br.readLine();
        System.out.println(line);
    } catch (IOException e) {
        e.printStackTrace();
    }
    ```
- Spring 개발 환경
  외부 API를 제외한 대부분의 자원을 IoC(제어의 역전)와 DI(의존성 주입)를 활용하여, 자원의 관리를 자동화

  대부분의 자원들을 Spring Container에서 자동으로 생성하고 관리하므로, Bean으로 등록된 객체들은 개발자가 close()메서드를 명시적으로 호출할 필요가 없음

    ```java
    @Service
    public class UserService {
    
        private final UserRepository userRepository;
    
        public UserService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }
    
        public User findUser(Long id) {
            return userRepository.findById(id)
                    .orElseThrow();
        }
    }
    ```

  개발자가 직접 userRepository.close()나 userService.close()를 하지 않음!

### finalize() 메소드를 수동으로 호출하거나 의존하는 것이 왜 위험한지 설명해주세요.
> finalize() 메서드에 의존하는 것이 위험한 이유는 실행 시점과 실행 여부가 보장되지 않기 때문입니다. 객체가 GC 대상이 되었다고 해서 finalize()가 바로 호출되는 것은 아니며, JVM 종료나 메모리 상황에 따라 아예 실행되지 않을 수도 있습니다.
> 
> 또한 finalize()가 있는 객체는 GC 대상이 되어도 즉시 메모리에서 회수되지 않고 finalizer queue에 들어가 finalizer thread가 처리할 때까지 대기합니다. 이 과정에서 finalizer thread의 처리 속도가 느리면 객체들이 큐에 쌓여 메모리 회수가 지연되고, 심하면 OutOfMemoryError가 발생할 수 있습니다.
> 
> 그리고 finalize()를 수동으로 호출하는 것도 위험합니다. 직접 호출하면 일반 메서드 호출일 뿐, GC나 메모리 회수와는 아무 관련이 없습니다. 오히려 자원이 이미 정리되었는데 다시 정리하려 하거나, 객체 상태가 불안정해지는 문제가 생길 수 있습니다.
> 
> 따라서 자원 정리는 finalize()에 맡기지 말고, try-with-resources, close(), 또는 Spring Bean의 생명주기 관리처럼 명확하고 예측 가능한 방식으로 처리해야 합니다.
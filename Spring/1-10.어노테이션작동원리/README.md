# 어노테이션의 작동 원리와 Lombok @Data 지양 이유

> - 어노테이션은 코드 자체에 동작을 주지 않는 단순한 메타데이터일뿐, 누군가가 읽고 해석해야 의미가 생김
> - `@Retention`은 어디까지 정보가 살아남을지 결정: SOURCE(컴파일 시 제거) / CLASS(.class까지 유지) / RUNTIME(런타임 리플렉션 가능)
> - 런타임 리플렉션(스프링 `@Transactional` 등)과 컴파일 타임 어노테이션 프로세서(Lombok, MapStruct, QueryDSL) 두 가지 방식으로 어노테이션 처리
> - Lombok `@Data`는 `@Setter`·`@EqualsAndHashCode`·`@ToString`을 한꺼번에 묶기 때문에 불변성 파괴, JPA 양방향 연관관계 무한 루프 등 부작용이 큼

## 어노테이션의 본질

어노테이션은 그 자체로는 아무 동작도 하지 않는 메타데이터에 불과하다.

- 클래스나 메서드에 어노테이션을 붙인다고 해서 어떠한 기능이 자동으로 적용 되는 것이 아님
- 그 어노테이션을 읽고 해석해 동작을 만들어내는 기능 정의 또는 프레임워크가 필요

```java
public @interface Test {

}  // 그냥 마커, 자체로는 아무 기능 없음
```

## 메타 어노테이션

어노테이션 자체를 정의할 때 붙이는 어노테이션으로, 어노테이션의 특성을 결정한다.

|     어노테이션     |                 역할                 |
|:-------------:|:----------------------------------:|
| `@Retention`  |  어디까지 유지될지 (SOURCE/CLASS/RUNTIME)  |
|   `@Target`   | 어디에 붙일 수 있는지 (TYPE/METHOD/FIELD …) |
| `@Documented` |             Javadoc 포함             |
| `@Inherited`  |           하위 클래스가 상속받는지            |
| `@Repeatable` |       같은 어노테이션 여러 번 부착 허용 여부       |

### @Retention 세 가지

|     정책     |        유지 범위         |       용도        |                       예시                        |
|:----------:|:--------------------:|:---------------:|:-----------------------------------------------:|
|   SOURCE   |      컴파일 시점에 제거      |     컴파일러 힌트     |        `@Override`, `@SuppressWarnings`         |
| CLASS(기본값) | `.class`까지 유지, 런타임 X | 컴파일 후 바이트코드 후처리 |                  Lombok 등이 활용                   |
|  RUNTIME   |       런타임에도 유지       | 리플렉션으로 읽어 동적 처리 | `@Transactional`, `@Autowired`, JPA `@Entity` 등 |

자바 코드는 소스(`.java`) → 바이트코드(`.class`) → JVM 메모리 → 런타임 실행 순으로 흐르고, `@Retention`은 어노테이션 정보가 이 흐름의 어느 지점까지 남아있는지를 결정한다.

|        단계         | SOURCE | CLASS | RUNTIME |
|:-----------------:|:------:|:-----:|:-------:|
|  `.java` 컴파일 시점   |   O    |   O   |    O    |
|  `.class` 바이트코드   |   X    |   O   |    O    |
| JVM 메모리 (리플렉션 가능) |   X    |   X   |    O    |

- SOURCE: 컴파일러가 검증·경고에만 사용하고 `.class`에는 기록하지 않음(바이트코드를 열어 봐도 흔적이 없고 런타임에도 존재하지 않음)
    - `@Override`는 메서드가 부모를 실제로 오버라이드하는지 컴파일러가 확인하는 용도로만 쓰이고, 검증이 끝나면 사라짐
- CLASS: `.class`에는 남지만 JVM에 로드될 때 사라짐
    - jar에는 들어 있으나 런타임 리플렉션으론 접근 불가
- RUNTIME: 실행 중 메모리에도 남아 있어 리플렉션으로 읽어 처리할 수 있음
    - 스프링은 컨테이너 기동 시 빈 클래스를 리플렉션으로 검사해 `@Transactional`이 보이면 프록시로 감싸 트랜잭션 시작·커밋 로직을 끼워넣음
    - 어노테이션 정보가 런타임에 메모리에 살아 있어야 이런 동적 처리가 가능

## 두 가지 처리 방식

어노테이션을 실제 동작으로 연결하는 방법은 크게 두 가지다.

### 1. 런타임 리플렉션

`RetentionPolicy.RUNTIME`으로 유지된 어노테이션을, 실행 중에 리플렉션 API로 읽어 처리한다.

```java
void example() {
    Method method = ...;
    if (method.isAnnotationPresent(Transactional.class)) {
        // 트랜잭션 시작·커밋 로직을 끼워넣음
    }
}
```

스프링 프레임워크 대부분이 이 방식으로, 컨테이너가 빈을 등록하고 프록시를 만들 때 리플렉션으로 어노테이션을 검사한다.

### 2. 컴파일 타임 어노테이션 프로세서

`javac`의 어노테이션 프로세싱 단계에 후킹해서, 컴파일 도중 추가 코드를 생성하거나 AST(Abstract Syntax Tree)를 수정한다.

|    도구     |                     역할                      |
|:---------:|:-------------------------------------------:|
|  Lombok   | AST 직접 조작 — getter/setter/equals 등을 클래스에 삽입 |
| QueryDSL  |           `@Entity`를 읽어 Q 클래스 생성            |

이 방법은 IDE/빌드 설정 필요(`annotationProcessor` 의존성, IntelliJ Annotation Processing 활성화)하다는 점을 주의해야 한다.

#### AST와 Lombok의 동작

AST(Abstract Syntax Tree) 는 컴파일러가 소스 코드를 파싱해 만드는 트리 구조의 중간 표현으로, 컴파일은 소스 → 파싱 → AST → 바이트코드 순서로 진행된다.

- 대부분의 프로세서(QueryDSL)는 새 `.java`를 생성하는 안전한 방식으로 사용
- Lombok은 비공식 API(`com.sun.tools.javac.tree.JCTree`)로 컴파일러가 만든 기존 클래스의 AST에 메서드 노드를 직접 삽입
    - 예를 들어 `@Getter`가 붙은 클래스의 AST에 `getName()` 노드를 끼워 넣으면, 컴파일러는 이를 모른 채 그대로 바이트코드로 변환해 `.class`에 메서드가 포함

## Lombok @Data를 지양하는 이유

`@Data`는 다음 어노테이션을 한꺼번에 적용하는 묶음 어노테이션으로, 편리하지만 부작용이 크다.

```
@Getter + @Setter + @RequiredArgsConstructor + @ToString + @EqualsAndHashCode
```

### 1. @Setter — 불변성 파괴

모든 필드에 setter가 자동 생성되어 객체가 언제든 변경 가능한 상태가 된다.

- setter가 모두 노출되면 "어디서든 상태를 바꿀 수 있다"는 신호가 되어, 응집도가 무너지고 변경 추적이 어려워짐
- 상태 변경은 의미를 가진 메서드(`changeName`, `markAsPaid` 등)로 표현하는 게 객체지향적 설계에 더 적합

### 2. @EqualsAndHashCode

기본적으로 모든 필드를 사용해 `equals`·`hashCode`를 생성하는데, JPA 엔티티에 적용하면 다음 문제가 발생한다.

- 양방향 연관관계 무한 루프: A가 B를 참조하고 B가 A를 참조하는 경우, `A.equals(...)`가 B의 `equals`를 호출하고 다시 A의 `equals`를 호출하는 무한 루프가 발생
- 지연 로딩 강제 초기화: 비교 시점에 LAZY 필드를 건드려 의도치 않은 쿼리 발생
- 컬렉션 키 불일치: 필드 값이 변경되면서 `hashCode`가 바뀌어, 영속화 전에 `Set`·`Map`에 넣어둔 객체를 더 이상 찾지 못함

엔티티의 `equals`·`hashCode`는 비즈니스 식별자 기준으로 직접 작성하는 것이 안전하다.

### 3. @ToString

전 필드 toString도 양방향 연관관계에서 무한 루프를 일으키며, 비밀번호·토큰 같은 민감 필드까지 그대로 로그에 찍힐 위험이 있다.

## `String` 의 불변성과 관련해 String Constant Pool 의 역할은 무엇인가요?

> String은 불변인 것이 가능한 것이 String Contant Pool의 덕분입니다.
>
> Contatnt Pool 은 힙 메모리 내부에 동일 문자열 리터럴을 하나만 저장하고 공유하는 캐싱 메커니즘 입니다.
>

Java 에서 `String` 객체는 한 번 생성되면 그 값을 변경할 수 없습니다.

string.java 를 보면, 내부의 실제 데이터 저장소인 `byte[]` 가 final 로 선언되어 있는 것을 볼 수 있습니다.

또한 이에 대한 수정 메서드는 존재하지 않습니다.


```Java
String s1 = "Hello";
String s2 = "Hello";
String s3 = new String("Hello");

System.out.println(s1==s2); // true - Pool 내 같은 객체 참조

 System.out.println(s1==s3); // false - Heasp 내 별도 객체

 String s4 = s3.intern();
 System.out.println(s1==s4); // true - 강제로 Pool 에 넣거나 있는 것을 가져옴

```

## String Constant Pool

이는 **힙 영역 내부에 위치한 저장소**로, 동일한 문자열 리터럴이 중복 생성되는 것을 방지합니다.

🤔 문자열 리터럴

- 소스 코드 상에서 고정된 값

- 예를 들어 정수 10, 문자열 'Java', Boolean 등이 이에 해당

- 코드 가독성을 위해 리터럴을 직접 사용하기 보다는 의미 있는 상수로 치환해 사용하는 것을 추천


`String s = "Java"` 코드가 실행 시 JVM 이 이 Pool 에 "Java" 라는 값이 있는지 체크후 있다면 해당 객체의 참조값만 반환하게 되어 이를 사용합니다.

🔎 어떻게 작동하나요?

- String Interning 이라는 캐싱 메커니즘을 사용합니다.

JVM이 내부에서 `StringTable` 이라는 이름의 **HashTable**을 사용해서 캐시를 관리합니다.

1. 조회 : 문자열 리터럴을 만나면 JVM이 StringTable 에서 해당 문자열의 해시값을 기준으로 기존 객체가 존재하는지 검색합니다.

2. Cache Hit : 이미 존재 시 새로 객체를 만들지 않고, 캐시된 객체의 주소값만 스택 변수에 전달됩니다.

3. Cache Miss : 존재하지 않는다면, 힙 영역에 새로운 String 객체를 생성, 그 참조를 StringTable 에 등록한 뒤 반환합니다.


🅾️ 그렇기에 동일 문자열이 수만 번 들어와도 메모리에는 단 하나만 존재하기에 **메모리 절약**이 가능합니다.

❎ 그렇지만 문자열을 수정시 (`s += "wordl"` ) 시 기존 객체가 수정되는 것이 아니라 **새로운 객체**가 계속 생성됩니다. (GC의 대상)

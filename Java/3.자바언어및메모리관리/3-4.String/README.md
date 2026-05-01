## `String`, `StringBuilder`, `StringBuffer` 의 차이점을 가변성(Mutability)와 스레드 안정성 관점에서 설명해주세요.

> String 은 불변 객체이기에 값을 바꿀 때마다 새로운 메모리 영역을 할당받습니다.
>
> StringBuilder와 StringBuffet 모두 가변 객체로 내부 버퍼를 직접 수정합니다.
>
> 둘의 차이는 StringBuilder은 동기화를 지원하지 않지만, StringBuffer은 메서드별 동기화 처리가 되어 있어 멀티 스레드에서 안전하지만 오버헤드가 있습니다.


### 1️⃣ String

- 불변이다.

String 은 내부적으로 `final` 키워드를 가지고 있습니다. (다음 챕터에서 더 설명)


- Thread Safe


- 연산 속도가 느리다.

`+` 연산이 될 때 기존 메모리 수정이 아니라 새로운 메모리 영역을 할당받아 새로운 객체를 만듭니다.


### 2️⃣ StringBuilder

- 가변이다.

가변적인 Buffer을 가지는데, 문자열을 추가할 때 새로운 객체를 만드는 것이 아닌 내부 배열의 크기를 조절해 직접 수정한다.

- Thread Unsafe


- 연산 속도가 빠르다.

```Java
String result = "";

for (int i = 0; i < 10000; i++) {
    result += i;
}

// instead ... 
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    sp.append(i);
}
String finalResult = sb.toString();

```


### 3️⃣ StringBuffer

- 가변이다.

가변적인 Buffer을 가지는데, 문자열을 추가할 때 새로운 객체를 만드는 것이 아닌 내부 배열의 크기를 조절해 직접 수정한다.

- Thread Safe

`synchronized` 키워드가 붙어 있어 한 thread 가 작업 중일 때 다른 스레드가 사용하지 못한다.

- 연산 속도 중간 

가 중간인 이유는 동기화가 되어 있기 때문에 점유중일 때 사용하지 못하기 때문

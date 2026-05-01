## GC의 실행 시점은 언제이며, 특정 변수가 null이 되었을 때 바로 GC의 대상이 되나요?
GC의 실행 시점은 JVM이 필요하다고 판단될 때!!

1. Young 영역이 가득 찼을 때
2. Old 영역이 부족할 때
3. 메모리 압박이 있을 때
4. System.gc()를 호출했을 때
   - System.gc()의 경우, GC 실행 요청일뿐! JVM이 반드시 즉시 실행되는 것은 아님


### GC의 대상
모든 객체의 참조가 모두 null 일 경우 GC의 대상이 됨!

1. 모든 객체 참조가 null인 경우
   ```java
   Person p1 = new Person();
   Person p2 = p1;
   
   p1 = null;
   p2 = null;
   ```
   
2. 객체가 블록 안에서 생성되고 블록이 종료된 경우
   ```java
   public void method() {
    if (true) {
        Person p = new Person();
    }
   }
   ```
   
   => 만약 블록 안 p를 블록 밖에서 참조하고 있으면 **GC 대상 XX**

3. 부모 객체가 null이 된 경우, 자식 객체는 자동적으로 GC 대상
   ```java
   class Parent {
        Child child = new Child();
   }
   
   Parent parent = new Parent();
   parent = null;
   
   Parent parent = new Parent();
   Child c = parent.child;
   
   parent = null
   ```
   
4. 객체가 Weak 참조만 가지고 있을 경우
   ```java
   WeakReference<Person> weakRef = new WeakReference<>(new Person());
   ```
5. 객체가 Soft 참조이지만 메모리 부족이 발생한 경우
   ```java
   SoftReference<Person> softRef = new SoftReference<>(new Person());
   ```

다만 여기서 중요한 점은 GC의 대상이 되었다고 GC가 실행되거나 즉시 메모리에서 제거되는 것은 아님!!

### 참조 유형
Java 1.2부터 프로그래머들이 reachable 객체를 더 자세히 구별하여 GC 동작을 다르게 지정할 수 있도록

strong, soft, weak, phantom으로 지정 가능

1. Strong References (강한 참조)
   Java의 기본 참조 유형
   ```java
   Person p = new Person();
   ```
   p가 참조를 가지고 있는 한 GC의 대상이 되지 않음!
2. Soft References (소프트 참조)
   대상 객체를 참조하는 경우가 SoftReference 객체만 존재하는 경우 GC 대상이 됨!

   단, JVM 메모리가 부족한 경우에만 힙 영여겡서 제거되고 메모리 부족하지 않다면 굳이 제거하지 않음!
      ```java
      person p = new Person();
      SoftReference<Person> softPerson = new SoftReference<Person>(p);
   
      p = null;
   
      p = softPerson.get();
      ```
   
   softPerson.get()에서 JVM의 메모리가 부족하지 않아 GC 실행 대상이 되지 않은 경우 null이 반환되지 않고 기존 객체 반환!

3. Weak References (약한 참조)
   대상 객체를 참조하는 경우가 WeakReferences 객체만 존재하는 경우 GC 대상이 됨
   
   다음 GC 실행시 무조건 힙 메모리에서 제거
   ```java
   Person p = new Person();
   WeakReference<Person> weakP = new WeakReference<Person>(p);
   
   p = null;
   
   ref = weakP.get();
   ```
   weakP.get()에서 다음 GC 실행시 무조건 힙 메모리에서 제거 -> 제거된 경우 null 반환
   
4. Phantom References (팬텀 참조)
   가장 약한 참조로 객체가 GC에 의해 회수되기 직전에 이 객체가 곧 정리될 예정이라는 사실을 감지하기 위해 쓰는 참조
   ```java
   ReferenceQueue<Person> queue = new ReferenceQueue<>();

   Person p = new Person();

   PhantomReference<Person> phantomPerson =
           new PhantomReference<>(p, queue);

   p = null;
   
   System.gc();
   
   System.out.println(phantomRef.isEnqueued());

   System.out.println(phantomRef.get()); 
   
   ```
   Soft, Weak와의 차이점은 객체가 회수되지 않았더라도 항상 null 반환

   => 객체가 사라지는 시점을 감지하기 위한 용도

* WeakReference는 메모리 누수 방지에 활용
  -> 객체를 참조하긴 하지만 객체의 수명을 강제로 늘리고 싶지 않을 때 사용
   ```java
   Map<User, String> map = new WeakHashMap<>();

   User user = new User("jimin");
   map.put(user, "temporary data");
   
   user = null;
   ```
  일반 HashMap이었다면 map이 User 객체를 계속 붙잡고 있어서 GC 대상이 안 될 수 있지만, WeakHashMap은 key를 weak reference로 잡기 때문에 외부에서 user를 더 이상 참조하지 않으면 GC 대상이 될 수 있음
   
   - 특정 객체에 대한 임시 메타데이터 저장 
   - 객체가 사라지면 관련 데이터도 같이 사라지게 하고 싶을 때 
   - 메모리 누수를 막고 싶을 때

### GC의 실행 시점은 언제이며, 특정 변수가 null이 되었을 때 바로 GC의 대상이 되나요?
> GC는 개발자가 정확한 실행 시점을 직접 제어하는 것이 아니라, JVM이 메모리 상황을 보고 필요하다고 판단할 때 실행됩니다. 대표적으로 Young 영역의 Eden 공간이 부족해지면 Minor GC가 발생할 수 있고, Old 영역이 부족하거나 객체 할당이 어려워지면 Major GC나 Full GC가 발생할 수 있습니다. System.gc()를 호출할 수도 있지만, 이것도 GC 실행을 요청하는 것일 뿐 반드시 즉시 실행된다는 보장은 없습니다.
> 
> 특정 변수가 null이 되었다고 해서 바로 GC가 실행되거나 객체가 즉시 메모리에서 제거되는 것은 아닙니다. 다만 그 변수가 참조하던 객체가 더 이상 어떤 GC Root로부터도 도달할 수 없는 상태가 되면 GC의 대상이 될 수 있습니다. 예를 들어 객체를 참조하던 유일한 변수가 null이 되면 해당 객체는 GC 대상이 될 수 있습니다. 하지만 다른 변수나 static 필드, 컬렉션 등이 여전히 그 객체를 참조하고 있다면 GC 대상이 아닙니다.
> 
> 따라서 핵심은 “변수가 null인가”가 아니라, 객체가 GC Root로부터 도달 가능한가입니다. null 처리는 객체를 GC 가능 상태로 만들 수는 있지만, 실제 회수 시점은 JVM이 결정합니다.
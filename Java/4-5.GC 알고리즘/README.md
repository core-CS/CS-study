## Parallel GC, G1GC 등 다양한 GC 알고리즘의 특징과 사용 환경에 대해 설명해주세요.
### Serial GC
![serial gc](https://blog.kakaocdn.net/dna/kBL3D/btrIT91k2n7/AAAAAAAAAAAAAAAAAAAAADwNDtyDakMmq6-t-n1hFybymhYbUvTbOfjw1iQHnFLE/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=%2B2T0dHGunWt%2B99v1A3WNUwE9lEE%3D)
서버의 CPU 코어가 1개일 때 사용하기 위해 개발된 가장 단순한 GC

GC를 처리하는 **스레드가 1개** (싱글 스레드)이기에 가장 **stop-the-world 시간이 긺**

Minor GC에는 Mark-Sweep / Major GC에는 Mark-Sweep-Compact 사용

디바이스 성능이 안좋아서 CPU 코어가 1개인 경우를 제외하고는 거의 사용하지 않음

```bash
java -XX:+UseSerialGC -jar Application.java
```

### Parallel GC
![parallel gc](https://blog.kakaocdn.net/dna/ckjP6E/btrIVf74R58/AAAAAAAAAAAAAAAAAAAAAFELkCPmwMYoHvlRRmkLa-haS5HzvOIahseju407-I55/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=keTxxz2aam47FdmHO4ZIel0ob%2BM%3D)
Java 8의 디폴트 GC

Serial GC와 기본적인 알고리즘은 같지만, Young 영역의 Minor GC를 멀티 스레드로 수행
(Old 영역은 여전히 싱글 스레드)

Serial GC에 비해 stop-the-world 시간 감소

```bash
java -XX:+UseParalleGC -jar Application.java

java -XX:+UseParallelGC -XX:ParallelGCThreads=4 -jar application.jar
```
-> GC 스레드는 기본적으로 cpu 개수만큼 할당되고 옵션을 통해 스레드 개수 설정 가능

### Parrallel Old GC (Parrallel Compacting Collector)
![parrallel old gc](https://blog.kakaocdn.net/dna/cs71MN/btrI0ePXZqr/AAAAAAAAAAAAAAAAAAAAAH15jhQBZkUvmII4GOHqOt2kOKf18qu8mypMe3VqUfyW/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=FTukt8R%2FXTD%2BfsL%2BDU9LWDBRHFM%3D)
Parrallel GC를 개선한 버전

Young 영역 뿐만 아니라, Old 영역에서도 멀티 스레드로 GC 수행

Old 영역에서는 새로운 가비지 컬렉션 청소 방식인 Mark-Summary-Compact 방식을 이용

* Mark-Summary-Compact
  - mark: old 영역을 region별로 나누고 region별 자주 참조되는 객체들을 식별
  - summary: region별 통계 정보로 살아남은 객체들의 밀도가 높은 부분이 어디까지인지 dense prefix 정함 -> 오랜 기각 참조된 객체는 앞으로 사용할 확률이 높다는 가정 하에 dense prefix를 기준으로 compact 영역을 줄임
  - compact: compact 영역을 destination과 source로 나누어, 살아남은 객체를 destination으로 이동시키고 참조되지 않는 객체 제거
  
```bash
java -XX:+UseParallelOldGC -jar Application.java
```

### CMS GC (Concurrent Mark Sweep)
![CMS GC](https://blog.kakaocdn.net/dna/btq9xn/btrIUalHp5a/AAAAAAAAAAAAAAAAAAAAAAh0sf86ZN68ZEnwuWpnKR0qcV53tVLqUWGGuqMBUzFB/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=tU49atpNkVKBncnihclZAMrEaBA%3D)
애플리케이션의 스레드와 GC 스레드가 동지에 실행되어 stop-the-world 시간을 최대한 줄이기 위해 고안된 GC

GC 과정이 매우 복잡

GC 대상 파악 과정이 복잡한 여러 단계로 수행 -> 다른 GC 대비 CPU 사용량이 높음

메모리 파편화 문제 발생! => old 영역에서 compact 단계 없음!

결국 사용 중지!~

* 동작 과정
  - Initial mark: GC의 root가 참조하는 객체만 마킹 (stop-the-world 발생)
  - Concurrent mark: 참조하는 객체를 따라가며 지속적으로 마킹 (stop-the-world 없이 발생, 마킹하는 스레드 외 다른 스레드도 작업 가능)
  - Remark: Concurrent mark 과정에서 변경된 사항이 없는지 다시 한 번 마킹 진행 (stop-the-world 발생)
  - Concurrent Sweep: Unreachable 객체 제거 (stop-the-world 없음)

```bash
java -XX:+UseConcMarkSweepGC -jar Application.java
```

### G1 GC (Garbage First)
![g1 gc](https://blog.kakaocdn.net/dna/cjO5N5/btrI1Ob7Gbb/AAAAAAAAAAAAAAAAAAAAAB5mgWjXJJYoLWCEHBE5DpYgUaOHfFdKRlyl3UAqHqp9/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=KlMG0snxkbp3JKAEsj82bpDuB0g%3D)
![g1 gc](https://blog.kakaocdn.net/dna/b3VAZL/btrIVglH89c/AAAAAAAAAAAAAAAAAAAAAGS0UJS2PrmF6oxOMI8hV-Pz1MZVWoYLUKw8LIdAW9LD/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=sJr%2FXKQkoS7B6h6plEfyGbSCuSk%3D)
![g1 gc](https://velog.velcdn.com/images%2Fguswlsapdlf%2Fpost%2F8122bf35-0e36-41fb-aa84-f8c5b85601b1%2Fimage.png)
CMS GC 대체를 위한 GC

Java 9+ 버전의 디폴트 GC로 지정

4GB 이상의 힙 메모리, stop-the-world 시간이 0.5초 정도 필요한 상황에 사용 (Heap이 너무 작을 경우 미사용 권장)

* G1 GC는 큰 힙을 여러 region으로 나눠 관리하고, 목표 pause time을 맞추도록 설계된 gC라서 큰 힙 + 짧은 STW 목표 있을 때 좋음!
  * 너무 작은 힙은 region을 나누고 추적하는 관리 비용이 오히려 오버헤드로 작용!
  * 

기존 GC 알고리즘에서는 Heap 영역을 물리적으로 고정된 Young/Old 영역을 나누어 사용했지만! G1 GC는 이런 개념을 뒤엎는 Region 개념 도입

  * 전체 Heap 영역을 Region이라는 영역으로 체스 같이 분할하여 상황에 따라 Eden, Survivor, Old 등 역할을 고정이 아닌 동적으로 부여

Garbage로 가득찬 영역을 빠르게 회수하여 빈 공간을 확보하므로, 결국 GC 빈도가 줄어드는 효과를 얻게 되는 원리

region 2개 추가
  * Humongous: region 크기의 50%를 초과하는 객체를 저장하는 region을 의미
  * Available/Unused: 사용되지 않는 region

- 동작 방식
  - Minor GC
    - region이 꽉 차면 다른 region에 객체 할당
    - 꽉찬 region에 대해 Minor GC 수행
    - eden region에서 GC 수행되면 살아남은 객체를 mark하고 메모리 회수
    - 살아남은 객체를 다른 region으로 이동 시킴
    - 이동된 region이 availagle/unused 지역이면 해당 지역은 Survivor region이 되고, Eden은 avaliable/unused 지역이 됨

  - Major GC
    ![g1 gc major gc](https://velog.velcdn.com/images/erinleeme/post/72e10b99-c984-4b0f-9fd2-6b099e019ab4/image.png)
    <details>
    <summary>동작 과정</summary>
     <ul>
      <li>Initial Mark: Old Region에 남아있는 객체들이 참조하고 있는 Survivor Region을 찾음 (Stop The World)</li>
      <li>Root Region Scan: Initial Mark 단계에서 찾은 Survivor Region에서 GC 작업 대상이 있는지 확인</li>
      <li>Concurrent Mark: Heap 영역에서 전체 Region을 스캔하며 GC 대상 객체가 발견되지 않은 Region은 다음 단계에서 제외</li>
      <li>Remark: 스캔이 끝나면 GC 대상에서 제외될 객체를 식별 (Stop the World)</li>
      <li>Clean up: 살아있는 객체가 가장 적은 Region 부터 사용되지 않은 객체를 제거 (Stop the World) -> 완전히 비어진 Region은 재사용 가능한 형태로 동작</li>
      <li>Copy: GC 대상이였지만 완전히 비워지지 않은 Region의 살아남은 객체들은 새로운 Region에 복사하여 Compaction 과정을 수행 (Stop The World)</li>
    </ul>
    </details>
    - 객체가 너무 많아 빠르게 sweep이 불가능할 때 Major GC 수행
    - 모든 heap 영역에서 수행되는 기존 GC와 다르게 어느 영역에 garbage 많은지 알고 있으므로 GC 수행 영역 조합하여 해당 region에서만 gC 수행
  
```bash
java -XX:+UseG1GC -jar Application.java
```

### Shenandoah GC
![Shenandoah gc](https://blog.kakaocdn.net/dna/lHh4s/btrISNkkpMV/AAAAAAAAAAAAAAAAAAAAAG9HkshwHahimRp0zXDomJtmRWsm2vgwDAY522Ohbshq/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=IiVdeZkhgcAGYV3PiZRjsqT%2B1%2BA%3D)
기존 CMS가 가진 단편화, G!이 가진 pause 이슈 해결

강력한 Concurrency와 가벼운 GC 로직으로 heap 사이즈에 영향을 받지 않고 일정한 pause 시간 소요

<details>
<summary>동작 과정</summary>
모든 객체는 헤더에 1word 크기의 포워딩 포인터를 위한 추가 공간 존재

읽기, 쓰기에 적용되는 배리어 존재 - 객체에 접근하려 할 때마다 배리어 코드 실행
-> 배리어가 객체 헤더를 확인해서 호워딩 포인터가 설정되어 있는지 확인하고, 포워딩 포인터가 있으면 이전 주소 참조를 새 주소로 업데이트 한 뒤 기존 작업 재개

- Init mark (STW 발생) - GC root 에서 직접 참조하는 객체 마킹
- Concurrent marking - 마킹된 객체부터 참조하는 모든 객체 마킹 - 동시에 압축할 region 선정 (살아남은 객체가 적은 것들 마킹)
- final mark (STW 발생) - 새로 생성되거나 참조가 변경된 객체들 최정 처리 - G1GC의 remark와 비슷한 역할
- Concurrent Evacuation - 압축 대상인 Region에 있는 살아있는 객체를 새로운 Region으로 복사 - 객체가 새 위치로 복사되면 이전 위치 객체의 헤더에 ㅇ새 주소를 가리키는 포워딩 포인터 설정, 그래서 옛 주소로 접근하면 배리어가 이를 감지해서 참조를 새 주소로 고쳐준 후 작업 재개
- Concurrent Update References - 객체 이동이 완료되면 힙 전체를 스캔해서 이전 주소를 가리키는 참조를 모두 새 주소로 업데이트
</details>

객체 이동 단계에서 STW 발생 X

처리량 감소 - 배리어가 참조를 읽을 때 마다 실행되기 때문에 CPU 오버헤드가 지속적으로 발생

CPU, 메모리 사용량 증가 - GC 과정에서 계속 백그라운 스레드 사용 및 힙 모든 객체가 1 word 추가 공간을 헤더로 가지고 있어야 하기에 메모리 오버헤드 증가
```bash
java -XX:+UseShenandoahGC -jar Application.java
```

### ZGC (Z Garbage Collector)
![ZGC](https://blog.kakaocdn.net/dna/IKx8x/btrIT9f9qjM/AAAAAAAAAAAAAAAAAAAAAEG8VOUj6fuL15E2X0wEfKEyb_RnwxhAZp887sNSCUMb/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1780239599&allow_ip=&allow_referer=&signature=b6eYMOUdMMKm8GyJ11xe4XvEiw4%3D)
대량의 메모리를 low-latency로 잘 처리하기 위해 디자인 된 GC

G1의 Region처럼, ZGC는 Z Page라는 영역 사용

G1의 Region은 크기가 고정이지만 Z Page는 2MB 배수로 동적으로 운영 (큰 객체가 들어오면 2의 지수승으로 영역을 구성해서 처리)

힙 크기가 증가하더라도 STW 시간이 10ms를 넘지 않음

<details>
<summary>동작 과정</summary>
- mark (STW) - 힙 메모리 내 객체 중 살아있는 객체 식별 및 표시
    
    - init mark: GC root 직접 참조 객체 마킹 (STW)
    - concurrent mark: 어플리케이션 스레드와 동시에 힙 전체를 스캔하며 live 객체를 마킹
    - remark: 동시 마킹 중 변경된 객체 재마킹 (STW)
- Relocation
  - Concurrent Relocation - live 객체를 new 메모리 영역으로 이동
- Remapping - 이동한 객체의 참조를 새 주소로 업데이트 
- reclaim - 더 이상 사용하지 않는 메모리 블록 회수, 새로운 객체가 할당될 수 있게 함
</details>

```bash
java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -jar Application.java
```

### Parallel GC, G1GC 등 다양한 GC 알고리즘의 특징과 사용 환경에 대해 설명해주세요.
> Java의 GC 알고리즘은 처리량을 우선할지, 지연 시간을 줄일지에 따라 선택합니다.
> 
> Serial GC는 GC 스레드가 하나라 구조는 단순하지만 Stop-the-World 시간이 길어, 단일 코어이거나 작은 애플리케이션에 적합합니다.
> 
> Parallel GC는 여러 GC 스레드로 Young 영역, 또는 Parallel Old GC의 경우 Old 영역까지 병렬 처리하기 때문에 처리량이 좋습니다. 그래서 응답 시간보다 전체 처리량이 중요한 배치 작업이나 백엔드 서버에 적합합니다.
> 
> CMS GC는 애플리케이션 스레드와 GC 스레드가 동시에 동작해 pause time을 줄이려 했지만, 메모리 파편화와 CPU 오버헤드 문제가 있어 현재는 거의 사용되지 않습니다.
>
> G1 GC는 Heap을 여러 Region으로 나누고, Garbage가 많은 Region을 우선 회수합니다. 목표 pause time을 설정할 수 있어 큰 Heap과 짧은 STW가 필요한 서버 환경에 적합하고, Java 9 이후 기본 GC입니다.
>
> 최근에는 ZGC, Shenandoah GC처럼 객체 이동까지 대부분 concurrent하게 처리해 매우 짧은 pause time을 목표로 하는 GC도 사용됩니다.
>
> 정리하면, 처리량 중심이면 Parallel GC, 큰 Heap과 안정적인 pause time이 필요하면 G1 GC, 초저지연이 중요하면 ZGC나 Shenandoah를 고려합니다.
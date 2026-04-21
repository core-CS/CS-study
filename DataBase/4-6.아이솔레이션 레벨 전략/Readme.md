<h2>MySQL, Oracle의 Default 아이솔레이션 레벨 전략이 무엇인지 설명해 주세요</h2>

> MySQL의 기본 격리 수준은 Repeatable Read이고, Oracle의 기본 격리 수준은 Read Committed이다. 
>
> 두 DBMS 모두 MVCC를 사용하지만 구현 방식과 기본 전략이 다르다.
> 
> MySQL이 Repeatable Read를 기본으로 채택한 이유는 과거 Statement 기반 복제(Replication)의 일관성을 보장하기 위해서였다. Read Committed에서는 마스터와 슬레이브 간 실행 순서에 따라 데이터가 달라질 수 있었기 때문에, 더 엄격한 격리 수준을 기본값으로 설정한 것이다. 또한 InnoDB는 Next-Key Lock(레코드 락 + 갭 락)을 통해 Repeatable Read에서도 Phantom Read를 상당 부분 방지한다.
>
> 반면 Oracle은 Undo 세그먼트 기반 MVCC로 읽기 일관성을 제공하며, 읽기 작업이 쓰기를 막지 않는 구조이기 때문에 Read Committed만으로도 충분한 성능과 일관성을 확보할 수 있다고 판단해 이를 기본값으로 사용한다.




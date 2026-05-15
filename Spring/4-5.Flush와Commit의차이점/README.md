## Flush 와 Commit 의 차이는 무엇인가요?

***

> Flush 는 JPA 가 SQL 에 DB를 보내는 행위이며 Commit 은 DB가 변경을 영구 확정하는 행위입니다.
즉, Flush 는 아직 트랜잭션 처리 중이며, Commit 은 트랜잭션이 종료됩니다.
> 

### Flush 수행 시 DB 에서 일어나는 일

---

```
JPA/Hibernate(Dirty checking -> 쓰기 지연 SQL 저장소 -> SQL 생성)
⬇️
JDBC PreparedStatement (SQL 문자열을 네트워크로 전송)
⬇️
DB Buffer Pool (메모리, 페이지 변경)
⬇️
Undo Log (롤백 대비용, 변경 전 데이터 보관을 함)
```

즉, Commit 전 Flush 이후에는 DB 에 SQL 이 도달하여 Buffer Pool 이 변경됩니다.

- 같은 트랜잭션 내에서는 변경점이 보이나 다른 트랜잭션에서는 확인이 불가합니다.

### Commit 수행 시 일어나는 일

---

```
Redo Log 기록 (변경 내역을 순차 디스크에 기록한다.)
⬇️
Row Lock 해제 (다른 트랜잭션이 row 접근 가능)
⬇️
MVCC 버전 확정 (다른 세션에서 변경 결과 확인 가능)
⬇️
Undo Log 정리 (롤백이 불필요하며 공간을 회수합니다.)
```

- 모든 트랜잭션에서 변경이 보이게 됩니다.

### 안티 패턴

---

🔴 Flush 와 외부 API 호출 순서를 잘못함.

```java
@Transactional
public void antiPattern(OrderDto dto) {
	Order order = new Order(dto);
	em.persist(order);
	em.flush();
	
	paymentApi.charge(order.getId()); // exception
}
```

- DB 에 order 데이터를 저장하기 전이지만 외부 api 를 호출했기에 예외가 발생합니다. (없는데이터)
- 그렇지만 롤백을 하면 DB 의 order 이 사라지지만, 결제는 이미 완료 되어서 결제만 되고 주문이 없는 상황이 발생합니다.

🟢 대신에 commit 이 끝난 후 외부 API 를 호출합시다.

```java
@Transactional
public void saveOrder(Order o) {
...}

public void processOrder(OrderDto o) {
	Order saved = saveOrder(o);
	
	paymentApi.charge(saved.getId());	
}
```

commit 완료 후 결제를 진행하며 만약 결제 실패 시 별도 보상 트랜잭션을 처리할 수 있게 됩니다. (사가 패턴)

- IO 작업이나 외부 API 호출 시에는 flush 와 commit 사이에 넣지 않아야 합니다.

### 그 외 꼬리 질문?

---

- **그럼 flush 후 rollback하면 Undo Log는 어떻게 처리되나요?**
    
    flush로 Buffer Pool에 반영된 Dirty Page는 Undo Log의 이전 이미지로 즉시 복구됩니다. InnoDB 기준으로 rollback 시 Undo Log에 기록된 before-image를 읽어 Buffer Pool 페이지를 되돌리고, 해당 Undo Log 세그먼트는 Purge Thread가 이후 회수합니다. Dirty Page가 checkpoint를 넘어 이미 디스크에 쓰였어도, Undo Log가 있으면 복구 가능합니다.
    
- **@Transactional(readOnly = true)일 때 flush가 발생하지 않는다고 했는데, commit은 발생하나요?**
    
    commit은 발생하지만 실질적으로 아무 의미가 없습니다. flush가 일어나지 않으니 DB에 전달된 변경 SQL이 없고, commit은 빈 트랜잭션을 종료할 뿐입니다. 더 중요한 이점은 Hibernate가 `readOnly = true` 시 스냅샷 자체를 저장하지 않아 Dirty Checking 대상 엔티티가 없으므로, 다수 엔티티 조회 시 메모리와 CPU를 절약할 수 있다는 점입니다. MySQL Replication 환경에서는 `readOnly = true` 트랜잭션을 Replica로 라우팅하는 용도로도 활용합니다.
    
- **분산 트랜잭션(2PC)에서 flush와 commit의 관계는 어떻게 달라지나요?**
    
    2PC에서 1단계(Prepare)는 flush와 유사하게 각 참여자 DB에 변경을 기록하고 "커밋 가능"을 확인하는 단계입니다. 2단계(Commit)에서 코디네이터가 모든 참여자에 commit 명령을 내려야 비로소 확정됩니다. 단일 DB의 flush-commit 구간이 분산 환경에서는 Prepare-Commit 구간으로 확장되고, 이 구간에 코디네이터 장애가 발생하면 In-Doubt 트랜잭션으로 남아 수동 복구가 필요해집니다. 이 복잡성 때문에 현대 MSA에서는 2PC 대신 Saga 패턴을 선호합니다.
# 관리자 명령어

**언어:** [日本語](../commands.md) | [English](../en/commands.md) | [中文](../zh/commands.md) | 한국어

모든 명령어는 `/kaisatsu`로 시작합니다. OP 권한이 필요합니다.

---

## 역 관리

```
/kaisatsu station list
```
등록된 모든 역의 목록을 표시합니다.

```
/kaisatsu station delete <역 이름>
```
지정한 역을 삭제합니다. 모든 노선 및 개찰기 설정에서 자동으로 제거됩니다.

```
/kaisatsu station rename <이전 이름> <새 이름>
```
역 이름을 변경합니다. 노선과 개찰기의 참조도 자동으로 업데이트됩니다.

---

## 노선 관리

```
/kaisatsu line list
```
등록된 모든 노선의 목록을 표시합니다.

```
/kaisatsu line delete <노선 ID>
```
지정한 노선을 삭제합니다.

---

## IC 카드 및 잔액 조작

```
/kaisatsu ic balance <플레이어 이름>
```
지정한 플레이어가 소지한 IC 카드의 잔액을 확인합니다.

```
/kaisatsu ic charge <플레이어 이름> <금액>
```
지정한 플레이어의 IC 카드에 잔액을 추가합니다 (관리자용).

```
/kaisatsu ic reset <플레이어 이름>
```
지정한 플레이어의 IC 카드 입장 상태를 초기화합니다. 입장 기록이 남아 출장할 수 없는 경우에 사용합니다.

---

## 데이터 관리

```
/kaisatsu reload
```
월드 데이터(역, 노선, 매출)를 디스크에서 다시 불러옵니다.

```
/kaisatsu export
```
현재 데이터를 JSON 형식으로 서버 로그에 출력합니다 (백업용).

---

## 디버그

```
/kaisatsu debug fare <출발역> <목적지역>
```
지정 구간의 요금 계산 결과와 경로를 표시합니다. 요금이 이상한 경우 조사에 사용합니다.

```
/kaisatsu debug network
```
등록된 역, 노선, 연결 상태의 네트워크 그래프를 채팅에 표시합니다.

---

## 권한

명령어 실행에는 OP 레벨 2 이상이 필요합니다. 특정 명령어를 스태프에게 개방하려면 권한 플러그인 (예: LuckPerms)을 통해 다음 노드를 부여하세요:

| 노드 | 내용 |
|---|---|
| `rtmkaisatsu.station.list` | 역 목록 열람 |
| `rtmkaisatsu.ic.balance` | IC 카드 잔액 확인 |
| `rtmkaisatsu.ic.reset` | 입장 상태 초기화 |

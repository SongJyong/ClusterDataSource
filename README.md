# ClusterDataSource
***
## command line 명령어
- add n : n 개의 Component connection pool 추가 (physical connection dummy는 항상 10개로 고정)


- inactive n : address값이 n인 Component를 비활성화 (connection 제공 멈추게 함)


- active n : address값이 n인 Component를 활성화 (기존에 inactive 였던 Component만 변화 있음.)


- remove n : address값이 n인 Component를 제거 (데이터는 archive로 보내고, list의 address n 값 안에는 null 이 들어감), 현재는 다시 살리는 방법 구현 x


- primary n : address값이 n인 Component를 우선순위화 ON/OFF (호출마다 옵션 켰다가 껐다가)


- failmark n : address값이 n인 Component의 동작을 랜덤 시간동안 (0~10초) 고장나게 만듬. (getConnect 실패하게 만듬) 즉, marking을 하고 난 후 따로 request가 들어가지 않는 이상 확인되진 않음.


- wait n : 명령어를 받는 스레드를 n 초만큼 기다리게 함. (명령어 여러 개 사이에 시간 텀을 두고 싶을 때 사용)


- get : 현재 Component Pool들의 상태를 전반적으로 콘솔에 출력

    - ex.) p*i*f*[0] Component Id : 0, Count : 0

    - p*: 이 Component는 primary 상태

    - i*: 이 Component는 inactive 상태

    - f*: 이 Component는 failed 상태

    - [0] : 현재 Component 리스트 address 0 에 위치하고 있음. (Component Id랑 다른 개념)

    - 추가적으로 맨 밑에 remove 된 옛날 Component들을 따로 출력해줌. (옛날 기록도 포함해서 마지막 줄은 서버에서 connection 제공한 총 응답 횟수)
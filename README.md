# ClusterDataSource

## spec

- 기본적으로 Client에서 보낸 요청 수와 Server에서 보낸 요청 수 가 같은지 확인을 원함 = base

- client server는 소켓 통신 (asyncSocketChannel말고 Selector를 쓸것 + jdk nio)


## Client

- getConnection 요청을 보냄


## Server

- 크게 2가지 레이어가 존재함. service , db

## service

- Client단에서 소켓 통신에서 넘어오는 요청을 받아들이고 유실되지 않게 잘 보관

- Server 단에서 넘어오는 요청을 처리

- ClusterConnectionPool 호출 하여 요청을 전달

## db

- db는 1개만 설치함

- ClusterConnectionPool 이 존재하고 내부적으로 여러 개의 Component Connection Pool이 존재하여 service의 요청을 정책에 맞게 위임한다.

- ConnectionPoolDataSource를 사용해서 db로부터 physicalConnection을 얻어오고 이를 pool 안에 저장한다.

    -- 예를들면 pool 을 4개 만들고 각각 5개씩 저장하여 총 4개의 Component Connection Pool을 생성한다.

- service 에서 받아온 요청을 처리하는 과정

    -- ClusteredConnectionPool 에서 정책(RoundRobin)에 맞는 Component ConnectionPool 고르고 physicalConnection을 받아오고 logicalConnection을 꺼냄

    -- 해당 pool의 logicalConnection을 썼다는것을 통계를 담당하는 클래스에 기록하고 요청 종료

    -- count를 증가하는 방식으로 잘 동기화가 되게

***

## 명령어

## server

- start n

n개의 component connection pool 생성 및 초기화

- add n

n개의 component connection pool  추가로 생성 및 초기화

client요청이 추가된 pool에도 잘 전달이 되어 야함

- remove n

n개의 component connection pool 제거 및 통계 동기화

처리하고 있는 요청이 있으면 대기하고 추가적인 요청을 더 받지 않도록 함

- get data

총 요청 수 출력

각 풀이 몇 개의 요청을 처리했는지 나오게

## client

- start n m

n개의 스레드를 만들어서 요청을 m개씩 보냄

- getdata

총 요청 수 출력

***

## 추가적인 고민사항

정책적인 측면

- failover

명령어를 통해 특정 pool 이 failed marked 되었다 콘솔에 출력

failed marking 된것은 랜덤한 시간동안 제대로 connection을 주지않게함

정책 변경 명령어로 failover 정책으로 변경

failed marking 된 pool 을 별도의 스레드에서 지속적으로 확인하고 제대로 된 connection을 주는 순간 failed marking을 해제하고 로그를 남김

- failback

component connection pool의 우선순위를 부여하여 failover가 될때마다 우선순위 순으로 나열하게 동작하게함

- Affinity

client의 요청을 구분할 수 있도록 프로토콜을 정의하고 (일종의 단순한 인가를 정의)

특정 client가 특정 component connection pool 을 계속 사용할 수 있도록하게함

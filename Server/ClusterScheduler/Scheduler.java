package Server.ClusterScheduler;


public interface Scheduler {
    // 정책과 다른 스케쥴러 메서드 활용 시, error message
    default int getScheduleAddress(){
        System.out.println("wrong method use : you should look scheduler policy");
        return -1;
    }
    default int getScheduleAddress(Integer clientId, int requestId, RoundRobinScheduler rrScheduler){
        System.out.println("wrong method use : you should look scheduler policy");
        return -1;
    }
}

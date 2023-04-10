package Server.ClusterScheduler;


public interface Scheduler {
    default int getScheduleIndex(){
        System.out.println("wrong method use : you should look scheduler policy");
        return -1;
    }
    default int getScheduleIndex(int clientId, RoundRobinScheduler rrScheduler){
        System.out.println("wrong method use : you should look scheduler policy");
        return -1;
    }
}

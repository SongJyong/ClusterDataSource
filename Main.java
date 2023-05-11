import Server.SingletonServer;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        SingletonServer singletonServer = new SingletonServer();
        singletonServer.startServer();
        singletonServer.start(10);

        System.out.println("'add n' -- add new n component pools");
        System.out.println("'inactive n' -- inactivate the component (that address is n)");
        System.out.println("'activate n' -- activate the component (that address is n)");
        System.out.println("'remove n' -- remove the component (that address is n)");
        System.out.println("'primary n' -- priority ON/Off (for component that address is n)");
        System.out.println("'failmark n' -- make the component(that address is n) work incorrect during random times(0~10)");
        System.out.println("'get' -- get response count with component pool status");
        System.out.println("'wait n' -- cli waits n seconds");

        Scanner scanner = new Scanner(System.in);
        Thread t = new Thread() {
            @Override
            public void run() {
                while (true) {
                    String s = scanner.nextLine();
                    if (!s.isEmpty()) {
                        if (s.equals("get")) {
                            System.out.printf("server total : %d \n", singletonServer.getData());
                            continue;
                        }
                        String[] spl = s.split(" ");
                        try {
                            int n = Integer.parseInt(spl[1]);
                            if (spl[0].equals("add")) {
                                singletonServer.add(n);
                            } else if (spl[0].equals("inactive")) {
                                singletonServer.inactive(n);
                            } else if (spl[0].equals("active")) {
                                singletonServer.active(n);
                            } else if (spl[0].equals("remove")) {
                                singletonServer.remove(n);
                            } else if (spl[0].equals("wait")) {
                                Thread.sleep(1000*n);
                            } else if (spl[0].equals("failmark")) {
                                singletonServer.setFailedMark(n);
                            } else if (spl[0].equals("primary")) {
                                singletonServer.updatePrimary(n);
                            }
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            System.out.println("Wrong Input Error");
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            System.out.println("Input thread be interrupted");
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        t.start();

    }
}
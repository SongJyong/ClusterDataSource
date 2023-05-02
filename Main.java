import Server.SingletonServer;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        SingletonServer singletonServer = new SingletonServer();
        singletonServer.startServer();
        singletonServer.start(10);

        Scanner scanner = new Scanner(System.in);
        Thread t = new Thread() {
            @Override
            public void run() {
                while (true) {
                    String s = scanner.nextLine();
                    if (!s.isEmpty()) {
                        if (s.equals("g")) {
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
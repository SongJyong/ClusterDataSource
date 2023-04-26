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
                        if (s.equals("a")){
                            try {
                                singletonServer.remove(5);
                                singletonServer.add(5);
                            } catch (InterruptedException e) {
                                System.out.println("Input thread be interrupted");
                                throw new RuntimeException(e);
                            }
                        }
                        else if (s.equals("g")) {
                            System.out.printf("server total : %d \n", singletonServer.getData());
                        }
                        else if (s.equals("f")) {
                            try {
                                singletonServer.setFailedMark();
                            } catch (InterruptedException e) {
                                System.out.println("Input thread be interrupted");
                                throw new RuntimeException(e);
                            }
                        }
                        String[] spl = s.split(" ");
                        try {
                            if (spl[0].equals("add")) {
                                int n = Integer.parseInt(spl[1]);
                                singletonServer.add(n);
                            } else if (spl[0].equals("r")) {
                                int n = Integer.parseInt(spl[1]);
                                singletonServer.remove(n);
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
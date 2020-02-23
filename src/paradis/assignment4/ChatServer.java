//Peter Fröberg, pefr7147

package paradis.assignment4;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatServer implements Runnable {
    private final static int PORT = 8000;
    private final static int MAX_CLIENTS = 5;
    private final static Executor executor = Executors.newFixedThreadPool(MAX_CLIENTS);
    private static CopyOnWriteArrayList<LinkedBlockingQueue> clientMessageQueues;

    private static Object lock = new Object();
    private final Socket clientSocket;
    private String clientName;
    private LinkedBlockingQueue clientMessageQueue;

    private ChatServer(Socket clientSocket, LinkedBlockingQueue clientMessageQueue, CopyOnWriteArrayList clientMessageQueues) {

        this.clientSocket = clientSocket;
        this.clientMessageQueue = clientMessageQueue;
        this.clientMessageQueues = clientMessageQueues;
    }

    public void run() {
        SocketAddress remoteSocketAddress = clientSocket.getRemoteSocketAddress();
        SocketAddress localSocketAddress = clientSocket.getLocalSocketAddress();
        System.out.println("Accepted client " + remoteSocketAddress
                + " (" + localSocketAddress + ").");

        PrintWriter socketWriter = null;
        BufferedReader socketReader = null;
        try {
            socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            PrintWriter finalSocketWriter = socketWriter;
            new Thread(() -> {
                String mess;
                while (true) {
                    try {
                        mess = (String) clientMessageQueue.take();
                        System.out.println("skickar till client in thread "+ Thread.currentThread().getName() + " : " + mess);
                        finalSocketWriter.println(mess);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


            socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String threadInfo = " (" + Thread.currentThread().getName() + ").";
            String inputLine = socketReader.readLine();
            System.out.println("Received: \"" + inputLine + "\" from "
                    + remoteSocketAddress + threadInfo);

            // First message is client name.
            clientName = inputLine;

            while (inputLine != null) {
                synchronized (lock) {
                    for (LinkedBlockingQueue que : clientMessageQueues) {
                        que.put(clientName + ": " + inputLine); // .add(mess);
                    }
                }
                inputLine = socketReader.readLine();
                System.out.println("Received: \"" + inputLine + "\" from "
                        + clientName + " " + remoteSocketAddress + threadInfo);
            }
            System.out.println("Closing connection " + remoteSocketAddress
                    + " (" + localSocketAddress + ").");
        } catch (Exception exception) {
            System.out.println(exception);
        } finally {
            try {
                if (socketWriter != null)
                    socketWriter.close();
                if (socketReader != null)
                    socketReader.close();
                if (clientSocket != null)
                    clientSocket.close();
            } catch (Exception exception) {
                System.out.println(exception);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Server started.");

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            SocketAddress serverSocketAddress = serverSocket.getLocalSocketAddress();
            System.out.println("Listening (" + serverSocketAddress + ").");
            CopyOnWriteArrayList<LinkedBlockingQueue> clientMessageQueues = new CopyOnWriteArrayList<>();
            LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

            while (true) {
                clientSocket = serverSocket.accept();
                LinkedBlockingQueue<String> clientMessageQueue = new LinkedBlockingQueue<>();
                clientMessageQueues.add(clientMessageQueue);
                executor.execute(new ChatServer(clientSocket, clientMessageQueue, clientMessageQueues));
            }
        } catch (Exception exception) {
            System.out.println(exception);
        } finally {
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (Exception exception) {
                System.out.println(exception);
            }
        }
    }
}

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

class ServerThread extends Thread {
    private static final String EXIT = "/exit";

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ServerThread(Socket socket1) throws IOException {
        socket = socket1;

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));

        start();
    }

    // отправка сообщения в чат (всем кроме самого себя)
    public void sendMsg(String msg) {
        for (ServerThread user : Server.users)
            if (user != this) user.takeMsg(msg);
    }

    // Получить сообщение во входной поток
    private void takeMsg(String msg) {
        out.println(msg);
        out.flush();
    }

    @Override
    public void run() {
        try {

            String s = in.readLine(); // Пришел первоход!!!
            sendMsg(s);
            while (true) {

                s = in.readLine();
                if (s.equals(EXIT)) {

                    String username = in.readLine(); // получаем username
                    sendMsg("# " + username + " was disconnected...");

                    System.out.println("# " + username + " was disconnected...");

                    downProcess(); // сворачиваемся
                    break;
                }

                sendMsg(s);
                System.out.println(s);
            }
        } catch (IOException e) {
            // пофиксить блок try-catch
            downProcess();
            System.err.println("Аварийное завершение!");
        }

    }


    private void downProcess() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Server.users.remove(this);
    }
}

public class Server {
    public static final int PORT = 1488;

    public static LinkedList<ServerThread> users = new LinkedList<>();

    public static void main(String[] args) throws IOException {

        ServerSocket sSocket = new ServerSocket(PORT);

        System.out.println("Server Started");

        try {
            while (true) {
                Socket socket = sSocket.accept();
                try {
                    users.addLast(new ServerThread(socket));
                } catch (IOException er) {
                    socket.close();
                    System.err.println("Ошибка подключения");
                }
            }
        } finally {
            sSocket.close();
        }
    }
}

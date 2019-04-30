import java.io.*;
import java.net.Socket;

public class Client {
    private static final String EXIT = "/exit";
    private static final String resetColor = "\u001B[0m";
    private static final String nameColor = "\u001B[92m";
    private static final String serverColor = "\u001B[94m";

    private String username = "";
    private BufferedReader in, reader;
    private PrintWriter out;
    private Socket socket;

    public Client(String ip, int port) throws IOException {
        socket = new Socket(ip, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
        reader = new BufferedReader(new InputStreamReader(System.in));

        login();    // получаем username

        // подрубаем непрерывный обмен данными
        new Reader().start();
        new Writer().start();
    }

    // ввод username Клиента, и сообщение в ЧАТ о его добавлении
    private void login() {
        System.out.print("Введите никнейм длинной 3 - 12 символов: ");
        try {
            username = reader.readLine();
            int N = username.length();
            while (!(N >= 3 && N <= 12)) {
                System.err.println("Никнейм не допустим!");

                System.out.print("Введите никнейм длинной 3 - 12 символов: ");
                username = reader.readLine();

                N = username.length();
            }

            out.println("# " + username + " joined the chat...");
            out.flush();

            System.out.println(coloring("# Добро пожаловать, " + username));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Закрываем сокет и соответствующие ему потоки
    private void disconnect() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Нить, передающая сообщения клиента в серверный сокет
    // Пока клиент не отправит EXIT команду
    public class Writer extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String tmp = reader.readLine();
                    tmp = !tmp.equals(EXIT) ? username + ":\t" + tmp : tmp;

                    out.println(tmp);
                    out.flush();

                    if (tmp.equals(EXIT)) {     // Перед завершением работы процесса
                        out.println(username);  // передаем username серверной части,
                        out.flush();            // для вывода инфы об отключени

                        disconnect();   // Закрывает сокет и соответствующие ему потока,
                        // что остановит Reader
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Нить, читающая сообщения от сервера
    public class Reader extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    String s = in.readLine();
                    System.out.println(coloring(s));
                } catch (IOException exit) {    //  Попытка считывания из закрытого потока.
                    break;
                }
            }
        }

    }

    //  Возвращает цвет строки
    //  1)  Синий, если строка начинается с символа '#'
    //  2)  Зеленый, если строка - сообщение от какого - либо пользователя
    public static String coloring(String s) {
        if (s.indexOf('#') == 0)
            return serverColor + s + resetColor;

        String[] tmp = s.split(":\t", 2);
        return nameColor + tmp[0] + ":\t" + tmp[1] + resetColor;
    }

    public static void main(String[] args) {

        try {
            new Client("localhost", 1488);
        } catch (IOException e) {
            System.err.println("Ошибка клиента ");
        }
    }

}
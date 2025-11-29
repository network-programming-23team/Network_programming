import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CatchMindServer extends JFrame {

    private JTextField portField;
    private JButton startButton;
    private JTextArea logArea;

    private ServerSocket serverSocket;
    private boolean running = false;

    private List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CatchMindServer().setVisible(true));
    }

    public CatchMindServer() {
        setTitle("CatchMind Server");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // UI 구성
        portField = new JTextField("12345", 10);
        startButton = new JButton("서버 시작");
        logArea = new JTextArea();
        logArea.setEditable(false);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("포트 번호:"));
        topPanel.add(portField);
        topPanel.add(startButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        startButton.addActionListener(e -> startServer());
    }

    private void startServer() {
        if (running) return;

        try {
            int port = Integer.parseInt(portField.getText().trim());
            serverSocket = new ServerSocket(port);
            running = true;
            log("서버 시작됨! 포트: " + port);

            // 클라이언트 수신 스레드
            new Thread(() -> {
                try {
                    while (running) {
                        Socket socket = serverSocket.accept();
                        log("클라이언트 연결됨: " + socket.getInetAddress());
                        ClientHandler handler = new ClientHandler(socket);
                        clients.add(handler);
                        handler.start();
                    }
                } catch (IOException ex) {
                    log("서버 종료됨.");
                }
            }).start();

            startButton.setEnabled(false);
            portField.setEnabled(false);

        } catch (Exception ex) {
            log("포트 열기 실패: " + ex.getMessage());
        }
    }

    private void broadcast(String msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
    }

    // 클라이언트 처리 스레드
    class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String msg;
                while ((msg = in.readLine()) != null) {
                    log("받음: " + msg);
                    broadcast(msg);
                }

            } catch (IOException e) {
                log("클라이언트 연결 종료");
            } finally {
                try { socket.close(); } catch (IOException ignore) {}
                clients.remove(this);
            }
        }

        public void send(String msg) {
            out.println(msg);
        }
    }
}

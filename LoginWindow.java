import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class LoginWindow extends JFrame {
    
    // [수정] 버튼 색상을 이미지와 어울리는 파스텔 톤으로 변경
    private final Color BUTTON_COLOR = new Color(119, 181, 254); // 파스텔 블루
    private final Color TEXT_COLOR = new Color(50, 50, 50); // 어두운 회색 (가독성)
    
    private JTextField nameField;
    private JButton loginButton;
    
    public LoginWindow() {
        setTitle("Crayon - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700); // 로그인 창 크기
        setLocationRelativeTo(null); 
        
        //setUndecorated(true); 
        
        ImagePanel mainPanel = new ImagePanel("/images/loginBack.png");
        
        // ▼▼▼ [수정 1] : 레이아웃을 null(절대 위치)로 변경 ▼▼▼
        mainPanel.setLayout(null); 
        
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        
        // 2. 입력 패널 (기존과 동일하게 생성)
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false); // 투명하게
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel nameLabel = new JLabel("아이디:");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        nameLabel.setForeground(TEXT_COLOR);
        
        nameField = new JTextField(15);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        nameField.setBackground(new Color(255, 255, 255, 200)); 
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        inputPanel.add(nameField, gbc);
// 3. 로그인 버튼 (이미지로 변경)
        
        // [삭제] JPanel buttonPanel = new JPanel(); 
        // [삭제] buttonPanel.setOpaque(false); 

        // --- ▼▼▼ [수정 1] : 2개의 이미지를 불러오고 "리사이징"하는 로직 ▼▼▼ ---
        
        ImageIcon normalIcon = null; // [수정] 일반 상태 아이콘
        ImageIcon hoverIcon = null;  // [추가] 마우스 오버 상태 아이콘
        
        int buttonWidth = 200;  
        int buttonHeight = 70; 

        int desiredWidth = 100;
        int desiredHeight = 80;

        try {
            // [!!] 마우스 오버용 이미지를 "start_btn_hover.png"라고 가정합니다.
            // [!!] 이 파일이 "src/images/" 폴더에 꼭 있어야 합니다!
            
            // 1. 원본 (일반) 이미지 불러오기
            ImageIcon originalNormalIcon = new ImageIcon(getClass().getResource("/images/start_btn1.png"));
            // 2. 원본 (마우스 오버) 이미지 불러오기
            ImageIcon originalHoverIcon = new ImageIcon(getClass().getResource("/images/start_btn2.png"));
            
            
            if (originalNormalIcon.getIconWidth() > 0 && originalHoverIcon.getIconWidth() > 0) {
                // 3. 일반 이미지 리사이징
                Image originalNormalImage = originalNormalIcon.getImage();
                Image resizedNormalImage = originalNormalImage.getScaledInstance(desiredWidth, desiredHeight, Image.SCALE_SMOOTH);
                normalIcon = new ImageIcon(resizedNormalImage);

                // 4. [추가] 마우스 오버 이미지 리사이징
                Image originalHoverImage = originalHoverIcon.getImage();
                Image resizedHoverImage = originalHoverImage.getScaledInstance(desiredWidth, desiredHeight, Image.SCALE_SMOOTH);
                hoverIcon = new ImageIcon(resizedHoverImage);

                // 5. setBounds에서 사용할 크기를 "원하는 크기"로 업데이트
                buttonWidth = desiredWidth;
                buttonHeight = desiredHeight;
                
            } else {
                throw new Exception("Image not found or empty. Check both start_btn.png and start_btn_hover.png");
            }
        } catch (Exception e) {
            System.err.println("Error loading/resizing button images.");
            e.printStackTrace();
        }

        // --- ▲▲▲ [수정 완료] ▲▲▲ ---
        
        
        // [수정] 리사이즈된 아이콘(resizedIcon)으로 버튼 생성
        if (normalIcon != null && hoverIcon != null) { // [수정] 두 아이콘이 모두 로드되었는지 확인
            loginButton = new JButton(normalIcon); // 기본 아이콘은 normalIcon
            
            // ▼▼▼ [핵심] 마우스가 올라갔을 때 보일 아이콘 설정 ▼▼▼
            loginButton.setRolloverIcon(hoverIcon);
            // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
            
            // 버튼을 투명하게 만들어 이미지만 보이도록 설정
            loginButton.setBorderPainted(false);
            loginButton.setContentAreaFilled(false);
            loginButton.setFocusPainted(false);
            loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
        } else {
            // [비상시]
            System.out.println("Button image load failed. Using fallback text button.");
            loginButton = createStyledButton("JOIN GAME", BUTTON_COLOR);
            loginButton.setForeground(Color.WHITE);
            buttonWidth = 200; 
            buttonHeight = 50; 
        }
        // [삭제] buttonPanel.add(loginButton);
        
        
        // ▼▼▼▼▼ [수정 2] : setBounds(x, y, width, height)로 위치 직접 지정 ▼▼▼▼▼
        
        // inputPanel의 크기를 먼저 계산합니다.
        Dimension inputSize = inputPanel.getPreferredSize();
        
        // [삭제] Dimension buttonSize = new Dimension(buttonWidth, buttonHeight);
        // [삭제] buttonPanel.setSize(buttonSize); 

        // 닉네임창 위치 (x, y, width, height)
        int inputX = (1100 - inputSize.width) / 2;
        int inputY = 400; 
        inputPanel.setBounds(inputX - 25, inputY, inputSize.width, inputSize.height);
        
        // 버튼 위치 (x, y, width, height)
        // [수정] buttonSize.width 대신 buttonWidth 변수 사용
        int buttonX = (1100 - buttonWidth) / 2; 
        int buttonY = inputY + inputSize.height + 30; 
        
        // [수정] buttonPanel.setBounds 대신 loginButton.setBounds로 변경
        loginButton.setBounds(buttonX, buttonY, buttonWidth, buttonHeight);

        // 4. 패널들을 mainPanel에 추가
        mainPanel.add(inputPanel);
        
        // [수정] buttonPanel 대신 loginButton을 mainPanel에 직접 추가
        mainPanel.add(loginButton); 
        
        // [삭제] BorderLayout 관련 코드는 모두 제거
        // ...

        // ▲▲▲▲▲ [수정 완료] ▲▲▲▲▲
        
        
        add(mainPanel);
        // --- 이벤트 리스너 ---
        loginButton.addActionListener(e -> onLogin());
        nameField.addActionListener(e -> onLogin());
        
        setVisible(true); // 창 보이기
    }
    
    /**
     * 로그인 버튼 클릭 또는 엔터 시 실행될 메소드
     */
    private void onLogin() {
        String playerName = nameField.getText().trim();
        
        // 닉네임 유효성 검사
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "닉네임을 입력해주세요.", 
                "입력 오류", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 로그인 창 닫기
        this.dispose();
        
        // [중요] 메인 게임 창 시작
        CatchMindGame game = new CatchMindGame(playerName);
        game.setupNetwork(); 
    }
    
    // --- CatchMindGame.java에서 가져온 헬퍼 메소드들 ---
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 둥근 모서리 효과
        button.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(15),
            BorderFactory.createEmptyBorder(10, 25, 10, 25)
        ));
        
        return button;
    }
    
    // 둥근 테두리를 위한 커스텀 Border 클래스
    class RoundedBorder implements Border {
        private int radius;
        
        RoundedBorder(int radius) {
            this.radius = radius;
        }
        
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius + 1, this.radius + 1, this.radius + 2, this.radius);
        }
        
        public boolean isBorderOpaque() {
            return true;
        }
        
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // [수정] 테두리를 부드럽게
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }
    
    // 테스트용 main (LoginWindow만 단독 실행)
    public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LoginWindow();
        });
    }
}
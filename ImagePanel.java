import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.net.URL; // URL import

// 배경 이미지를 그리는 커스텀 JPanel
public class ImagePanel extends JPanel {

    private Image backgroundImage;

    public ImagePanel(String imagePath) {
        // [수정] 이미지를 리소스로부터 안전하게 불러옵니다.
        // getResource()는 src 폴더를 기준으로 경로를 찾습니다.
        URL imageUrl = getClass().getResource(imagePath);
        
        if (imageUrl != null) {
            backgroundImage = new ImageIcon(imageUrl).getImage();
        } else {
            // 이미지를 못 찾을 경우 에러 메시지를 콘솔에 출력
            System.err.println("Error: Image not found at path: " + imagePath);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            // 이미지를 패널 크기에 꽉 차게 그립니다.
            g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
        }
    }
}
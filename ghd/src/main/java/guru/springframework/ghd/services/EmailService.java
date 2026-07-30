package guru.springframework.ghd.services;

import guru.springframework.ghd.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Async
    public void sendWelcomeEmail(User user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Chào mừng bạn đến với hệ thống!");
        String loginUrl = "https://greenhomeshop.vn/admin/v1/sign-in";

        String emailContent = "Chào " + user.getUsername() + ",\n\n" +
                "Chúc mừng! Tài khoản quản trị của bạn đã được khởi tạo thành công.\n\n" +
                "Dưới đây là thông tin đăng nhập của bạn:\n" +
                "--------------------------------------\n" +
                " Link đăng nhập: " + loginUrl + "\n" +
                " Tên đăng nhập: " + user.getUsername() + "\n" +
                " Mật khẩu: Test123@ \n" +
                "--------------------------------------\n\n" +
                "Lưu ý: Để bảo mật, vui lòng thay đổi mật khẩu ngay sau khi đăng nhập lần đầu.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ kỹ thuật.";

        message.setText(emailContent);
        mailSender.send(message);
    }
}
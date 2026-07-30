package guru.springframework.ghd.controllers.views;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NoHandlerFoundException.class, Exception.class})
    public String handleErrors(Exception ex, Model model) {
        model.addAttribute("titlePage", "404 - Trang không tồn tại");
        return "error/404";
    }
}
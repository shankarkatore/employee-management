package com.empmgmt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.empmgmt.model.Role;
import com.empmgmt.security.model.User;
import com.empmgmt.security.repository.UserRepository;



@Controller
public class AuthController {
	@Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    @GetMapping("/register-admin")
    public String adminRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register-admin";
    }

    @PostMapping("/register-admin")
    public String registerAdmin(@ModelAttribute User user,
                                RedirectAttributes redirectAttributes) {

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setRole(Role.ROLE_ADMIN);

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "Admin account created successfully!");

        return "redirect:/login";
    }
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String role,
                                        @RequestParam String usernameOrEmail,
                                        @RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/forgot-password";
        }

        Optional<User> optionalUser = userRepository.findByUsername(usernameOrEmail);
        if (optionalUser.isEmpty()) {
            optionalUser = userRepository.findByEmail(usernameOrEmail);
        }

        if (optionalUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/forgot-password";
        }

        User user = optionalUser.get();

        // Validate role matches
        boolean roleMatches = false;
        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            roleMatches = (user.getRole() == Role.ROLE_EMPLOYEE || user.getRole() == Role.EMPLOYEE);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            roleMatches = (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_HR);
        }

        if (!roleMatches) {
            redirectAttributes.addFlashAttribute("error", "Role mismatch for the given username/email!");
            return "redirect:/forgot-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null); // Clear any old reset tokens
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Password updated successfully!");
        return "redirect:/login";
    }
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token,
                                    Model model) {

        model.addAttribute("token", token);

        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       RedirectAttributes redirectAttributes) {

        Optional<User> optionalUser = userRepository.findByResetToken(token);

        if (optionalUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid Token!");

            return "redirect:/login";
        }

        User user = optionalUser.get();

        user.setPassword(passwordEncoder.encode(password));

        user.setResetToken(null);

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "Password Updated Successfully!");

        return "redirect:/login";
    }
    
    
}

package com.web.service;

import com.web.dto.CustomUserDetails;
import com.web.dto.LoginDto;
import com.web.dto.TokenDto;
import com.web.dto.request.FilterUserRequest;
import com.web.entity.User;
import com.web.enums.ActiveStatus;
import com.web.exception.MessageException;
import com.web.jwt.JwtTokenProvider;
import com.web.repository.UserRepository;
import com.web.repositoryCustom.CustomUserRepository;
import com.web.utils.Contains;
import com.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public TokenDto login(LoginDto loginDto){
        Optional<User> users = userRepository.findByUsername(loginDto.getUsername());
        if(users.isEmpty()){
            throw new MessageException("Tài khoản không tồn tại");
        }
        if(passwordEncoder.matches(loginDto.getPassword(), users.get().getPassword()) == false){
            throw new MessageException("Tài khoản hoặc Mật khẩu không chính xác");
        }
        // check infor user
        if(users.get().getActived() == false){
            throw new MessageException("Tài khoản đã bị khóa");
        }
        CustomUserDetails customUserDetails = new CustomUserDetails(users.get());
        String token = jwtTokenProvider.generateToken(customUserDetails);
        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(token);
        tokenDto.setUser(users.get());
        return tokenDto;
    }

    public User regis(User user){
        userRepository.findByUsername(user.getUsername())
                .ifPresent(exist->{
                    throw new MessageException("Tên đăng nhập đã tồn tại", 400);
                });
        user.setCreatedDate(LocalDate.now());
        user.setActived(true);
        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User result = userRepository.save(user);
        return result;
    }

    public String lockOrUnlock(Long id){
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            throw new MessageException("user not found");
        }
        if(user.get().getActived() == true){
            user.get().setActived(false);
            userRepository.save(user.get());
            return "Đã khoá tài khoản!";
        }
        else{
            user.get().setActived(true);
            userRepository.save(user.get());
            return "Đã mở khoá tài khoản";
        }
    }

    public User saveAndUpdateByAdmin(User user){
        Optional<User> userOptional = userRepository.findById(user.getId());
        if (userOptional.isEmpty()){
            user.setActived(true);
            user.setCreatedDate(LocalDate.now());

            if(!passwordEncoder.matches(user.getPassword(), userOptional.get().getPassword())){
                if (user.getPassword().length() < 5){
                    if (passwordEncoder.matches(user.getPassword(), userOptional.get().getPassword()) || user.getPassword().isEmpty()) {
                        user.setPassword(userOptional.get().getPassword());
                    } else {
                        throw new MessageException("Mật khẩu không được ít hơn 5 ký tự");
                    }
                } else if (user.getPassword().contains(" ")) {
                    throw new MessageException("Mật khẩu không được để ký tự space");
                } else {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                }
            }

            if (userRepository.findByUsername(user.getUsername()).isPresent()){
                throw new MessageException("Tên tài khoản đã tồn tại!");
            }
            if (!user.getRole().equals(Contains.ROLE_ADMIN) && !user.getRole().equals(Contains.ROLE_USER)
                    && !user.getRole().equals(Contains.ROLE_BLOG_MANAGER) && !user.getRole().equals(Contains.ROLE_DOCUMENT_MANAGER)) {
                throw new MessageException("Tên quyền không tồn tại!");
            }

            userRepository.save(user);
            return user;
        }

        if (userRepository.findByUsernameAndId(user.getUsername(), user.getId()).isPresent()) {
            throw new MessageException("Tên tài khoản đã tồn tại");
        }
        if (!user.getRole().equals(Contains.ROLE_ADMIN) && !user.getRole().equals(Contains.ROLE_USER)
                && !user.getRole().equals(Contains.ROLE_BLOG_MANAGER) && !user.getRole().equals(Contains.ROLE_DOCUMENT_MANAGER)) {
            throw new MessageException("Tên quyền không tồn tại!");
        }

        user.setCreatedDate(LocalDate.now());
        if(!passwordEncoder.matches(user.getPassword(), userOptional.get().getPassword())){
            if (user.getPassword().length() < 5){
                if (passwordEncoder.matches(user.getPassword(), userOptional.get().getPassword()) || user.getPassword().isEmpty()) {
                    user.setPassword(userOptional.get().getPassword());
                } else {
                    throw new MessageException("Mật khẩu không được ít hơn 5 ký tự");
                }
            } else if (user.getPassword().contains(" ")) {
                throw new MessageException("Mật khẩu không được để ký tự space");
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
        return userRepository.save(user);
    }

    public User updateInfor(User user){
        Optional<User> userOptional = userRepository.findById(user.getId());
        if (userOptional.isEmpty()){
            throw new MessageException("Tài khoản không tồn tại");
        }
        if (userRepository.findByUsernameAndId(user.getUsername(), user.getId()).isPresent()) {
            throw new MessageException("Tên tài khoản đã tồn tại");
        }

        if(!passwordEncoder.matches(user.getPassword(), userOptional.get().getPassword())){
            if (user.getPassword().length() < 5){
                if (passwordEncoder.matches(user.getPassword(), userOptional.get().getPassword()) || user.getPassword().isEmpty()) {
                    user.setPassword(userOptional.get().getPassword());
                } else {
                    throw new MessageException("Mật khẩu không được ít hơn 5 ký tự");
                }
            } else if (user.getPassword().contains(" ")) {
                throw new MessageException("Mật khẩu không được để ký tự space");
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }

        if (!user.getAvatar().equals(userOptional.get().getAvatar())){
            user.setAvatar(user.getAvatar());
        } if (user.getAvatar().isEmpty() || user.getAvatar() == null) {
            user.setAvatar(userOptional.get().getAvatar());
        }

        user.setCreatedDate(userOptional.get().getCreatedDate());
        user.setRole(userOptional.get().getRole());
        user.setActived(userOptional.get().getActived());
        return userRepository.save(user);
    }

    public User findUserById(Long id){
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new MessageException("User không tồn tại");
        }
        return user.get();
    }

    public Page<User> getAllUser(String userName, String roleName, Boolean active, Pageable pageable){
        if (userName.isEmpty() && roleName.isEmpty() && active == null) {
            return userRepository.getAllUser(pageable);
        } else if (userName.isEmpty() && roleName.isEmpty() && active) {
            return userRepository.getUserActived(pageable);
        } else if (userName.isEmpty() && roleName.isEmpty() && !active) {
            return userRepository.getUserUnactived(pageable);
        } else if (userName.isEmpty() && active == null) {
            return userRepository.findByRole(roleName,pageable);
        } else if (roleName == null && active == null) {
            return userRepository.findByName(userName,pageable);
        } else {
            return userRepository.findByParamAndRole(userName,roleName,pageable);
        }
    }

    public Page<User> filterUser(FilterUserRequest filterUserRequest, Pageable pageable){
        Specification<User> userSpecification = CustomUserRepository.filterUser(filterUserRequest);
        return userRepository.findAll(userSpecification, pageable);
    }
}

package com.softwarego.selection.user.service;

import com.softwarego.selection.user.domain.Role;
import com.softwarego.selection.user.domain.User;
import com.softwarego.selection.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserMapper userMapper;

    /**
     * 按 (staff_no, role) 查找身份，不存在则自动创建。
     *
     * <p>不包 @Transactional：并发首次登录时唯一索引会抛 DuplicateKeyException，
     * 捕获后需要回查已提交的记录；若在同一事务内捕获，事务会被标记回滚。</p>
     */
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    public User findOrCreate(String staffNo, Role role) {
        User existing = userMapper.findByStaffNoAndRole(staffNo, role);
        if (existing != null) {
            return existing;
        }

        User user = new User();
        user.setStaffNo(staffNo);
        user.setRole(role);
        try {
            userMapper.insert(user);
            log.info("首次登录创建用户: userId={}, role={}", user.getId(), role);
            return user;
        } catch (DuplicateKeyException exception) {
            // 并发首次登录：另一个请求已经插入成功，回查同一条身份。
            User raced = userMapper.findByStaffNoAndRole(staffNo, role);
            if (raced == null) {
                throw exception;
            }
            return raced;
        }
    }
}

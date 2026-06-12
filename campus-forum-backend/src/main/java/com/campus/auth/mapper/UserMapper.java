package com.campus.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.admin.dto.UserAdminVO;
import com.campus.auth.entity.User;
import com.campus.user.dto.UserProfileVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM \"user\" WHERE student_no = #{studentNo} AND deleted = 0")
    User selectByStudentNo(@Param("studentNo") String studentNo);

    @Select("SELECT * FROM \"user\" WHERE email = #{email} AND deleted = 0")
    User selectByEmail(@Param("email") String email);

    /**
     * 查询用户资料（含发帖数和获赞数联查），结果由 UserMapper.xml 定义
     */
    UserProfileVO selectProfileById(@Param("id") Long id);

    /**
     * 更新用户头像路径
     */
    @Update("UPDATE \"user\" SET avatar = #{avatarPath}, updated_at = NOW() WHERE id = #{id} AND deleted = 0")
    int updateAvatar(@Param("id") Long id, @Param("avatarPath") String avatarPath);

    /**
     * 管理员查询用户列表（含发帖数），结果由 UserMapper.xml 定义
     */
    List<UserAdminVO> selectAdminUserList(@Param("keyword") String keyword,
                                           @Param("status") Integer status,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 管理员查询用户总数
     */
    long countAdminUserList(@Param("keyword") String keyword,
                            @Param("status") Integer status);

    /**
     * 查询总用户数
     */
    @Select("SELECT COUNT(*) FROM \"user\" WHERE deleted = 0")
    Long selectTotalUsers();

    /**
     * 查询今日新增用户数
     */
    @Select("SELECT COUNT(*) FROM \"user\" WHERE deleted = 0 AND created_at >= CURRENT_DATE")
    Long selectTodayNewUsers();

    /**
     * 查询 N 天内活跃用户数（发帖或评论的用户）
     */
    Long selectActiveUsers(@Param("days") int days);
}

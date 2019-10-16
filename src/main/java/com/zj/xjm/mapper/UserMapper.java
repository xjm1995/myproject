package com.zj.xjm.mapper;

import com.zj.xjm.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface UserMapper {
    @Insert("insert into user (name,token,accountId,gmtcreate,gmtmodified) values( #{name},#{token},#{accountId},#{gmtcreate},#{gmtmodified})")
    public void save(User user);

    @Select("select *from user where token=#{token}")
    User findbyToken(@Param("token") String token);
}

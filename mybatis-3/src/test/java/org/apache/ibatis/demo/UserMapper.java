package org.apache.ibatis.demo;

import org.apache.ibatis.annotations.Param;

public interface UserMapper {
  User selectUser(@Param("id") Integer id);
}

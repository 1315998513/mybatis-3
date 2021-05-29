package org.apache.ibatis.demo;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class MybatisMain {
  public static void main(String[] args) throws IOException {

    /**
     * 1.mybatis是如何获取数据库配置？
     * 2.mybatis是如何执行sql语句？
     * 3.mybatis是如何执行数据库的？
     */
    String resource = "mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);

    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

    SqlSession session = sqlSessionFactory.openSession();

    User user = session.selectOne("org.mybatis.example.UserMapper.selectUser", 1);
    System.out.println(user);

  }
}

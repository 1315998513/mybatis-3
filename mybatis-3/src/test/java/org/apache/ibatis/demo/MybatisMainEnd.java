package org.apache.ibatis.demo;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class MybatisMainEnd {
  public static void main(String[] args) throws IOException {

    /**
     * 1.mybatis是如何获取数据库配置？
     * 2.mybatis是如何执行sql语句？
     * 3.mybatis是如何执行数据库的？
     */

    /**
     * 从 mybatis-config.xml
     * 拿到连接数据库的配置文件流对象
     */
    String resource = "mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);

    /**
     * 构建会话工厂，如何获取到数据库源？
     * 一、解析数据库配置封装到 Configuration 对象
     * 1.SqlSessionFactoryBuilder().build(inputStream)
     *  2.XMLConfigBuilder.prase()
     *    3.parseConfiguration()
     *      4.environmentsElement()
     *        5.dataSourceElement()
     *          6.configuration.setEnvironment(environmentBuilder.build())
     * 二、解析Mapper封装到 Configuration 对象
     * 1.mapperElement()
     *  2.XMLMapperBuilder.prase()
     *    3.configurationElement()
     *      4.buildStatementFromContext()
     *        5.buildStatementFromContext()
     *          6.parseStatementNode()
     *            7.addMappedStatement()
     *              8.configuration.addMappedStatement(statement)
     */
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

    /**
     * 执行器初始化，构建sqlSession，通过SqlSession得到Mapper接口对象进行数据库操作
     * 1.openSessionFromDataSource()
     *  2.configuration.newExecutor()
     *    3.new SimpleExecutor()
     */
    SqlSession session = sqlSessionFactory.openSession();

    /**
     * 底层调用执行器查询数据：先开启本地缓存，在本地缓存中查找为空后，去数据库查找，然后把查询到的数据设置到缓存，在进行数据的映射、返回
     * 1.selectOne()
     *  2.selectList() -> selectList()
     *    3.executor.query()  -> createCacheKey() 创建缓存
     *      4.query() -> query()
     *        5.queryFromDatabase()
     *          6.doQuery()
     *            7.handleResultSets()
     *              8.getFirstResultSet()
     *                9.getResultSet()
     *                  10.ResultSetWrapper()
     *                  创建三个list数组，分别用于存放 数据表列名、Java对象字段类型名、数据表字段类型名
     *                  columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
     *                  jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
     *                  classNames.add(metaData.getColumnClassName(i));
     *                    11.handleResultSet()
     *                      12.handleRowValues()
     *                        13.handleRowValuesForNestedResultMap
     *                          14.getRowValue()
     *                            15.applyAutomaticMappings() //自动映射
     */
    User user = session.selectOne("org.mybatis.example.UserMapper.selectUser", 1);
    System.out.println(user);
  }
}

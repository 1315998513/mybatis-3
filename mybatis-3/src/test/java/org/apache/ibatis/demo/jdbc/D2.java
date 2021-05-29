package org.apache.ibatis.demo.jdbc;

import java.util.HashMap;

public class D2 {

  /**
   * 合理偷懒 ~
   * attr.var .if .null .nonull/.nn ...
   * @param args
   */
  public static void main(String[] args) {
    String usr = "jack";
    String pwd = "123";
    // .nn
    if (usr.length() != 0 && pwd.length() != 0) {
      login(usr, pwd);
    }


    HashMap<Object, Object> map = new HashMap<>();
    map.put("usr",usr);
    String usr2 = (String) map.get("usr");
  }

  public static boolean login(String usr, String pwd) {
    if ("jack".equals(usr)) {
      if ("123".equals(pwd)) {
        return true;
      }
    }
    return false;
  }
}

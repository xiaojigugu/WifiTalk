package com.junt.wifitalk.model;

/**
 * description :用户信息model
 *
 * @author Junt
 * @date :2019/4/2 15:33
 */
public class User {

    private static User user;

    public static User getInstance(){
        if (user==null){
            user=new User();
        }
        return user;
    }
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

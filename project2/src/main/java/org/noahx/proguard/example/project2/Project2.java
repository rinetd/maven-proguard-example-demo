package org.noahx.proguard.example.project2;

import org.noahx.proguard.example.project2.dao.TestDao;
import org.noahx.proguard.example.project2.impl.User;

/**
 * Created by noah on 8/20/14.
 */
public class Project2 {

    public void init() {
        test1();
        test2();
    }

    private void test1() {
        Status on = Status.valueOf("On");
        switch (on) {
            case On: {

            }
            break;
            case Off: {

            }
            break;
        }
    }

    private void test2() {
        TestDao testDao=new TestDao();
        User user=new User();
        user.setUserid("abc");
        user.setPassword("pwd");
        user.setDescription("des");
        testDao.save(user);

    }

    private void test3() {
    }

    private void test4() {
    }

    private void throwException() {
        throw new RuntimeException("hello");
    }

    public void destroy() {
        test3();
        test4();
        throwException();
    }
}

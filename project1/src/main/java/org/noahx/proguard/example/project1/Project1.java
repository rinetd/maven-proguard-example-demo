package org.noahx.proguard.example.project1;

import org.noahx.proguard.example.project2.Project2;

/**
 * Created by noah on 8/20/14.
 */
public class Project1 {

    public static void main(String[] args) {
        Project2 project2=new Project2();
        project2.init();
        project2.destroy();
    }
}

package org.example;

public class newThreadWork {




    public static void main(String[] args) {
        Thread work = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("你好");
            }
        });
        work.start();
    }

}

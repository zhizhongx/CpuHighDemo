package org.example;

public class CpuHighDemo {

    static volatile long counter = 0;

    static Thread createWorker() {
        return new Thread(() -> {
            while (true) {
                counter++;
            }
        }, "cpu-worker");
    }

    public static void main(String[] args) {
        Thread worker = createWorker();
        worker.start();
    }
}

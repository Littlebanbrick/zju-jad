package pta;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MultithreadedAlternateExecution {
    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);
        Repo repo = new Repo(sc.nextLine());
        Thread t1 = new Thread(new Worker1(repo));
        Thread t2 = new Thread(new Worker2(repo));
        t1.start();
        Thread.yield();
        t2.start();
        sc.close();
    }
}

class Repo {
    public boolean finished = false;
    public List<String> tasks = new ArrayList<>();
    public int turn = 0;
    
    public Repo (String items) {
        String[] parts = items.trim().split("\\s+");
        for (String part : parts) {
            tasks.add(part);
        }
    }

    public synchronized int getSize() {
        return tasks.size();
    }

    public synchronized String getTask(int threadId) {
        while (!finished && turn != threadId) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        if (tasks.isEmpty()) {
            finished = true;
            notifyAll();
            return null;
        }

        if (finished) {
            return null;
        }

        String task = tasks.remove(0);
        System.out.println(
            Thread.currentThread().getName() + " finish " + task
        );
        turn = 1 - turn;
        notifyAll();
        return task;
    }
}

class Worker1 implements Runnable {
    public Repo repo;

    public Worker1(Repo repo) {
        this.repo = repo;
    }

    @Override
    public void run() {
        String task;
        while ((task = repo.getTask(0)) != null);
    }
}

class Worker2 implements Runnable {
    public Repo repo;

    public Worker2(Repo repo) {
        this.repo = repo;
    }

    @Override
    public void run() {
        String task;
        while ((task = repo.getTask(1)) != null);
    }
}
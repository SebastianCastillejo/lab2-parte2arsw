package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Immortal extends Thread {

    private static final Object pauseLock = new Object();

    private static final Object tieLock = new Object();

    private static volatile boolean paused = false;

    private static volatile boolean stopped = false;

    private static final AtomicInteger livingThreads = new AtomicInteger(0);

    private static final AtomicInteger pausedThreads = new AtomicInteger(0);

    private ImmortalUpdateReportCallback updateCallback=null;
    
    private volatile int health;
    
    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private final Random r = new Random(System.currentTimeMillis());


    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
    }

    // enciende la bandera y espera a que TODOS los hilos vivos se hayan dormido
    public static void pauseAll() {
        synchronized (pauseLock) {
            paused = true;
            while (livingThreads.get() > 0 && pausedThreads.get() < livingThreads.get()) {
                try {
                    pauseLock.wait(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    //apaga la bandera
    public static void resumeAll() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    // pide a todos los hilos que salgan de su ciclo
    public static void stopAll() {
        synchronized (pauseLock) {
            stopped = true;
            paused = false;
            pauseLock.notifyAll();
            while (livingThreads.get() > 0) {
                try {
                    pauseLock.wait(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void resetControlFlags() {
        synchronized (pauseLock) {
            paused = false;
            stopped = false;
            pausedThreads.set(0);
        }
    }

    public void run() {

        livingThreads.incrementAndGet();
        try {
            while (!stopped) {

                synchronized (pauseLock) {
                    while (paused && !stopped) {
                        pausedThreads.incrementAndGet();
                        pauseLock.notifyAll();
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            pausedThreads.decrementAndGet();
                            return;
                        }
                        pausedThreads.decrementAndGet();
                    }
                }

                if (stopped) {
                    break;
                }

                if (health <= 0) {
                    immortalsPopulation.remove(this);
                    break;
                }

                int currentSize = immortalsPopulation.size();
                if (currentSize <= 1) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }

                int myIndex = immortalsPopulation.indexOf(this);
                if (myIndex < 0) {
                    break;
                }

                int nextFighterIndex = r.nextInt(currentSize);
                if (nextFighterIndex == myIndex) {
                    nextFighterIndex = ((nextFighterIndex + 1) % currentSize);
                }

                Immortal im;
                try {
                    im = immortalsPopulation.get(nextFighterIndex);
                } catch (IndexOutOfBoundsException e) {
                    continue;
                }

                if (im != this) {
                    this.fight(im);
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

            }
        } finally {
            livingThreads.decrementAndGet();
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }

    }

    public void fight(Immortal i2) {

        int myHash = System.identityHashCode(this);
        int otherHash = System.identityHashCode(i2);

        if (myHash < otherHash) {
            synchronized (this) {
                synchronized (i2) {
                    doFight(i2);
                }
            }
        } else if (myHash > otherHash) {
            synchronized (i2) {
                synchronized (this) {
                    doFight(i2);
                }
            }
        } else {
            synchronized (tieLock) {
                synchronized (this) {
                    synchronized (i2) {
                        doFight(i2);
                    }
                }
            }
        }

    }

    private void doFight(Immortal i2) {
        if (this.health <= 0) {
            return;
        }

        if (i2.getHealth() > 0) {
            i2.changeHealth(i2.getHealth() - defaultDamageValue);
            this.health += defaultDamageValue;
            updateCallback.processReport("Fight: " + this + " vs " + i2+"\n");
            if (i2.getHealth() <= 0) {
                immortalsPopulation.remove(i2);
            }
        } else {
            immortalsPopulation.remove(i2);
            updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
        }
    }

    public void changeHealth(int v) {
        health = v;
    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }

}

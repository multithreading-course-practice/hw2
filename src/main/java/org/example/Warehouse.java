package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public class Warehouse extends Thread {

    private final List<Block> storage = new ArrayList<>();
    private final Queue<Truck> trucks = new ArrayDeque<>();

    public Warehouse(String name) {
        super(name);
    }

    public Warehouse(String name, Collection<Block> initialStorage) {
        this(name);
        storage.addAll(initialStorage);
    }

    @Override
    public void run() {
        Truck truck;
        while (!currentThread().isInterrupted()) {
            try {
                truck = getNextArrivedTruck();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (truck == null) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    if (currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                continue;
            }
            if (truck.getBlocks().isEmpty()) {
                loadTruck(truck);
            } else {
                unloadTruck(truck);
            }
        }
        log.info("Warehouse thread interrupted");
    }

    private void loadTruck(Truck truck) {
        Collection<Block> blocksToLoad = getFreeBlocks(truck.getCapacity());
        log.info("Loading into truck {} {} blocks from {}. There are {} blocks left",
                truck.getName(), blocksToLoad.size(), this.getName(), storage.size());
        try {
            sleep(10L * blocksToLoad.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while loading truck", e);
            Thread.currentThread().interrupt();
        }
        truck.getBlocks().addAll(blocksToLoad);
        log.info("{} loaded", truck.getName());
        synchronized (truck) {
            truck.notifyAll();
        }
    }

    private synchronized Collection<Block> getFreeBlocks(int maxItems) {
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            if (storage.isEmpty()) {
                log.info("no more blocks");
                break;
            }
            blocks.add(storage.remove(0));
        }
        return blocks;
    }

    private synchronized void returnBlocksToStorage(List<Block> returnedBlocks) {
        storage.addAll(returnedBlocks);
    }

    private void unloadTruck(Truck truck) {
        log.info("Unloading truck {}, there are {} blocks in it", truck.getName(), truck.getBlocks().size());
        List<Block> arrivedBlocks = truck.getBlocks();
        try {
            sleep(10L * arrivedBlocks.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while unloading truck", e);
            Thread.currentThread().interrupt();
        }
        returnBlocksToStorage(arrivedBlocks);
        truck.getBlocks().clear();
        synchronized (truck) {
            truck.notifyAll();
        }
        log.info("{} unloaded at {}, there are {} blocks", truck.getName(), this.getName(), storage.size());
    }

    private Truck getNextArrivedTruck() throws InterruptedException {
        return trucks.poll();
    }

    public void arrive(Truck truck) {
        try {
            trucks.add(truck);
            synchronized (truck) {
                truck.wait();
            }
            log.info("{} leaving", truck.getName());
        } catch (InterruptedException e) {
            log.info("truck arrived interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
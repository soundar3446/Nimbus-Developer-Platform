package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.service.PortAllocatorService;
import org.springframework.stereotype.Service;
import java.net.ServerSocket;

@Service
public class PortAllocatorServiceImpl implements PortAllocatorService {

    private static final int MIN_PORT = 8081;
    private static final int MAX_PORT = 9000;
    private final Object lock = new Object();

    @Override
    public int allocate() {
        synchronized (lock) {
            for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                if (isPortAvailable(port)) {
                    return port;
                }
            }
            throw new RuntimeException("Infrastructure Crisis: No free worker ports available in pool.");
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
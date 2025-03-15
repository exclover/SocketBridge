package org.exclover;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private final int port;
    private final String targetHost;
    private final int targetPort;
    private final ExecutorService threadPool;

    public Server(int port, String targetHost, int targetPort) {
        this.port = port;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleConnection(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdownNow();
        }
    }

    private void handleConnection(Socket clientSocket) {
        threadPool.execute(() -> {
            try {
                Socket targetSocket = new Socket(targetHost, targetPort);
                new SocketHandler(targetSocket, clientSocket).start();
            } catch (IOException e) {
                closeQuietly(clientSocket);
                e.printStackTrace();
            }
        });
    }

    private void closeQuietly(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        new Server(3131, "11.11.11.11", 3306).start();
    }

    public class SocketHandler {
        private final Socket server;
        private final Socket client;
        private final ExecutorService executorService;
        private final AtomicBoolean isRunning;
        private final CountDownLatch closeLatch;
        private static final int BUFFER_SIZE = 8192;

        public SocketHandler(Socket server, Socket client) {
            this.server = server;
            this.client = client;
            this.executorService = Executors.newFixedThreadPool(2);
            this.isRunning = new AtomicBoolean(true);
            this.closeLatch = new CountDownLatch(2);

            try {
                server.setTcpNoDelay(true);
                server.setReceiveBufferSize(BUFFER_SIZE);
                server.setSendBufferSize(BUFFER_SIZE);
                server.setSoTimeout(30000);

                client.setTcpNoDelay(true);
                client.setReceiveBufferSize(BUFFER_SIZE);
                client.setSendBufferSize(BUFFER_SIZE);
                client.setSoTimeout(30000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void start() {
            executorService.submit(() -> {
                try {
                    fastTransferData(server, client);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeLatch.countDown();
                    if (closeLatch.getCount() == 1) {
                        isRunning.set(false);
                    }
                }
            });

            executorService.submit(() -> {
                try {
                    fastTransferData(client, server);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeLatch.countDown();
                    if (closeLatch.getCount() == 1) {
                        isRunning.set(false);
                    }
                }
            });

            executorService.submit(() -> {
                try {
                    closeLatch.await();
                    closeAllConnections();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        private void fastTransferData(Socket source, Socket destination) throws IOException {
            if (source.isClosed() || destination.isClosed()) {
                return;
            }

            ReadableByteChannel inputChannel = null;
            WritableByteChannel outputChannel = null;

            try {
                inputChannel = Channels.newChannel(source.getInputStream());
                outputChannel = Channels.newChannel(destination.getOutputStream());

                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                int bytesRead;

                while (isRunning.get() && !source.isClosed() && !destination.isClosed()) {
                    try {
                        bytesRead = inputChannel.read(buffer);

                        if (bytesRead == -1) {
                            break;
                        }
                        else if (bytesRead > 0) {
                            buffer.flip();
                            while (buffer.hasRemaining()) {
                                outputChannel.write(buffer);
                            }
                            buffer.clear();
                        }
                    } catch (SocketException e) {
                        break;
                    }
                }
            } finally {
                if (inputChannel != null) {
                    try {
                        inputChannel.close();
                    } catch (IOException e) {}
                }

                if (outputChannel != null) {
                    try {
                        outputChannel.close();
                    } catch (IOException e) {}
                }
            }
        }

        private void closeAllConnections() {
            isRunning.set(false);
            executorService.shutdownNow();

            try {
                if (!server.isClosed()) {
                    server.close();
                }

                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

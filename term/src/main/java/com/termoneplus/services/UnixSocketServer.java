/*
 * Copyright (C) 2019 Roumen Petrov.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.termoneplus.services;

import android.net.Credentials;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.termoneplus.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;


public class UnixSocketServer {
    private static final int SO_TIMEOUT = (3 /*sec.*/ * 1000);

    private final ServerThread server;


    public UnixSocketServer(String address, ConnectionHandler handler) throws IOException {
        final LocalServerSocket socket = new LocalServerSocket(address);
        server = new ServerThread(socket, handler);
        server.setName(BuildConfig.APPLICATION_ID + "-UnixSockets-" + android.os.Process.myPid());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        LocalSocketAddress address = server.socket.getLocalSocketAddress();
        LocalSocket client = new LocalSocket();
        server.interrupted = true;
        try {
            client.connect(address);
        } catch (IOException ignore) {
        }
        try {
            client.close();
        } catch (IOException ignore) {
        }
    }


    public interface ConnectionHandler {
        void handle(InputStream inputStream, OutputStream outputStream) throws IOException;
    }


    private static class ServerThread extends Thread {
        private final LocalServerSocket socket;
        private final ConnectionHandler handler;
        boolean interrupted = false;

        private ServerThread(LocalServerSocket socket, ConnectionHandler handler) {
            this.socket = socket;
            this.handler = handler;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final LocalSocket connection = socket.accept();
                    if (interrupted) {
                        connection.close();
                        break;
                    }
                    connection.setSoTimeout(SO_TIMEOUT);

                    Credentials credentials = connection.getPeerCredentials();
                    int uid = credentials.getUid();
                    // accept requests only from same user id
                    if (uid != android.os.Process.myUid())
                        return;

                    Random random = new Random();
                    WorkerThread worker = new WorkerThread(connection, handler);
                    worker.setName(BuildConfig.APPLICATION_ID
                            + ":unix_socket_" + android.os.Process.myPid()
                            + "." + random.nextInt());
                    worker.setDaemon(true);
                    worker.start();
                } catch (IOException ignore) {
                    break;
                }
            }
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static class WorkerThread extends Thread {
        private final LocalSocket socket;
        private final ConnectionHandler handler;


        WorkerThread(LocalSocket socket, ConnectionHandler handler) {
            this.socket = socket;
            this.handler = handler;
        }

        @Override
        public void run() {
            try {
                handler.handle(socket.getInputStream(), socket.getOutputStream());
            } catch (IOException ignore) {
            }
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }
}

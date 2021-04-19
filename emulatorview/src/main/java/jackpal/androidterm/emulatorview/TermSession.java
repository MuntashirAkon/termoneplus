/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2018-2020 Roumen Petrov.  All rights reserved.
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

package jackpal.androidterm.emulatorview;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.termoneplus.compat.CharacterCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * A terminal session, consisting of a VT100 terminal emulator and its
 * input and output streams.
 * <p>
 * You need to supply an {@link InputStream} and {@link OutputStream} to
 * provide input and output to the terminal.  For a locally running
 * program, these would typically point to a tty; for a telnet program
 * they might point to a network socket.  Reader and writer threads will be
 * spawned to do I/O to these streams.  All other operations, including
 * processing of input and output in {@link #processInput processInput} and
 * {@link #write(byte[], int, int) write}, will be performed on the main thread.
 * <p>
 * Call {@link #setTermIn} and {@link #setTermOut} to connect the input and
 * output streams to the emulator.  When all of your initialization is
 * complete, your initial screen size is known, and you're ready to
 * start VT100 emulation, call {@link #initializeEmulator} or {@link
 * #updateSize} with the number of rows and columns the terminal should
 * initially have.  (If you attach the session to an {@link EmulatorView},
 * the view will take care of setting the screen size and initializing the
 * emulator for you.)
 * <p>
 * When you're done with the session, you should call {@link #finish} on it.
 * This frees emulator data from memory, stops the reader and writer threads,
 * and closes the attached I/O streams.
 */
public class TermSession {
    // Number of rows in the transcript
    private static final int TRANSCRIPT_ROWS = 10000;
    private static final int NEW_INPUT = 1;
    private static final int NEW_OUTPUT = 2;
    private static final int FINISH = 3;
    private static final int EOF = 4;
    private ColorScheme mColorScheme = BaseTextRenderer.defaultColorScheme;
    private UpdateCallback mNotify;
    private OutputStream mTermOut;
    private InputStream mTermIn;
    private String mTitle;
    private TranscriptScreen mTranscriptScreen;
    private TerminalEmulator mEmulator;
    private Thread mReaderThread;
    private ByteQueue mByteQueue;
    private byte[] mReceiveBuffer;
    private Thread mWriterThread;
    private ByteQueue mWriteQueue;
    private Handler mWriterHandler;
    private CharBuffer mWriteCharBuffer;
    private ByteBuffer mWriteByteBuffer;
    private CharsetEncoder mUTF8Encoder;
    private FinishCallback mFinishCallback;
    private boolean mIsRunning = false;
    private UpdateCallback mTitleChangedListener;

    boolean mDefaultUTF8Mode; /* shared with emulator instance */


    public TermSession() {
        this(false);
    }

    public TermSession(boolean exitOnEOF) {
        // on Android default charset is always UTF-8
        mUTF8Encoder = Charset.defaultCharset().newEncoder();
        mUTF8Encoder.onMalformedInput(CodingErrorAction.REPLACE);
        mUTF8Encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        mWriteCharBuffer = CharBuffer.allocate(3); // ensure extra space (>= +1)
        mWriteByteBuffer = ByteBuffer.allocate(6);

        mReceiveBuffer = new byte[4 * 1024];
        mByteQueue = new ByteQueue(4 * 1024);
        // Note if exitOnEOF is set at end of reader thread run is send EOF message
        // and then handler on EOF message calls onProcessExit().
        mReaderThread = new ReaderThread("TermSession input reader", exitOnEOF);

        mWriteQueue = new ByteQueue(4096);
        mWriterThread = new WriterThread("TermSession output writer");
    }

    protected void onProcessExit() {
        finish();
    }

    /**
     * Set the terminal emulator's window size and start terminal emulation.
     *
     * @param columns The number of columns in the terminal window.
     * @param rows    The number of rows in the terminal window.
     */
    public void initializeEmulator(int columns, int rows) {
        mTranscriptScreen = new TranscriptScreen(columns, TRANSCRIPT_ROWS, rows, mColorScheme);

        mEmulator = new TerminalEmulator(this, mTranscriptScreen, columns, rows, mColorScheme);

        mIsRunning = true;

        mReaderThread.start();
        mWriterThread.start();
    }

    /**
     * Write data to the terminal output.  The written data will be consumed by
     * the emulation client as input.
     * <p>
     * <code>write</code> itself runs on the main thread.  The default
     * implementation writes the data into a circular buffer and signals the
     * writer thread to copy it from there to the {@link OutputStream}.
     * <p>
     * Subclasses may override this method to modify the output before writing
     * it to the stream, but implementations in derived classes should call
     * through to this method to do the actual writing.
     *
     * @param data   An array of bytes to write to the terminal.
     * @param offset The offset into the array at which the data starts.
     * @param count  The number of bytes to be written.
     */
    public void write(byte[] data, int offset, int count) {
        try {
            while (count > 0) {
                // do not write if consumer is finished
                if (mReaderThread.getState() == Thread.State.TERMINATED)
                    break;

                int written = mWriteQueue.write(data, offset, count);
                offset += written;
                count -= written;
                notifyNewOutput();
            }
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Write the UTF-8 representation of a String to the terminal output.  The
     * written data will be consumed by the emulation client as input.
     * <p>
     * This implementation encodes the String and then calls
     * {@link #write(byte[], int, int)} to do the actual writing.  It should
     * therefore usually be unnecessary to override this method; override
     * {@link #write(byte[], int, int)} instead.
     *
     * @param data The String to write to the terminal.
     */
    public void write(String data) {
        byte[] bytes = data.getBytes(mUTF8Encoder.charset());
        write(bytes, 0, bytes.length);
    }

    /**
     * Write the UTF-8 representation of a single Unicode code point to the
     * terminal output.  The written data will be consumed by the emulation
     * client as input.
     * <p>
     * This implementation encodes the code point and then calls
     * {@link #write(byte[], int, int)} to do the actual writing.  It should
     * therefore usually be unnecessary to override this method; override
     * {@link #write(byte[], int, int)} instead.
     *
     * @param codePoint The Unicode code point to write to the terminal.
     */
    public void write(int codePoint) {
        ByteBuffer byteBuf = mWriteByteBuffer;
        if (codePoint < 128) {
            // Fast path for ASCII characters
            byte[] buf = byteBuf.array();
            buf[0] = (byte) codePoint;
            write(buf, 0, 1);
            return;
        }

        CharBuffer charBuf = mWriteCharBuffer;
        CharsetEncoder encoder = mUTF8Encoder;

        charBuf.clear();
        int cplen = CharacterCompat.toChars(codePoint, charBuf.array(), 0);
        charBuf.limit(cplen);

        encoder.reset();
        byteBuf.clear();
        encoder.encode(charBuf, byteBuf, true);
        encoder.flush(byteBuf);

        write(byteBuf.array(), 0, byteBuf.position());
    }

    /* Notify the writer thread that there's new output waiting */
    private void notifyNewOutput() {
        Handler writerHandler = mWriterHandler;
        if (writerHandler == null) {
            /* Writer thread isn't started -- will pick up data once it does */
            return;
        }
        writerHandler.sendEmptyMessage(NEW_OUTPUT);
    }

    /**
     * Get the {@link OutputStream} associated with this session.
     *
     * @return This session's {@link OutputStream}.
     */
    public OutputStream getTermOut() {
        return mTermOut;
    }

    /**
     * Set the {@link OutputStream} associated with this session.
     *
     * @param termOut This session's {@link OutputStream}.
     */
    public void setTermOut(OutputStream termOut) {
        mTermOut = termOut;
    }

    /**
     * Get the {@link InputStream} associated with this session.
     *
     * @return This session's {@link InputStream}.
     */
    public InputStream getTermIn() {
        return mTermIn;
    }

    /**
     * Set the {@link InputStream} associated with this session.
     *
     * @param termIn This session's {@link InputStream}.
     */
    public void setTermIn(InputStream termIn) {
        mTermIn = termIn;
    }

    /**
     * @return Whether the terminal emulation is currently running.
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    TranscriptScreen getTranscriptScreen() {
        return mTranscriptScreen;
    }

    TerminalEmulator getEmulator() {
        return mEmulator;
    }

    /**
     * Set an {@link UpdateCallback} to be invoked when the terminal emulator's
     * screen is changed.
     *
     * @param notify The {@link UpdateCallback} to be invoked on changes.
     */
    public void setUpdateCallback(UpdateCallback notify) {
        mNotify = notify;
    }

    /**
     * Notify the {@link UpdateCallback} registered by {@link
     * #setUpdateCallback setUpdateCallback} that the screen has changed.
     */
    protected void notifyUpdate() {
        if (mNotify != null) {
            mNotify.onUpdate();
        }
    }

    /**
     * Get the terminal session's title (may be null).
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Change the terminal session's title.
     */
    public void setTitle(String title) {
        mTitle = title;
        notifyTitleChanged();
    }

    /**
     * Set an {@link UpdateCallback} to be invoked when the terminal emulator's
     * title is changed.
     *
     * @param listener The {@link UpdateCallback} to be invoked on changes.
     */
    public void setTitleChangedListener(UpdateCallback listener) {
        mTitleChangedListener = listener;
    }

    /**
     * Notify the UpdateCallback registered for title changes, if any, that the
     * terminal session's title has changed.
     */
    protected void notifyTitleChanged() {
        UpdateCallback listener = mTitleChangedListener;
        if (listener != null) {
            listener.onUpdate();
        }
    }

    /**
     * Change the terminal's window size.  Will call {@link #initializeEmulator}
     * if the emulator is not yet initialized.
     * <p>
     * You should override this method if your application needs to be notified
     * when the screen size changes (for example, if you need to issue
     * <code>TIOCSWINSZ</code> to a tty to adjust the window size).  <em>If you
     * do override this method, you must call through to the superclass
     * implementation.</em>
     *
     * @param columns The number of columns in the terminal window.
     * @param rows    The number of rows in the terminal window.
     */
    public void updateSize(int columns, int rows) {
        synchronized (this) {
            if (mEmulator == null)
                initializeEmulator(columns, rows);
            else
                mEmulator.updateSize(columns, rows);
        }
    }

    /**
     * Retrieve the terminal's screen and scrollback buffer.
     *
     * @return A {@link String} containing the contents of the screen and
     * scrollback buffer.
     */
    public String getTranscriptText() {
        return mTranscriptScreen.getTranscriptText();
    }

    /**
     * Look for new input from the ptty, send it to the terminal emulator.
     */
    private void readFromProcess() {
        int bytesAvailable = mByteQueue.getBytesAvailable();
        int bytesToRead = Math.min(bytesAvailable, mReceiveBuffer.length);
        int bytesRead;
        try {
            bytesRead = mByteQueue.read(mReceiveBuffer, 0, bytesToRead);
        } catch (InterruptedException e) {
            return;
        }

        // Give subclasses a chance to process the read data
        processInput(mReceiveBuffer, 0, bytesRead);
        notifyUpdate();
    }

    /**
     * Process input and send it to the terminal emulator.  This method is
     * invoked on the main thread whenever new data is read from the
     * InputStream.
     * <p>
     * The default implementation sends the data straight to the terminal
     * emulator without modifying it in any way.  Subclasses can override it to
     * modify the data before giving it to the terminal.
     *
     * @param data   A byte array containing the data read.
     * @param offset The offset into the buffer where the read data begins.
     * @param count  The number of bytes read.
     */
    protected void processInput(byte[] data, int offset, int count) {
        mEmulator.append(data, offset, count);
    }

    /**
     * Write something directly to the terminal emulator input, bypassing the
     * emulation client, the session's {@link InputStream}, and any processing
     * being done by {@link #processInput processInput}.
     *
     * @param data   The data to be written to the terminal.
     * @param offset The starting offset into the buffer of the data.
     * @param count  The length of the data to be written.
     */
    protected final void appendToEmulator(byte[] data, int offset, int count) {
        mEmulator.append(data, offset, count);
    }

    /**
     * Set the terminal emulator's color scheme (default colors).
     *
     * @param scheme The {@link ColorScheme} to be used (use null for the
     *               default scheme).
     */
    public void setColorScheme(ColorScheme scheme) {
        if (scheme == null) {
            scheme = BaseTextRenderer.defaultColorScheme;
        }
        mColorScheme = scheme;
        if (mEmulator == null) {
            return;
        }
        mEmulator.setColorScheme(scheme);
    }

    /**
     * Set whether the terminal emulator should be in UTF-8 mode by default.
     * <p>
     * In UTF-8 mode, the terminal will handle UTF-8 sequences, allowing the
     * display of text in most of the world's languages, but applications must
     * encode C1 control characters and graphics drawing characters as the
     * corresponding UTF-8 sequences.
     *
     * @param utf8ByDefault Whether the terminal emulator should be in UTF-8
     *                      mode by default.
     */
    public void setDefaultUTF8Mode(boolean utf8ByDefault) {
        mDefaultUTF8Mode = utf8ByDefault;
        if (mEmulator == null) {
            return;
        }
        mEmulator.notifyUTF8ModeDefaultChange();
    }

    /**
     * Get whether the terminal emulator is currently in UTF-8 mode.
     *
     * @return Whether the emulator is currently in UTF-8 mode.
     */
    public boolean getUTF8Mode() {
        if (mEmulator == null) {
            return mDefaultUTF8Mode;
        } else {
            return mEmulator.getUTF8Mode();
        }
    }

    /**
     * Set an {@link UpdateCallback} to be invoked when the terminal emulator
     * goes into or out of UTF-8 mode.
     *
     * @param utf8ModeNotify The {@link UpdateCallback} to be invoked.
     */
    public void setUTF8ModeUpdateCallback(UpdateCallback utf8ModeNotify) {
        if (mEmulator != null) {
            mEmulator.setUTF8ModeUpdateCallback(utf8ModeNotify);
        }
    }

    /**
     * Reset the terminal emulator's state.
     */
    public void reset() {
        if (mEmulator != null)
            mEmulator.reset();
        notifyUpdate();
    }

    /**
     * Set a {@link FinishCallback} to be invoked once this terminal session is
     * finished.
     *
     * @param callback The {@link FinishCallback} to be invoked on finish.
     */
    public void setFinishCallback(FinishCallback callback) {
        mFinishCallback = callback;
    }

    /**
     * Finish this terminal session.  Frees resources used by the terminal
     * emulator and closes the attached <code>InputStream</code> and
     * <code>OutputStream</code>.
     */
    public void finish() {
        finalizeEmulator();
    }

    private synchronized void finalizeEmulator() {
        if (!mIsRunning) return;
        mIsRunning = false;

        if (mFinishCallback != null) mFinishCallback.onSessionFinish(this);

        // Stop the reader and writer threads, and close the I/O streams
        // Reader thread, if running, will be terminated indirectly by closed
        // input stream.
        // Remarks:
        // - when reader finish it closes writer thread.
        // - flag IsRunning blocks recursive call:
        //   -> mMsgHandler on EOF ->  onProcessExit() -> finish()
        try {
            mTermIn.close();
            mTermOut.close();
        } catch (IOException ignored) {
            // We don't care if this fails
        } catch (NullPointerException ignored) {
        }

        if (mEmulator != null) mEmulator.finish();
        if (mTranscriptScreen != null) mTranscriptScreen.finish();
    }

    /**
     * Callback to be invoked when a {@link TermSession} finishes.
     *
     * @see TermSession#setUpdateCallback
     */
    public interface FinishCallback {
        /**
         * Callback function to be invoked when a {@link TermSession} finishes.
         *
         * @param session The <code>TermSession</code> which has finished.
         */
        void onSessionFinish(TermSession session);
    }


    private static class TermHandler extends Handler {
        private final WeakReference<TermSession> reference;

        TermHandler(TermSession session) {
            //by default runs on application's main looper
            super(Looper.getMainLooper());
            reference = new WeakReference<>(session);
        }

        @Override
        public void handleMessage(Message msg) {
            TermSession session = reference.get();
            if (session == null) return;
            if (!session.mIsRunning) return;

            if (msg.what == NEW_INPUT)
                session.readFromProcess();
            else if (msg.what == EOF)
                session.onProcessExit();
        }
    }

    private static class WriterHandler extends Handler {
        private final WeakReference<WriterThread> reference;

        WriterHandler(Looper looper, WriterThread thread) {
            super(looper);
            reference = new WeakReference<>(thread);
        }

        @Override
        public void handleMessage(Message msg) {
            WriterThread thread = reference.get();
            if (thread == null) return;

            if (msg.what == NEW_OUTPUT) {
                thread.writeToOutput();
            } else if (msg.what == FINISH) {
                getLooper().quit();
            }
        }
    }

    private static class TraceThread extends Thread {
        private TraceThread(String name) {
            super(name);
        }

        @Override
        public synchronized void start() {
            Thread.State state = getState();
            boolean is_alive = isAlive();
            String msg = "thread: " + getName() + ", state: " + state + ", isAlive: " + is_alive;
            try {
                super.start();
            } catch (IllegalThreadStateException e) {
                if (state == State.NEW)
                    throw new ExceptionStateNew(msg);
                else if (state == State.TERMINATED)
                    throw new ExceptionStateTerminated(msg);
                else if (is_alive)
                    throw new ExceptionisAlive(msg);
                else
                    throw new IllegalThreadStateException(msg);
            }
        }

        private static class ExceptionStateNew extends IllegalThreadStateException {
            private ExceptionStateNew(String msg) {
                super(msg);
            }
        }

        private static class ExceptionStateTerminated extends IllegalThreadStateException {
            private ExceptionStateTerminated(String msg) {
                super(msg);
            }
        }

        private static class ExceptionisAlive extends IllegalThreadStateException {
            private ExceptionisAlive(String msg) {
                super(msg);
            }
        }
    }

    private class ReaderThread extends TraceThread {
        private final boolean notify_eof;

        ReaderThread(String name, boolean notify_eof) {
            super(name);
            this.notify_eof = notify_eof;
        }

        @Override
        public void run() {
            Handler handler = new TermHandler(TermSession.this);
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    if (isInterrupted())
                        break;
                    int read = mTermIn.read(buffer);
                    if (read == -1) {
                        // EOF -- process exited
                        break;
                    }
                    int offset = 0;
                    while (read > 0) {
                        int written = mByteQueue.write(buffer, offset, read);
                        offset += written;
                        read -= written;
                        handler.sendEmptyMessage(NEW_INPUT);
                    }
                }
            } catch (IOException ignored) {
            } catch (InterruptedException ignored) {
            }

            { // stop producer as well
                Handler writer = mWriterHandler;
                mWriterHandler = null;
                if (writer != null)
                    writer.sendEmptyMessage(FINISH);
            }

            if (notify_eof)
                handler.sendEmptyMessage(EOF);
        }
    }

    private class WriterThread extends TraceThread {
        private byte[] buffer = new byte[4096];

        private WriterThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Looper.prepare();
            mWriterHandler = new WriterHandler(Looper.myLooper(), this);
            // Drain anything in the queue from before we started
            writeToOutput();
            Looper.loop();
        }

        private void writeToOutput() {
            int bytesAvailable = mWriteQueue.getBytesAvailable();
            int bytesToWrite = Math.min(bytesAvailable, buffer.length);

            if (bytesToWrite == 0) return;

            try {
                mWriteQueue.read(buffer, 0, bytesToWrite);
                mTermOut.write(buffer, 0, bytesToWrite);
                mTermOut.flush();
            } catch (IOException e) {
                // Ignore exception
                // We don't really care if the receiver isn't listening.
                // We just make a best effort to answer the query.
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

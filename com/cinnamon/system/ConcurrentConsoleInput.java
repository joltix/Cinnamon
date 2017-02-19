package com.cinnamon.system;

import java.io.Console;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>
 *     Non-blocking access to console input.
 * </p>
 *
 * <p>
 *     Implementors must override four methods: {@link #onNextLine(String)} to modify or throw away undesirable input
 *     prior to creating a message, {@link #isStopRequest(String)} to choose the desired stop {@link String}, and
 *     {@link #createMessage(String)} to construct a {@link E} to be polled by another thread.
 * </p>
 *
 * <p>All public methods are thread-safe.</p>
 *
 * Created by chris on 2/7/2017.
 */
public abstract class ConcurrentConsoleInput<E>
{
    // Separate input polling to avoid blocking main Thread
    private Thread mThread = new Thread(new Runnable() {
        @Override
        public void run() {
            process();
        }
    });

    // Whether or not to stop polling for input
    private volatile boolean mClose = false;

    // Messages (based on input) to provide to other Threads
    private ConcurrentLinkedQueue<E> mMessages = new ConcurrentLinkedQueue<E>();

    // Input via Console for when outside IDE
    private Console mConsole = System.console();

    // Backup input for when Console is null (running in IDE)
    private Scanner mScanner = new Scanner(System.in);

    // Whether or not backup Scanner is used
    private boolean mUsesScanner;

    // Text to print at the start of the console
    private String mIntro;

    // Text to print after close command
    private String mOutro;

    /**
     * <p>Constructor for ConcurrentConsoleInput.</p>
     *
     * @param intro message to print after {@link #start()} is called.
     * @param outro message to print after {@link #stop()} is called.
     * @throws IllegalArgumentException if either the intro or outro text is null, empty, or made of whitespace.
     */
    public ConcurrentConsoleInput(String intro, String outro)
    {
        // Check if intro text is valid
        final String formattedIntro = formatConsoleText(intro);
        if (formattedIntro == null) {
            throw new IllegalArgumentException("Intro text cannot be null, empty, or solely whitespace");
        }

        // Check if outro text is valid
        final String formattedOutro = formatConsoleText(outro);
        if (formattedOutro == null) {
            throw new IllegalArgumentException("Outro text cannot be null, empty, or solely whitespace");
        }

        // Save console messages
        mIntro = formattedIntro;
        mOutro = formattedOutro;

        // Retrieve Console access
        mConsole = System.console();

        // Default to System.in if Console's unavailable (IDE)
        if (mConsole == null) {
            mUsesScanner = true;
            mScanner = new Scanner(System.in);
        }
    }

    /**
     * <p>Removes the oldest available {@link E} console input.</p>
     *
     * @return message.
     */
    public final E poll()
    {
        return mMessages.poll();
    }

    /**
     * <p>Checks whether or not any {@link E} messages are available from the console.</p>
     *
     * @return true if messages are available.f
     */
    public final boolean isEmpty()
    {
        return mMessages.isEmpty();
    }

    /**
     * <p>Starts receiving console input on a {@link Thread} other than the one calling this method.</p>
     *
     * <p>This method should be treated as {@link Thread#start()} and must not be called more than once, even if
     * {@link #stop()} has already been called.</p>
     *
     * @throws IllegalThreadStateException if this method is called more than once.
     */
    public final void start()
    {
        // Begin input polling in a separate Thread
        mThread.start();
    }

    /**
     * <p>Removes trailing and leading whitespace and appends a newline character at the end if the givent text does
     * not have it.</p>
     *
     * @param text String to format for console printing.
     * @return formatted text, or null if the given text is null or empty.
     */
    private String formatConsoleText(String text)
    {
        // Can't do anything with a null String
        if (text == null) {
            return null;
        }

        // Nothing to format in a String of whitespace
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }

        // Add new line in front if not there
        if (text.charAt(0) != '\n') {
            text = '\n' + text;
        }

        // Append new line character if not at end
        if (text.charAt(text.length() - 1) != '\n') {
            text = text + '\n';
        }

        return text;
    }

    /**
     * <p>Requests a stop to listening for console input.</p>
     */
    public final void stop()
    {
        mClose = true;
    }

    /**
     * <p>Checks whether or not console input is being read.</p>
     *
     * @return true if console input is actively read.
     */
    public final boolean isClosed()
    {
        return mClose;
    }

    /**
     * <p>Loops while checking the console for input and creates {@link E}s to be posted for consumers if the input is
     * valid.</p>
     *
     * <p>Whether or not input is valid is decided in {@link #onNextLine(String)}.</p>
     */
    private void process()
    {
        // Print intro text
        System.out.printf("%s\n", mIntro);

        // Loop till stop requested
        while (!mClose) {
            // Get next line of input
            final String input = getNextLine();

            // Print if valid
            if (input != null) {

                // Check if user requested Thread stop
                if (isStopRequest(input)) {
                    stop();
                } else {

                    // Create appropriate msg based off input
                    final E msgOut = createMessage(input);

                    // Enqueue msg to expose to other Threads
                    if (msgOut != null) {
                        mMessages.add(msgOut);
                    }
                }
            }
        }

        // Print closing text
        System.out.printf("%s\n", mOutro);
    }

    /**
     * <p>Gets the next line of input from the console.</p>
     *
     * @return console input.
     */
    private String getNextLine()
    {
        // Read input line from Console if available, otherwise use Scanner
        final String line = (mUsesScanner) ? mScanner.nextLine() : mConsole.readLine();

        // Allow subclass to process/filter input
        return onNextLine(line);
    }

    /**
     * <p>This method is meant for implementors to modify the input {@link String} from the console before a message is
     * constructed in {@link #createMessage(String)}. Returning <i>null</i> signifies the input is <b>not valid</b> and
     * will not be passed on to create an appropriate {@link E}.
     *
     * @param line console input.
     * @return processed input, or null if line is to be discarded.
     */
    protected abstract String onNextLine(String line);

    /**
     * <p>Determines whether or not the given line is a request to stop checking for input.</p>
     *
     * @param line console input.
     * @return true if the input requests the console shut down.
     */
    protected abstract boolean isStopRequest(String line);

    /**
     * <p>Creates a {@link E} to be posted based on the console's input.</p>
     *
     * @param line input from the console.
     * @return message.
     */
    protected abstract E createMessage(String line);
}

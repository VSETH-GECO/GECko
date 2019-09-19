/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package ch.ethz.geco.gecko;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * Provides a function to handle errors in a good readable way. Additionally it stores exceptions it couldn't
 * report because of errors to try to report them later on. If everything fails, it will still report the
 * exception in the console.
 */
public class ErrorHandler {
    /**
     * A buffer to store exceptions we couldn't post because D4J wasn't ready.
     */
    private static final LinkedList<Throwable> exceptionBuffer = new LinkedList<>();
    private static volatile boolean isTimerRunning = false;

    /**
     * Starts the timer to empty the exception buffer.
     */
    synchronized private static void startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true;

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    for (int i = 0; i < exceptionBuffer.size(); i++) {
                        handleError(exceptionBuffer.poll());
                    }
                }
            }, 2000, 1000);
        }
    }

    /**
     * Tries to report the given exception in a good readable way.
     *
     * @param e the exception to report
     */
    public static void handleError(Throwable e) {
        try {

            StackTraceElement[] stackTraceElements = e.getStackTrace();

            List<StackTraceElement> botTrace = new ArrayList<>();
            List<StackTraceElement> discordTrace = new ArrayList<>();
            List<StackTraceElement> javaTrace = new ArrayList<>();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                if (stackTraceElement.getClassName().startsWith("ch.ethz.geco.gecko")) {
                    botTrace.add(stackTraceElement);
                } else if (stackTraceElement.getClassName().startsWith("sx.blah.")) {
                    discordTrace.add(stackTraceElement);
                } else {
                    javaTrace.add(stackTraceElement);
                }
            }

            StringBuilder builder = new StringBuilder();

            List<StackTraceElement> listToUse;
            if (botTrace.size() > 0) {
                listToUse = botTrace;
            } else if (discordTrace.size() > 0) {
                listToUse = discordTrace;
            } else {
                listToUse = javaTrace;
            }

            for (StackTraceElement traceElement : listToUse) {
                String[] packagePath = traceElement.getClassName().split("\\.");
                builder.append("at ").append(packagePath[packagePath.length - 1]).append(".").append(traceElement.getMethodName())
                        .append("(").append(traceElement.getFileName()).append(":").append(traceElement.getLineNumber()).append(")").append("\n");
            }

            if (GECko.discordClient.isConnected()) {
                GECko.mainChannel.createMessage(messageCreateSpec -> messageCreateSpec.setEmbed(embedCreateSpec -> embedCreateSpec
                        .setColor(new Color(255, 0, 0))
                        .setTitle(e.getClass().getSimpleName() + ": " + e.getMessage())
                        .setDescription(builder.toString())))
                        .doOnError(err -> {
                            e.printStackTrace();
                            System.out.println("------------------------------");
                            err.printStackTrace();
                        }).subscribe();
            } else {
                exceptionBuffer.offer(e);
                startTimer();
            }
        } catch (Exception fatal) {
            e.printStackTrace();
            fatal.printStackTrace();
        }
    }
}

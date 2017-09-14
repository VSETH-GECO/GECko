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

package ch.ethz.geco;

import ch.ethz.geco.gecko.GECkO;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Launcher of the GECkO.
 */
public class Launcher {
    public static final org.slf4j.Logger logger = LoggerFactory.getLogger(Launcher.class);  // Logger of this class

    public static void main(String[] args) {
        // Init message
        logger.info("GECkO Launcher");

        // Get java environment stuff
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = GECkO.class.getCanonicalName();

        // Merge args with process commands
        String[] command = new String[4 + args.length];
        command[0] = javaBin;
        command[1] = "-cp";
        command[2] = classpath;
        command[3] = className;
        System.arraycopy(args, 0, command, 4, args.length);

        ProcessBuilder builder = new ProcessBuilder(command);

        int exitCode = 1;
        while (exitCode != 0) {
            Process process;
            try {
                process = builder.inheritIO().start();
                exitCode = process.waitFor();
            } catch (IOException e) {
                logger.error("[Launcher] An error occurred while trying to start the GECkO: " + e.getMessage());
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                logger.error("[Launcher] The GECkO process was interrupted: " + e.getMessage());
                e.printStackTrace();
            }

            switch (exitCode) {
                case 0:
                    logger.info("[Launcher] GECkO requested shutdown. Good Bye!");
                    break;
                case 2:
                    logger.info("[Launcher] GECkO requested restart. Restarting...");
                    break;
                default:
                    logger.error("[Launcher] GECkO crashed. Restarting...");
            }
        }
    }
}

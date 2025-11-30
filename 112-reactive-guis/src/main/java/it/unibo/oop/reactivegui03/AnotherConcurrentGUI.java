package it.unibo.oop.reactivegui03;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unibo.oop.JFrameUtil;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;

/**
 * Third experiment with reactive gui.
 */
@SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "False positive by spotbugs")
public final class AnotherConcurrentGUI extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AnotherConcurrentGUI.class);
    private final JLabel display = new JLabel();
    private final JButton stop = new JButton("stop");
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");
    private final Agent agent = new Agent();
    private final AutoAgent autoAgent = new AutoAgent();

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        JFrameUtil.dimensionJFrame(this);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(stop);
        panel.add(up);
        panel.add(down);
        this.getContentPane().add(panel);
        this.setVisible(true);
        /*
         * Create the counter agent and start it. This is actually not so good:
         * thread management should be left to
         * java.util.concurrent.ExecutorService
         */
        new Thread(agent).start();
        new Thread(autoAgent).start();
        /*
         * Register a listener that stops it
         */
        stop.addActionListener(e -> stopEverything());
        down.addActionListener(e -> agent.goDown());
        up.addActionListener(e -> agent.goUp());
    }

    private void stopEverything() {
            agent.stopCounting();
            autoAgent.stopCounting();
            SwingUtilities.invokeLater(() -> {
                stop.setEnabled(false);
                up.setEnabled(false);
                down.setEnabled(false);
        });
    }

    /*
     * The counter agent is implemented as a nested class. This makes it
     * invisible outside and encapsulated.
     */
    private final class Agent implements Runnable {
        /*
         * Stop is volatile to ensure visibility. Look at:
         *
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         *
         * For more details on how to use volatile:
         *
         * http://archive.is/4lsKW
         *
         */
        private volatile boolean stop;
        private volatile boolean order = true;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    // The EDT doesn't access `counter` anymore, it doesn't need to be volatile
                    final var nextText = Integer.toString(this.counter);
                    SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(nextText));
                    if (order) {
                        this.counter++;
                    } else {
                        this.counter--;
                    }
                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }

        public void goDown() {
            this.order = false;
        }

        public void goUp() {
            this.order = true;
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }
    }

    private final class AutoAgent implements Runnable {

        private static final long STOPTIME = 10_000L;
        private volatile boolean stop;
        private final long startTime;

        AutoAgent() {
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    final long timePassed = System.currentTimeMillis() - startTime;
                    if (timePassed >= STOPTIME) {
                        stopEverything();
                    }
                    //System.out.println(timePassed);
                    Thread.sleep(100);
                } catch (final InterruptedException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }

        public void stopCounting() {
            this.stop = true;
        }
    }
}

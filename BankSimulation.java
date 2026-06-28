import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.text.DecimalFormat;
public class BankSimulation {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InputWindow().setVisible(true));
    }
}

// ─────────────────────────────────────────────
//  INPUT WINDOW
// ─────────────────────────────────────────────
class InputWindow extends JFrame {

    private JTextField txtCustomers, txtArrivalMin, txtArrivalMax, txtServiceMin, txtServiceMax;
    private JButton btnSimulate;

    public InputWindow() {
        setTitle("Bank Queue Simulation — Input");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 340);
        setLocationRelativeTo(null);
        setResizable(false);

        // ── Main panel ──
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        main.setBackground(new Color(245, 248, 252));

        // ── Title ──
        JLabel title = new JLabel("Bank Queue Simulation", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(30, 60, 120));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        main.add(title, BorderLayout.NORTH);

        // ── Form ──
        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));
        form.setOpaque(false);

        txtCustomers  = addRow(form, "Number of Customers:",     "100");
        txtArrivalMin = addRow(form, "Arrival Time Min (min):",  "1");
        txtArrivalMax = addRow(form, "Arrival Time Max (min):",  "8");
        txtServiceMin = addRow(form, "Service Time Min (min):",  "1");
        txtServiceMax = addRow(form, "Service Time Max (min):",  "6");

        main.add(form, BorderLayout.CENTER);

        // ── Button ──
        btnSimulate = new JButton("▶  Run Simulation");
        btnSimulate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSimulate.setBackground(new Color(30, 90, 200));
        btnSimulate.setForeground(Color.WHITE);
        btnSimulate.setFocusPainted(false);
        btnSimulate.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnSimulate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSimulate.addActionListener(e -> runSimulation());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);
        btnPanel.add(btnSimulate);
        main.add(btnPanel, BorderLayout.SOUTH);

        add(main);
    }

    private JTextField addRow(JPanel panel, String label, String defaultValue) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JTextField tf = new JTextField(defaultValue);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        panel.add(lbl);
        panel.add(tf);
        return tf;
    }

    private void runSimulation() {
        try {
            int    n          = Integer.parseInt(txtCustomers.getText().trim());
            double arrMin     = Double.parseDouble(txtArrivalMin.getText().trim());
            double arrMax     = Double.parseDouble(txtArrivalMax.getText().trim());
            double svcMin     = Double.parseDouble(txtServiceMin.getText().trim());
            double svcMax     = Double.parseDouble(txtServiceMax.getText().trim());

            if (n <= 0 || arrMin < 0 || arrMax <= arrMin || svcMin < 0 || svcMax <= svcMin) {
                JOptionPane.showMessageDialog(this,
                        "Please enter valid values.\n• Customers must be > 0\n• Max must be greater than Min",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            SimulationResult result = Simulator.run(n, arrMin, arrMax, svcMin, svcMax);
            new OutputWindow(result).setVisible(true);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "All fields must be numeric.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// ─────────────────────────────────────────────
//  SIMULATION ENGINE
// ─────────────────────────────────────────────
class Simulator {

    static SimulationResult run(int n, double arrMin, double arrMax, double svcMin, double svcMax) {
        Random rng = new Random();
        double[] interArrival = new double[n];
        double[] serviceTime  = new double[n];
        double[] arrivalTime  = new double[n];
        double[] waitTime     = new double[n];
        double[] serviceStart = new double[n];
        double[] serviceEnd   = new double[n];
        double[] timeInSystem = new double[n];

        double serverFreeAt = 0.0;
        double clock        = 0.0;

        for (int i = 0; i < n; i++) {
            // Generate random times (uniform distribution)
            interArrival[i] = arrMin + (arrMax - arrMin) * rng.nextDouble();
            serviceTime[i]  = svcMin + (svcMax - svcMin) * rng.nextDouble();

            // Compute arrival time
            clock += interArrival[i];
            arrivalTime[i] = clock;

            // Service starts when server is free OR customer arrives (whichever is later)
            serviceStart[i] = Math.max(arrivalTime[i], serverFreeAt);
            waitTime[i]     = serviceStart[i] - arrivalTime[i];
            serviceEnd[i]   = serviceStart[i] + serviceTime[i];
            timeInSystem[i] = waitTime[i] + serviceTime[i];
            serverFreeAt    = serviceEnd[i];
        }

        return new SimulationResult(n, interArrival, serviceTime,
                arrivalTime, waitTime, serviceStart, serviceEnd, timeInSystem, arrivalTime[n-1]);
    }
}
class SimulationResult {
    final int      n;
    final double[] interArrival, serviceTime, arrivalTime;
    final double[] waitTime, serviceStart, serviceEnd, timeInSystem;
    final double   totalSimTime;

    // Statistics
    final double avgWaitTime;
    final double probWait;
    final double probServerBusy;
    final double propServerIdle;
    final double avgServiceTime;
    final double avgWaitTimeWaiters;
    final double avgInterArrival;
    final double avgTimeInSystem;

    SimulationResult(int n,
                     double[] interArrival, double[] serviceTime,
                     double[] arrivalTime,  double[] waitTime,
                     double[] serviceStart, double[] serviceEnd,
                     double[] timeInSystem, double totalSimTime) {
        this.n            = n;
        this.interArrival = interArrival;
        this.serviceTime  = serviceTime;
        this.arrivalTime  = arrivalTime;
        this.waitTime     = waitTime;
        this.serviceStart = serviceStart;
        this.serviceEnd   = serviceEnd;
        this.timeInSystem = timeInSystem;
        this.totalSimTime = totalSimTime;

        // ── Compute statistics ──
        double sumWait = 0, sumSvc = 0, sumSystem = 0, sumIA = 0;
        int waiters = 0;
        double busyTime = 0;

        for (int i = 0; i < n; i++) {
            sumWait   += waitTime[i];
            sumSvc    += serviceTime[i];
            sumSystem += timeInSystem[i];
            sumIA     += interArrival[i];
            busyTime  += serviceTime[i];
            if (waitTime[i] > 0.0001) waiters++;
        }

        avgWaitTime        = sumWait   / n;
        probWait           = (double) waiters / n;
        probServerBusy     = busyTime  / totalSimTime;
        propServerIdle     = 1.0 - probServerBusy;
        avgServiceTime     = sumSvc    / n;
        avgWaitTimeWaiters = waiters > 0 ? sumWait / waiters : 0;
        avgInterArrival    = sumIA     / n;
        avgTimeInSystem    = sumSystem / n;
    }
}
// ─────────────────────────────────────────────
//  OUTPUT WINDOW
// ─────────────────────────────────────────────
class OutputWindow extends JFrame {

    private static final DecimalFormat DF = new DecimalFormat("0.0000");
    private static final DecimalFormat PCT = new DecimalFormat("0.00%");

    public OutputWindow(SimulationResult r) {
        setTitle("Bank Queue Simulation — Results");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabs.addTab("📊  Queue Statistics", buildStatsPanel(r));
        tabs.addTab("📋  Simulation Table",  buildTablePanel(r));

        add(tabs);
    }

    // ── STATISTICS PANEL ──────────────────────
    private JPanel buildStatsPanel(SimulationResult r) {
        JPanel outer = new JPanel(new BorderLayout(15, 15));
        outer.setBackground(new Color(240, 245, 255));
        outer.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Queue Statistics — " + r.n + " Customers", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(new Color(20, 50, 120));
        outer.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 15, 15));
        grid.setOpaque(false);

        addStatCard(grid, "Avg Waiting Time",                    DF.format(r.avgWaitTime)       + " min",  new Color(30,  90,  200));
        addStatCard(grid, "Probability Customer Waits",          PCT.format(r.probWait),                   new Color(180, 60,  60));
        addStatCard(grid, "Proportion of Server Idle Time",      PCT.format(r.propServerIdle),             new Color(20,  140, 80));
        addStatCard(grid, "Probability Server is Busy",          PCT.format(r.probServerBusy),             new Color(150, 80,  180));
        addStatCard(grid, "Avg Service Time",                    DF.format(r.avgServiceTime)    + " min",  new Color(200, 120, 20));
        addStatCard(grid, "Avg Wait Time (Waiters Only)",        DF.format(r.avgWaitTimeWaiters)+ " min",  new Color(30,  150, 180));
        addStatCard(grid, "Avg Time Between Arrivals",           DF.format(r.avgInterArrival)   + " min",  new Color(60,  100, 40));
        addStatCard(grid, "Avg Time Spent in System",            DF.format(r.avgTimeInSystem)   + " min",  new Color(100, 50,  20));

        outer.add(grid, BorderLayout.CENTER);

        JLabel footer = new JLabel("Total Simulation Time: " + DF.format(r.totalSimTime) + " minutes", SwingConstants.RIGHT);
        footer.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footer.setForeground(Color.GRAY);
        outer.add(footer, BorderLayout.SOUTH);

        return outer;
    }

    private void addStatCard(JPanel panel, String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(Color.GRAY);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 20));
        val.setForeground(accent);

        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        panel.add(card);
    }

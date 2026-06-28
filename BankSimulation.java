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
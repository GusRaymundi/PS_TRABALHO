package executor;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class InterfaceExecutor extends JFrame {

    private JTextArea areaMemoria;
    private JTextArea areaRegistradores;

    public InterfaceExecutor() {
        setTitle("Simulador SIC/XE – Executor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));

        // Área para memória
        areaMemoria = new JTextArea();
        areaMemoria.setEditable(false);
        JScrollPane scrollMem = new JScrollPane(areaMemoria);
        scrollMem.setBorder(BorderFactory.createTitledBorder("Memória"));

        // Área para registradores
        areaRegistradores = new JTextArea();
        areaRegistradores.setEditable(false);
        JScrollPane scrollReg = new JScrollPane(areaRegistradores);
        scrollReg.setBorder(BorderFactory.createTitledBorder("Registradores"));

        add(scrollMem);
        add(scrollReg);

        atualizarInterface();
        setVisible(true);
    }

    // Atualiza os textos das duas áreas
    private void atualizarInterface() {

        // REGISTRADORES
        Map<String, Registrador> regs = new LinkedHashMap<>();
        regs.put("A", Executor.A);
        regs.put("X", Executor.X);
        regs.put("L", Executor.L);
        regs.put("B", Executor.B);
        regs.put("S", Executor.S);
        regs.put("T", Executor.T);
        regs.put("PC", Executor.PC);
        regs.put("SW", Executor.SW);

        StringBuilder sbReg = new StringBuilder();

        for (String nome : regs.keySet()) {
            Registrador r = regs.get(nome);
            sbReg.append(nome)
                    .append(" = ")
                    .append(r.getNumber())
                    .append("\n");
        }

        areaRegistradores.setText(sbReg.toString());

        // MEMÓRIA
        StringBuilder sbMem = new StringBuilder();

        for (int i = 0; i < Executor.pilha.memory.length; i++) {
            sbMem.append(String.format("[%03d]  %s\n", i, Executor.pilha.memory[i]));
        }

        areaMemoria.setText(sbMem.toString());
    }

    public static void main(String[] args) {
        // Apenas inicializa a interface
        SwingUtilities.invokeLater(() -> new InterfaceExecutor());
    }
}
